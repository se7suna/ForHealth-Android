from __future__ import annotations

import json
from pathlib import Path
from typing import List, Dict, Any, Optional, Tuple

from fastapi import UploadFile, HTTPException, status

from app.config import settings
from app.models.food import NutritionData
from app.schemas.ai_assistant import (
    FoodImageRecognitionResponse,
    RecognizedFoodItemResponse,
    FoodRecognitionConfirmRequest,
    FoodRecognitionConfirmResponse,
    ProcessedFoodItem,
    MealPlanRequest,
    MealPlanResponse,
    NutritionQuestionRequest,
    NutritionQuestionResponse,
    SportsQuestionRequest,
    SportsQuestionResponse,
)
from app.schemas.food import FoodRecordCreateRequest, FoodCreateRequest
from app.services import food_service, user_service
from app.utils.image_storage import save_food_image, get_image_url, validate_image_file
from app.utils.qwen_vl_client import call_qwen_vl_with_local_file, call_qwen_vl_with_url
from datetime import date, timedelta, datetime


async def _call_ai_for_foods(image_path: Path) -> List[Dict[str, Any]]:
    """
    调用多模态大模型识别图片中的食物，期望返回结构化 JSON。

    约定返回格式:
    {
      "recognized_foods": [
        {
          "food_name": "苹果",
          "serving_size": 150,
          "serving_unit": "克",
          "nutrition_per_serving": {
            "calories": 81,
            "protein": 0.45,
            "carbohydrates": 20.25,
            "fat": 0.3,
            "fiber": 3.6,
            "sugar": 15.3,
            "sodium": 1.5
          },
          "confidence": 0.92,
          "category": "水果"
        }
      ]
    }
    """
    prompt = (
        "你是营养与食物识别助手，请严格按照要求分析这张图片：\n"
        "1. 找出图片中所有可以清晰识别的可食用食物（忽略餐具、桌子等）。\n"
        "2. 估计每种食物的名称（中文），大致重量（克）和营养信息。\n"
        "3. 只回答 JSON，且必须是合法 JSON，不能包含任何解释性文字。\n"
        "4. JSON 顶层结构为：{\"recognized_foods\": [...]}，每个元素字段：\n"
        "   - food_name: string\n"
        "   - serving_size: number (克)\n"
        "   - serving_unit: string，固定为 \"克\" 或其它计量单位\n"
        "   - nutrition_per_serving: {calories, protein, carbohydrates, fat, fiber, sugar, sodium}\n"
        "   - confidence: 0-1 的小数\n"
        "   - category: 可选，食物分类\n"
        "不要使用注释，不要包含多余字段。"
    )

    raw = call_qwen_vl_with_local_file(
        image_path=str(image_path),
        prompt=prompt,
        model="qwen-vl-plus",
        api_key=None,
    )

    # 容错解析：尽量从返回文本中提取 JSON
    try:
        data = json.loads(raw)
    except Exception:
        # 尝试从文本中截取第一个大括号开始的部分
        try:
            start = raw.find("{")
            end = raw.rfind("}")
            if start != -1 and end != -1 and end > start:
                data = json.loads(raw[start : end + 1])
            else:
                data = {}
        except Exception:
            data = {}

    foods = data.get("recognized_foods") or []
    if not isinstance(foods, list):
        return []
    return foods


def _build_nutrition_from_ai(data: Dict[str, Any]) -> NutritionData:
    """从大模型返回的 nutrition_per_serving 构建 NutritionData，缺省值做兼容处理。"""
    n = data or {}
    return NutritionData(
        calories=float(n.get("calories", 0.0) or 0.0),
        protein=float(n.get("protein", 0.0) or 0.0),
        carbohydrates=float(n.get("carbohydrates", 0.0) or 0.0),
        fat=float(n.get("fat", 0.0) or 0.0),
        fiber=(None if n.get("fiber") is None else float(n.get("fiber"))),
        sugar=(None if n.get("sugar") is None else float(n.get("sugar"))),
        sodium=(None if n.get("sodium") is None else float(n.get("sodium"))),
    )


async def recognize_food_image(
    file: UploadFile,
    user_email: str,
) -> FoodImageRecognitionResponse:
    """
    上传图片并识别其中的食物。

    1. 验证并保存图片，生成可访问的 image_url；
    2. 调用多模态模型识别食物列表；
    3. 对每个识别结果，优先到本地数据库中匹配（按名称模糊搜索），若有命中则使用数据库营养信息；
       否则使用 AI 返回的营养信息；
    4. 汇总得到 FoodImageRecognitionResponse。
    """
    # 先做基础类型校验（非图片直接报错，给出友好信息）
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="请上传图片文件（content-type 需要为 image/*）",
        )

    # 进一步使用项目统一的图片校验逻辑
    validate_image_file(file)

    # 保存图片，获得相对路径与 URL
    relative_path = await save_food_image(file)
    image_url = get_image_url(relative_path)

    # 计算本地物理路径，用于传给 Qwen
    image_path = Path(settings.IMAGE_STORAGE_PATH) / relative_path

    try:
        ai_foods = await _call_ai_for_foods(image_path)
    except Exception as e:
        # AI 调用失败时，仍然返回结构化响应
        return FoodImageRecognitionResponse(
            success=False,
            message=f"AI 识别失败：{str(e)}",
            recognized_foods=[],
            total_calories=0.0,
            total_nutrition=None,
            image_url=image_url,
        )

    recognized_items: List[RecognizedFoodItemResponse] = []

    for item in ai_foods:
        food_name = (item.get("food_name") or "").strip()
        if not food_name:
            continue

        serving_size = float(item.get("serving_size") or 0.0)
        serving_unit = item.get("serving_unit") or "克"
        confidence = item.get("confidence")
        category = item.get("category")

        # 1. 先在本地数据库中按名称搜索，优先使用本地数据
        local_candidates = await food_service.search_local_foods_only(
            keyword=food_name,
            user_email=user_email,
            limit=1,
        )

        if local_candidates:
            local = local_candidates[0]
            nutrition = local.get("nutrition_per_serving") or {}
            # Pydantic 会自动把 dict 转为 NutritionData
            recognized_items.append(
                RecognizedFoodItemResponse(
                    food_name=local.get("name", food_name),
                    serving_size=serving_size if serving_size > 0 else float(local.get("serving_size") or 100.0),
                    serving_unit=serving_unit or (local.get("serving_unit") or "克"),
                    nutrition_per_serving=nutrition,
                    full_nutrition=local.get("full_nutrition"),
                    confidence=float(confidence) if confidence is not None else 1.0,
                    food_id=str(local.get("food_id") or local.get("_id")),
                    source="database",
                    category=local.get("category") or category,
                    image_url=local.get("image_url"),
                )
            )
        else:
            # 2. 使用大模型返回的营养信息
            nutrition_ai = _build_nutrition_from_ai(item.get("nutrition_per_serving") or {})
            recognized_items.append(
                RecognizedFoodItemResponse(
                    food_name=food_name,
                    serving_size=serving_size if serving_size > 0 else 100.0,
                    serving_unit=serving_unit,
                    nutrition_per_serving=nutrition_ai,
                    full_nutrition=None,
                    confidence=float(confidence) if confidence is not None else None,
                    food_id=None,
                    source="ai",
                    category=category,
                    image_url=None,
                )
            )

    # 计算总营养
    if recognized_items:
        total = {
            "calories": 0.0,
            "protein": 0.0,
            "carbohydrates": 0.0,
            "fat": 0.0,
            "fiber": 0.0,
            "sugar": 0.0,
            "sodium": 0.0,
        }
        for r in recognized_items:
            n = r.nutrition_per_serving
            total["calories"] += n.calories
            total["protein"] += n.protein
            total["carbohydrates"] += n.carbohydrates
            total["fat"] += n.fat
            total["fiber"] += n.fiber or 0.0
            total["sugar"] += n.sugar or 0.0
            total["sodium"] += n.sodium or 0.0

        # 四舍五入
        for k in total:
            total[k] = round(total[k], 2)

        total_nutrition = NutritionData(**total)
        total_calories = total_nutrition.calories
    else:
        total_nutrition = None
        total_calories = 0.0

    message = (
        f"成功识别到 {len(recognized_items)} 种食物"
        if recognized_items
        else "未能从图片中识别到明确的食物，请尝试更清晰的照片"
    )

    return FoodImageRecognitionResponse(
        success=bool(recognized_items),
        message=message,
        recognized_foods=recognized_items,
        total_calories=total_calories,
        total_nutrition=total_nutrition,
        image_url=image_url,
    )


async def confirm_food_recognition(
    user_email: str,
    payload: FoodRecognitionConfirmRequest,
) -> FoodRecognitionConfirmResponse:
    """
    处理AI识别结果，确保食物存在于本地数据库。
    
    功能：
    1. 对于有 food_id 的项，验证 food_id 是否存在
    2. 对于没有 food_id 的项，根据 AI 识别结果自动创建本地食物
    3. 返回处理后的食物信息（包含 food_id 和 serving_amount 建议）
    
    注意：此函数不创建饮食记录，前端需要调用 /api/food/record 来创建记录。
    """
    processed_foods: List[ProcessedFoodItem] = []

    for item in payload.recognized_foods:
        # 1. 优先尝试使用已有 food_id（如果存在且合法）
        food = None
        food_id: str | None = item.food_id
        if food_id:
            food = await food_service.get_food_by_id(food_id)

        # 2. 如果没有 food_id 或查不到对应食物，则根据 AI 结果自动创建本地食物
        if not food:
            # 使用 AI 识别结果构建 FoodCreateRequest
            try:
                food_create = FoodCreateRequest(
                    name=item.food_name,
                    category=item.category,
                    serving_size=item.serving_size if item.serving_size > 0 else 100.0,
                    serving_unit=item.serving_unit or "克",
                    nutrition_per_serving=item.nutrition_per_serving,
                    full_nutrition=item.full_nutrition,
                    brand=None,
                    barcode=None,
                    image=None,
                )
            except Exception:
                # 如果构建失败，跳过该识别项
                continue

            try:
                food = await food_service.create_food(food_create, creator_email=user_email)
                food_id = food.get("_id")
            except ValueError:
                # 如果名称冲突等原因导致创建失败，尝试按名称在本地查找已有食物
                local_candidates = await food_service.search_local_foods_only(
                    keyword=item.food_name,
                    user_email=user_email,
                    limit=1,
                )
                if local_candidates:
                    food = local_candidates[0]
                    food_id = str(food.get("food_id") or food.get("_id"))
                else:
                    # 仍然失败则跳过该识别项
                    continue

        if not food or not food_id:
            # 兜底：既没有找到食物也无法创建时跳过
            continue

        base_serving_size = float(food.get("serving_size") or 100.0)
        if base_serving_size <= 0:
            base_serving_size = 100.0

        # 根据克数推导份数，例如：图片估计 150g，标准份量为 100g，则 serving_amount=1.5
        serving_amount = item.serving_size / base_serving_size

        # 保存处理后的食物信息，供前端调用 /api/food/record 创建记录
        processed_foods.append(
            ProcessedFoodItem(
                food_id=food_id,
                food_name=item.food_name,
                serving_amount=serving_amount,
                serving_size=item.serving_size,
                serving_unit=item.serving_unit,
            )
        )

    success = len(processed_foods) > 0
    message = (
        f"成功处理 {len(processed_foods)} 种食物，请调用 /api/food/record 创建饮食记录"
        if success
        else "未能处理任何识别项（可能所有识别项都无法创建或匹配到食物）"
    )

    return FoodRecognitionConfirmResponse(
        success=success,
        message=message,
        processed_foods=processed_foods,
        total_foods=len(processed_foods),
    )


async def generate_meal_plan(
    user_email: str,
    payload: MealPlanRequest,
) -> MealPlanResponse:
    """
    根据用户输入使用 LLM 生成个性化饮食计划。
    
    优先使用请求中的值，如果请求中未提供，则从用户档案中读取。
    """
    # 1. 从用户档案获取默认值
    user_profile = await user_service.get_user_profile(user_email)
    
    # 2. 确定目标热量（优先使用请求值，其次用户档案，最后默认值）
    default_target_calories = None
    if user_profile and user_profile.get("daily_calorie_goal"):
        default_target_calories = float(user_profile["daily_calorie_goal"])
    target_calories = payload.target_calories or default_target_calories or 2000
    
    # 3. 合并食物偏好（请求中的值优先，否则使用用户档案中的值）
    # 如果请求中没有提供偏好，则从用户档案中读取
    final_pref = payload.food_preference
    if not final_pref and user_profile:
        from app.schemas.ai_assistant import FoodPreferenceRequest
        final_pref = FoodPreferenceRequest(
            liked_foods=user_profile.get("liked_foods"),
            disliked_foods=user_profile.get("disliked_foods"),
            allergies=user_profile.get("allergies"),
            dietary_restrictions=user_profile.get("dietary_restrictions"),
            preferred_tastes=user_profile.get("preferred_tastes"),
            cooking_skills=user_profile.get("cooking_skills"),
        )
    
    pref_desc = ""
    if final_pref:
        if final_pref.liked_foods:
            pref_desc += f"喜欢的食物：{', '.join(final_pref.liked_foods)}；"
        if final_pref.disliked_foods:
            pref_desc += f"不吃的食物：{', '.join(final_pref.disliked_foods)}；"
        if final_pref.allergies:
            pref_desc += f"过敏原：{', '.join(final_pref.allergies)}；"
        if final_pref.dietary_restrictions:
            pref_desc += f"饮食限制：{', '.join(final_pref.dietary_restrictions)}；"
        if final_pref.preferred_tastes:
            pref_desc += f"口味偏好：{', '.join(final_pref.preferred_tastes)}；"
        if final_pref.cooking_skills:
            pref_desc += f"烹饪技能：{final_pref.cooking_skills}；"

    # 4. 合并预算信息（请求中的值优先，否则使用用户档案中的值）
    include_budget = payload.include_budget
    budget_per_day = payload.budget_per_day
    
    if include_budget is None and user_profile:
        include_budget = user_profile.get("include_budget", False)
    
    if budget_per_day is None and user_profile and user_profile.get("budget_per_day"):
        budget_per_day = float(user_profile["budget_per_day"])
    
    budget_desc = ""
    if include_budget and budget_per_day:
        budget_desc = f"每日预算限制：{budget_per_day}元；"
    
    # 5. 构建 Prompt
    plan_days = payload.plan_days if payload.plan_duration == "day" else 7

    prompt = (
        f"请作为一名专业的营养师，为用户生成一份个性化的饮食计划。\n"
        f"计划周期：{plan_days}天\n"
        f"每日目标热量：{target_calories}千卡\n"
        f"每日餐次：{payload.meals_per_day}餐\n"
        f"{pref_desc}\n"
        f"{budget_desc}\n"
        f"要求：\n"
        f"1. 严格符合营养学原则，宏量营养素配比合理。\n"
        f"2. 食谱具体可执行，包含菜名、烹饪方法、份量（克）。\n"
        f"3. 输出格式必须为合法的 JSON，不要包含 markdown 标记或其他文字。\n"
        f"4. JSON 结构符合 MealPlanResponse 定义，主要包含 daily_plans 列表。\n"
        f"   每日计划包含：date (YYYY-MM-DD), meals (字典，key为早餐/午餐/晚餐等), daily_nutrition, daily_calories, macro_ratio, daily_cost (可选)。\n"
        f"   每餐包含食物列表，每个食物包含：food_name, serving_size, serving_unit, cooking_method, nutrition (估算), estimated_cost (可选)。\n"
        f"   dates 从 {date.today()} 开始。\n"
        f"5. nutrition_summary 包含平均数据。\n"
    )

    try:
        # 2. 调用 LLM (使用多模态模型处理纯文本，image_url=None)
        raw_response = call_qwen_vl_with_url(
            image_url=None,
            prompt=prompt,
            model="qwen-vl-plus"
        )
        
        # 3. 解析 JSON
        # 容错处理：去除可能的 markdown 代码块标记
        clean_json = raw_response.replace("```json", "").replace("```", "").strip()
        data = json.loads(clean_json)
        
        # 确保数据结构符合 Pydantic 模型
        # 如果 LLM 返回的结构有细微差异，这里可能需要适配，暂且假设 LLM 遵循指令
        
        # 补充顶层字段（如果 LLM 漏掉）
        if "success" not in data:
            data["success"] = True
        if "message" not in data:
            data["message"] = f"成功生成{plan_days}天个性化饮食计划"
        if "plan_duration" not in data:
            data["plan_duration"] = payload.plan_duration
        if "plan_days" not in data:
            data["plan_days"] = plan_days
        if "target_calories" not in data:
            data["target_calories"] = target_calories
            
        return MealPlanResponse(**data)

    except Exception as e:
        # 失败时返回错误信息
        return MealPlanResponse(
            success=False,
            message=f"生成计划失败: {str(e)}",
            plan_duration=payload.plan_duration,
            plan_days=plan_days,
            target_calories=target_calories,
            daily_plans=[],
            nutrition_summary={},
            suggestions=["请稍后重试或调整偏好设置"]
        )


async def answer_nutrition_question(
    user_email: str,
    payload: NutritionQuestionRequest,
) -> NutritionQuestionResponse:
    """
    回答用户关于营养知识的问题。
    
    使用 prompt 引导大模型参考专业知识回答，并过滤敏感信息。
    自动从用户档案中读取相关信息（如体重、活动水平、健康目标等）。
    """
    # 1. 从用户档案获取信息
    user_profile = await user_service.get_user_profile(user_email)
    
    # 2. 合并上下文信息（请求中的值优先，否则使用用户档案中的值）
    context_items = []
    
    # 用户目标
    user_goal = payload.context.get("user_goal") if payload.context else None
    if not user_goal and user_profile:
        health_goal = user_profile.get("health_goal_type")
        if health_goal:
            goal_map = {
                "lose_weight": "减重",
                "gain_weight": "增重",
                "maintain_weight": "保持体重"
            }
            user_goal = goal_map.get(health_goal, health_goal)
    if user_goal:
        context_items.append(f"用户目标：{user_goal}")
    
    # 活动水平
    activity_level = payload.context.get("activity_level") if payload.context else None
    if not activity_level and user_profile:
        activity_level = user_profile.get("activity_level")
    if activity_level:
        activity_map = {
            "sedentary": "久坐",
            "lightly_active": "轻度活动",
            "moderately_active": "中度活动",
            "very_active": "重度活动",
            "extremely_active": "极重度活动"
        }
        activity_desc = activity_map.get(activity_level, activity_level)
        context_items.append(f"活动水平：{activity_desc}")
    
    # 体重
    weight = payload.context.get("weight") if payload.context else None
    if not weight and user_profile and user_profile.get("weight"):
        weight = float(user_profile["weight"])
    if weight:
        context_items.append(f"体重：{weight}kg")
    
    # 身高
    height = payload.context.get("height") if payload.context else None
    if not height and user_profile and user_profile.get("height"):
        height = float(user_profile["height"])
    if height:
        context_items.append(f"身高：{height}cm")
    
    # 年龄
    age = payload.context.get("age") if payload.context else None
    if not age and user_profile and user_profile.get("age"):
        age = user_profile["age"]
    if age:
        context_items.append(f"年龄：{age}岁")
    
    context_info = ""
    if context_items:
        context_info = f"\n用户背景信息：{'; '.join(context_items)}。"
    
    prompt = (
        f"你是一名专业的营养师和健康顾问，请基于营养学专业知识回答用户的问题。\n"
        f"用户问题：{payload.question}{context_info}\n\n"
        f"要求：\n"
        f"1. 回答必须基于科学的营养学原理和权威指南（如《中国居民膳食指南》等）。\n"
        f"2. 回答要准确、专业、易懂，避免使用过于专业的术语。\n"
        f"3. 如果问题涉及医疗建议，请明确说明需要咨询专业医生。\n"
        f"4. 不要提供任何可能有害的建议（如极端节食、未经证实的偏方等）。\n"
        f"5. 回答要客观、中立，避免夸大效果或误导用户。\n"
        f"6. 如果问题超出营养学范畴，请礼貌地说明并建议咨询相关专业人士。\n"
        f"请直接给出回答，不需要重复问题。"
    )
    
    try:
        # 2. 调用 LLM (使用 qwen-vl-plus，纯文本输入)
        raw_response = call_qwen_vl_with_url(
            image_url=None,
            prompt=prompt,
            model="qwen-vl-plus"
        )
        
        # 3. 过滤敏感信息
        filtered_answer = _filter_sensitive_content(raw_response)
        
        # 4. 尝试从回答中提取相关话题和来源（如果 LLM 返回了结构化信息）
        # 这里简化处理，实际可以要求 LLM 返回 JSON 格式
        related_topics = _extract_related_topics(filtered_answer)
        sources = _extract_sources(filtered_answer)
        
        return NutritionQuestionResponse(
            success=True,
            question=payload.question,
            answer=filtered_answer,
            related_topics=related_topics if related_topics else None,
            sources=sources if sources else ["营养学专业知识库", "中国居民膳食指南"],
            confidence=0.9  # 默认置信度
        )
        
    except Exception as e:
        return NutritionQuestionResponse(
            success=False,
            question=payload.question,
            answer=f"抱歉，无法回答您的问题：{str(e)}",
            related_topics=None,
            sources=None,
            confidence=None
        )


def _filter_sensitive_content(content: str) -> str:
    """
    过滤回答中的敏感信息。
    
    过滤规则：
    1. 移除可能有害的建议（如极端节食、未经证实的偏方）
    2. 移除医疗诊断相关内容
    3. 移除可能误导用户的内容
    """
    if not content:
        return content
    
    # 敏感关键词列表（可根据需要扩展）
    sensitive_patterns = [
        "可以治愈",
        "一定能治好",
        "绝对有效",
        "包治百病",
        "立即见效",
        "三天瘦十斤",
        "不吃饭",
        "只喝水",
    ]
    
    filtered = content
    for pattern in sensitive_patterns:
        if pattern in filtered:
            # 如果包含敏感内容，添加免责声明
            filtered = filtered.replace(
                pattern,
                f"[已过滤：{pattern}]"
            )
    
    # 如果检测到可能有害的建议，添加免责声明
    if any(keyword in filtered.lower() for keyword in ["极端", "偏方", "秘方", "神药"]):
        disclaimer = "\n\n【重要提示】以上信息仅供参考，如有健康问题请咨询专业医生或营养师。"
        if disclaimer not in filtered:
            filtered += disclaimer
    
    return filtered


def _extract_related_topics(answer: str) -> List[str]:
    """
    从回答中提取相关话题（简单实现）。
    
    实际可以要求 LLM 返回结构化 JSON，这里做简单提取。
    """
    # 简单实现：查找常见营养话题关键词
    topics = []
    topic_keywords = {
        "蛋白质": "蛋白质补充",
        "碳水化合物": "碳水化合物摄入",
        "脂肪": "脂肪摄入",
        "维生素": "维生素补充",
        "矿物质": "矿物质补充",
        "膳食纤维": "膳食纤维摄入",
        "减重": "减重饮食",
        "增肌": "增肌营养",
        "运动": "运动营养",
    }
    
    answer_lower = answer.lower()
    for keyword, topic in topic_keywords.items():
        if keyword in answer_lower and topic not in topics:
            topics.append(topic)
    
    return topics[:3]  # 最多返回3个相关话题


def _extract_sources(answer: str) -> List[str]:
    """
    从回答中提取参考来源（简单实现）。
    """
    # 默认来源列表
    default_sources = [
        "中国居民膳食指南（2022）",
        "营养学专业知识库"
    ]
    
    # 如果回答中提到特定来源，可以提取
    mentioned_sources = []
    if "膳食指南" in answer:
        mentioned_sources.append("中国居民膳食指南（2022）")
    if "研究" in answer or "论文" in answer:
        mentioned_sources.append("营养学研究文献")
    
    return mentioned_sources if mentioned_sources else default_sources


async def answer_sports_question(
    user_email: str,
    payload: SportsQuestionRequest,
) -> SportsQuestionResponse:
    """
    回答用户关于运动知识的问题。
    
    使用 prompt 引导大模型参考运动科学知识回答，并过滤敏感信息。
    自动从用户档案中读取相关信息（如体重、身高、活动水平、健康目标等）。
    """
    # 1. 从用户档案获取信息
    user_profile = await user_service.get_user_profile(user_email)
    
    # 2. 合并上下文信息（请求中的值优先，否则使用用户档案中的值）
    context_items = []
    
    # 用户目标
    user_goal = payload.context.get("user_goal") if payload.context else None
    if not user_goal and user_profile:
        health_goal = user_profile.get("health_goal_type")
        if health_goal:
            goal_map = {
                "lose_weight": "减重",
                "gain_weight": "增重",
                "maintain_weight": "保持体重"
            }
            user_goal = goal_map.get(health_goal, health_goal)
    if user_goal:
        context_items.append(f"用户目标：{user_goal}")
    
    # 活动水平
    activity_level = payload.context.get("activity_level") if payload.context else None
    if not activity_level and user_profile:
        activity_level = user_profile.get("activity_level")
    if activity_level:
        activity_map = {
            "sedentary": "久坐",
            "lightly_active": "轻度活动",
            "moderately_active": "中度活动",
            "very_active": "重度活动",
            "extremely_active": "极重度活动"
        }
        activity_desc = activity_map.get(activity_level, activity_level)
        context_items.append(f"活动水平：{activity_desc}")
    
    # 体重
    weight = payload.context.get("weight") if payload.context else None
    if not weight and user_profile and user_profile.get("weight"):
        weight = float(user_profile["weight"])
    if weight:
        context_items.append(f"体重：{weight}kg")
    
    # 身高
    height = payload.context.get("height") if payload.context else None
    if not height and user_profile and user_profile.get("height"):
        height = float(user_profile["height"])
    if height:
        context_items.append(f"身高：{height}cm")
    
    # 年龄
    age = payload.context.get("age") if payload.context else None
    if not age and user_profile and user_profile.get("age"):
        age = user_profile["age"]
    if age:
        context_items.append(f"年龄：{age}岁")
    
    context_info = ""
    if context_items:
        context_info = f"\n用户背景信息：{'; '.join(context_items)}。"
    
    prompt = (
        f"你是一名专业的运动教练和运动科学顾问，请基于运动科学原理回答用户的问题。\n"
        f"用户问题：{payload.question}{context_info}\n\n"
        f"要求：\n"
        f"1. 回答必须基于科学的运动原理和权威指南（如ACSM运动指南、运动生理学等）。\n"
        f"2. 回答要准确、专业、易懂，避免使用过于专业的术语。\n"
        f"3. 如果问题涉及医疗建议或运动损伤，请明确说明需要咨询专业医生或物理治疗师。\n"
        f"4. 不要提供任何可能有害的建议（如过度训练、不正确的动作姿势、忽视运动安全等）。\n"
        f"5. 强调运动安全，提醒用户根据自身身体状况调整运动强度。\n"
        f"6. 回答要客观、中立，避免夸大效果或误导用户。\n"
        f"7. 如果问题超出运动科学范畴，请礼貌地说明并建议咨询相关专业人士。\n"
        f"请直接给出回答，不需要重复问题。"
    )
    
    try:
        # 2. 调用 LLM (使用 qwen-vl-plus，纯文本输入)
        raw_response = call_qwen_vl_with_url(
            image_url=None,
            prompt=prompt,
            model="qwen-vl-plus"
        )
        
        # 3. 过滤敏感信息
        filtered_answer = _filter_sensitive_sports_content(raw_response)
        
        # 4. 尝试从回答中提取相关话题和来源
        related_topics = _extract_sports_related_topics(filtered_answer)
        sources = _extract_sports_sources(filtered_answer)
        
        return SportsQuestionResponse(
            success=True,
            question=payload.question,
            answer=filtered_answer,
            related_topics=related_topics if related_topics else None,
            sources=sources if sources else ["运动科学原理", "ACSM运动指南"],
            confidence=0.9  # 默认置信度
        )
        
    except Exception as e:
        return SportsQuestionResponse(
            success=False,
            question=payload.question,
            answer=f"抱歉，无法回答您的问题：{str(e)}",
            related_topics=None,
            sources=None,
            confidence=None
        )


def _filter_sensitive_sports_content(content: str) -> str:
    """
    过滤运动知识回答中的敏感信息。
    
    过滤规则：
    1. 移除可能有害的建议（如过度训练、不正确的动作）
    2. 移除医疗诊断相关内容
    3. 移除可能误导用户的内容
    """
    if not content:
        return content
    
    # 敏感关键词列表（运动相关）
    sensitive_patterns = [
        "可以治愈",
        "一定能治好",
        "绝对有效",
        "立即见效",
        "一周瘦十斤",
        "不休息",
        "每天训练",
        "极限训练",
    ]
    
    filtered = content
    for pattern in sensitive_patterns:
        if pattern in filtered:
            # 如果包含敏感内容，添加免责声明
            filtered = filtered.replace(
                pattern,
                f"[已过滤：{pattern}]"
            )
    
    # 如果检测到可能有害的建议，添加免责声明
    if any(keyword in filtered.lower() for keyword in ["极端", "过度", "极限", "危险"]):
        disclaimer = "\n\n【重要提示】以上信息仅供参考，如有健康问题或运动损伤请咨询专业医生或运动教练。"
        if disclaimer not in filtered:
            filtered += disclaimer
    
    return filtered


def _extract_sports_related_topics(answer: str) -> List[str]:
    """
    从回答中提取相关运动话题（简单实现）。
    """
    topics = []
    topic_keywords = {
        "有氧": "有氧运动计划",
        "力量": "力量训练",
        "减脂": "减脂运动",
        "增肌": "增肌训练",
        "拉伸": "拉伸运动",
        "跑步": "跑步训练",
        "瑜伽": "瑜伽练习",
        "游泳": "游泳训练",
        "运动损伤": "运动损伤预防",
        "运动营养": "运动营养补充",
    }
    
    answer_lower = answer.lower()
    for keyword, topic in topic_keywords.items():
        if keyword in answer_lower and topic not in topics:
            topics.append(topic)
    
    return topics[:3]  # 最多返回3个相关话题


def _extract_sports_sources(answer: str) -> List[str]:
    """
    从回答中提取参考来源（简单实现）。
    """
    # 默认来源列表
    default_sources = [
        "运动科学原理",
        "ACSM运动指南"
    ]
    
    # 如果回答中提到特定来源，可以提取
    mentioned_sources = []
    if "acsm" in answer.lower() or "美国运动医学会" in answer:
        mentioned_sources.append("ACSM运动指南")
    if "研究" in answer or "论文" in answer:
        mentioned_sources.append("运动科学研究文献")
    if "生理学" in answer:
        mentioned_sources.append("运动生理学")
    
    return mentioned_sources if mentioned_sources else default_sources


# ========== 智能提醒与反馈 ==========

async def get_reminder_settings(user_email: str) -> dict:
    """
    获取用户的提醒设置
    
    Returns:
        提醒设置字典，如果不存在则返回默认设置
    """
    user_profile = await user_service.get_user_profile(user_email)
    
    if user_profile and user_profile.get("reminder_settings"):
        return user_profile["reminder_settings"]
    
    # 返回默认设置
    from app.schemas.ai_assistant import ReminderSettings
    default_settings = ReminderSettings().dict()
    return default_settings


async def update_reminder_settings(
    user_email: str,
    settings: dict
) -> dict:
    """
    更新用户的提醒设置
    
    Args:
        user_email: 用户邮箱
        settings: 提醒设置字典
    
    Returns:
        更新后的提醒设置
    """
    from app.database import get_database
    
    db = get_database()
    
    # 更新用户文档
    result = await db.users.find_one_and_update(
        {"email": user_email},
        {
            "$set": {
                "reminder_settings": settings,
                "updated_at": datetime.utcnow()
            }
        },
        return_document=True
    )
    
    if not result:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="用户不存在"
        )
    
    return settings


async def create_notification(
    user_email: str,
    notification_type: str,
    title: str,
    content: str,
    action_url: Optional[str] = None,
    priority: str = "normal"
) -> dict:
    """
    创建通知消息
    
    Args:
        user_email: 用户邮箱
        notification_type: 通知类型（meal_reminder、record_reminder、goal_achievement、motivational、feedback）
        title: 标题
        content: 内容
        action_url: 操作链接（可选）
        priority: 优先级（low、normal、high）
    
    Returns:
        创建的通知字典
    """
    from app.database import get_database
    from bson import ObjectId
    
    db = get_database()
    
    notification = {
        "user_email": user_email,
        "type": notification_type,
        "title": title,
        "content": content,
        "created_at": datetime.utcnow(),
        "read": False,
        "action_url": action_url,
        "priority": priority
    }
    
    result = await db.notifications.insert_one(notification)
    notification["_id"] = str(result.inserted_id)
    notification["id"] = notification["_id"]
    
    return notification


async def get_notifications(
    user_email: str,
    limit: int = 50,
    offset: int = 0,
    unread_only: bool = False
) -> Tuple[List[dict], int, int]:
    """
    获取用户的通知列表
    
    Args:
        user_email: 用户邮箱
        limit: 返回数量限制
        offset: 偏移量
        unread_only: 是否只返回未读通知
    
    Returns:
        (通知列表, 总数, 未读数)
    """
    from app.database import get_database
    
    db = get_database()
    
    # 构建查询条件
    query = {"user_email": user_email}
    if unread_only:
        query["read"] = False
    
    # 获取总数和未读数
    total = await db.notifications.count_documents({"user_email": user_email})
    unread_count = await db.notifications.count_documents({
        "user_email": user_email,
        "read": False
    })
    
    # 查询通知列表
    cursor = db.notifications.find(query).sort("created_at", -1).skip(offset).limit(limit)
    notifications = await cursor.to_list(length=limit)
    
    # 转换 ObjectId 为字符串
    for notification in notifications:
        notification["_id"] = str(notification["_id"])
        notification["id"] = notification["_id"]
    
    return notifications, total, unread_count


async def mark_notifications_read(
    user_email: str,
    notification_ids: List[str]
) -> int:
    """
    标记通知为已读
    
    Args:
        user_email: 用户邮箱
        notification_ids: 通知ID列表
    
    Returns:
        更新的通知数量
    """
    from app.database import get_database
    from bson import ObjectId
    
    db = get_database()
    
    # 转换字符串ID为ObjectId
    object_ids = []
    for nid in notification_ids:
        try:
            object_ids.append(ObjectId(nid))
        except Exception:
            continue
    
    if not object_ids:
        return 0
    
    # 更新通知
    result = await db.notifications.update_many(
        {
            "_id": {"$in": object_ids},
            "user_email": user_email
        },
        {
            "$set": {"read": True}
        }
    )
    
    return result.modified_count


async def get_daily_feedback(
    user_email: str,
    target_date: date
) -> dict:
    """
    获取每日反馈数据
    
    Args:
        user_email: 用户邮箱
        target_date: 目标日期
    
    Returns:
        反馈数据字典，包含 feedback 和 notification
    """
    from app.services import food_service
    from app.utils.qwen_vl_client import call_qwen_vl_with_url
    
    # 1. 获取用户档案和目标热量
    user_profile = await user_service.get_user_profile(user_email)
    target_calories = 2000.0  # 默认值
    if user_profile and user_profile.get("daily_calorie_goal"):
        target_calories = float(user_profile["daily_calorie_goal"])
    
    # 2. 获取当日营养摘要
    nutrition_summary = await food_service.get_daily_nutrition_summary(
        user_email, target_date
    )
    
    daily_calories = nutrition_summary.get("total_calories", 0.0)
    meal_count = nutrition_summary.get("meal_count", 0)
    
    # 3. 计算进度和目标状态
    calories_progress = min(daily_calories / target_calories, 1.0) if target_calories > 0 else 0.0
    
    if calories_progress > 1.1:
        goal_status = "exceeded"  # 超标
    elif calories_progress < 0.8:
        goal_status = "below"  # 不足
    else:
        goal_status = "on_track"  # 正常
    
    # 4. 构建营养摘要（包含所有营养数据）
    nutrition_data = {
        "calories": daily_calories,
        "protein": nutrition_summary.get("total_protein", 0.0),
        "carbohydrates": nutrition_summary.get("total_carbohydrates", 0.0),
        "fat": nutrition_summary.get("total_fat", 0.0),
        "fiber": 0.0,  # 如果需要，可以从记录中汇总
        "sugar": 0.0,
        "sodium": 0.0
    }
    
    # 从记录中汇总其他营养数据
    for record in nutrition_summary.get("records", []):
        record_nutrition = record.get("nutrition_data", {})
        nutrition_data["fiber"] += record_nutrition.get("fiber", 0.0)
        nutrition_data["sugar"] += record_nutrition.get("sugar", 0.0)
        nutrition_data["sodium"] += record_nutrition.get("sodium", 0.0)
    
    # 四舍五入
    for key in nutrition_data:
        nutrition_data[key] = round(nutrition_data[key], 2)
    
    # 5. 使用LLM生成个性化建议
    suggestions = []
    try:
        prompt = (
            f"根据以下用户的每日营养数据，生成3-5条简洁、实用的个性化建议（每条建议不超过30字）：\n"
            f"日期：{target_date.strftime('%Y-%m-%d')}\n"
            f"目标热量：{target_calories}千卡\n"
            f"实际摄入：{daily_calories}千卡\n"
            f"完成进度：{calories_progress * 100:.1f}%\n"
            f"进食次数：{meal_count}次\n"
            f"营养数据：\n"
            f"- 蛋白质：{nutrition_data['protein']}克\n"
            f"- 碳水化合物：{nutrition_data['carbohydrates']}克\n"
            f"- 脂肪：{nutrition_data['fat']}克\n"
            f"- 纤维：{nutrition_data['fiber']}克\n"
            f"- 糖：{nutrition_data['sugar']}克\n"
            f"- 钠：{nutrition_data['sodium']}毫克\n"
            f"目标状态：{goal_status}\n\n"
            f"要求：\n"
            f"1. 建议要具体、可执行\n"
            f"2. 根据目标状态给出相应建议（如超标则建议控制，不足则建议补充）\n"
            f"3. 关注营养均衡\n"
            f"4. 输出格式为JSON数组，例如：[\"建议1\", \"建议2\", \"建议3\"]\n"
            f"5. 不要包含markdown标记或其他解释性文字，只输出JSON数组"
        )
        
        raw_response = call_qwen_vl_with_url(
            image_url=None,
            prompt=prompt,
            model="qwen-vl-plus"
        )
        
        clean_json = raw_response.replace("```json", "").replace("```", "").strip()
        suggestions = json.loads(clean_json)
        
        if not isinstance(suggestions, list):
            suggestions = []
    except Exception as e:
        # 如果LLM调用失败，使用默认建议
        if goal_status == "exceeded":
            suggestions = [
                "今日热量摄入略高，建议适当控制饮食",
                "可以增加一些运动来消耗多余热量",
                "明天可以适当减少高热量食物的摄入"
            ]
        elif goal_status == "below":
            suggestions = [
                "今日热量摄入不足，建议适当增加营养",
                "可以增加一些优质蛋白质的摄入",
                "记得按时用餐，保证营养均衡"
            ]
        else:
            suggestions = [
                "今日营养摄入良好，继续保持！",
                "建议继续保持当前的饮食习惯",
                "记得多喝水，保持充足水分"
            ]
    
    # 6. 构建反馈数据
    feedback_data = {
        "date": target_date.strftime("%Y-%m-%d"),
        "daily_calories": daily_calories,
        "target_calories": target_calories,
        "calories_progress": round(calories_progress, 3),
        "nutrition_summary": nutrition_data,
        "meal_count": meal_count,
        "goal_status": goal_status,
        "suggestions": suggestions[:5]  # 最多5条建议
    }
    
    # 7. 根据目标状态创建通知（如果需要）
    notification = None
    if goal_status == "exceeded" and calories_progress > 1.2:
        # 超标超过20%，创建提醒通知
        notification = await create_notification(
            user_email=user_email,
            notification_type="feedback",
            title="热量摄入提醒",
            content=f"今日热量摄入为{daily_calories:.0f}千卡，超过目标{target_calories:.0f}千卡的20%，建议适当控制。",
            action_url="/food/record",
            priority="normal"
        )
    elif goal_status == "on_track" and calories_progress >= 0.9:
        # 接近目标，创建鼓励通知
        notification = await create_notification(
            user_email=user_email,
            notification_type="goal_achievement",
            title="目标达成",
            content=f"恭喜！您今日的营养摄入已接近目标（{calories_progress * 100:.0f}%），继续保持！",
            action_url="/feedback",
            priority="normal"
        )
    
    return {
        "feedback": feedback_data,
        "notification": notification
    }


