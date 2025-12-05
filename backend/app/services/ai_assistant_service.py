from __future__ import annotations

import json
from pathlib import Path
from typing import List, Dict, Any, Optional

from fastapi import UploadFile, HTTPException, status

from app.config import settings
from app.models.food import NutritionData
from app.schemas.ai_assistant import (
    FoodImageRecognitionResponse,
    RecognizedFoodItemResponse,
    FoodRecognitionConfirmResponse,
    ProcessedFoodItem,
    QuestionRequest,
    QuestionResponse,
    DietAnalysisRequest,
    DietAnalysisResponse,
    MealRecommendationResponse,
)
from datetime import datetime, date, timedelta
from app.schemas.food import FoodRecordCreateRequest, FoodCreateRequest
from app.services import food_service, user_service
from app.utils.image_storage import save_food_image, get_image_url, validate_image_file, delete_food_image
from app.utils.qwen_vl_client import call_qwen_vl_with_local_file, call_qwen_vl_with_url


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
        model="qwen3-vl-flash",
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
    4. 汇总得到 FoodImageRecognitionResponse；
    5. 识别完成后自动删除临时图片文件。
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
        # 调用 AI 识别
        ai_foods = await _call_ai_for_foods(image_path)
    except Exception as e:
        # AI 调用失败时，仍然返回结构化响应
        # 注意：图片会在函数末尾的 finally 块中删除
        response = FoodImageRecognitionResponse(
            success=False,
            message=f"AI 识别失败：{str(e)}",
            recognized_foods=[],
            total_calories=0.0,
            total_nutrition=None,
            image_url=None,  # 识别失败时不返回图片URL，因为图片会被删除
        )
        # 在返回前删除图片
        try:
            delete_food_image(relative_path)
        except Exception as del_e:
            # 删除失败不影响主流程，只记录错误
            print(f"警告：删除临时识别图片失败 {relative_path}: {str(del_e)}")
        return response

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

    # 构建响应
    response = FoodImageRecognitionResponse(
        success=bool(recognized_items),
        message=message,
        recognized_foods=recognized_items,
        total_calories=total_calories,
        total_nutrition=total_nutrition,
        image_url=None,  # 识别完成后图片会被删除，不返回URL
    )
    
    # 识别完成后自动删除临时图片
    try:
        delete_food_image(relative_path)
    except Exception as e:
        # 删除失败不影响主流程，只记录错误
        print(f"警告：删除临时识别图片失败 {relative_path}: {str(e)}")
    
    return response


async def recognize_and_process_food_image(
    file: UploadFile,
    user_email: str,
    meal_type: Optional[str] = None,
    notes: Optional[str] = None,
    recorded_at: Optional[datetime] = None,
) -> FoodRecognitionConfirmResponse:
    """
    上传图片、识别食物并自动处理识别结果（合并了识别和确认功能）。
    
    流程：
    1. 验证并保存图片
    2. 调用多模态模型识别食物列表
    3. 自动处理识别结果（创建/匹配食物）
    4. 返回处理后的食物信息（包含 food_id 和 serving_amount）
    5. 识别完成后自动删除临时图片
    
    注意：此函数不创建饮食记录，前端需要调用 /api/food/record 来创建记录。
    """
    # 先做基础类型校验
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
        # 调用 AI 识别
        ai_foods = await _call_ai_for_foods(image_path)
    except Exception as e:
        # AI 调用失败时，删除图片并返回错误响应
        try:
            delete_food_image(relative_path)
        except Exception:
            pass
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"AI 识别失败：{str(e)}",
        )

    # 将 AI 识别结果转换为 RecognizedFoodItemResponse 列表
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

    # 识别完成后自动删除临时图片
    try:
        delete_food_image(relative_path)
    except Exception as e:
        # 删除失败不影响主流程，只记录错误
        print(f"警告：删除临时识别图片失败 {relative_path}: {str(e)}")

    # 如果没有识别到任何食物，直接返回
    if not recognized_items:
        return FoodRecognitionConfirmResponse(
            success=False,
            message="未能从图片中识别到明确的食物，请尝试更清晰的照片",
            processed_foods=[],
            total_foods=0,
        )

    # 处理识别结果：创建/匹配食物
    processed_foods: List[ProcessedFoodItem] = []

    for item in recognized_items:
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

        # 获取营养信息（优先使用数据库中的，因为食物已经在数据库中了）
        nutrition = food.get("nutrition_per_serving") or {}
        if not nutrition:
            # 如果数据库中没有，使用识别结果中的营养信息
            nutrition = item.nutrition_per_serving
        
        # 确定 source（使用识别时的 source，因为这是识别阶段的来源）
        source = item.source if item.source else ("database" if item.food_id else "ai")

        # 转换营养信息为 NutritionData 对象
        if isinstance(nutrition, NutritionData):
            nutrition_data = nutrition
        elif isinstance(nutrition, dict):
            nutrition_data = NutritionData(**nutrition)
        else:
            # 兜底：创建默认营养数据
            nutrition_data = NutritionData(
                calories=0.0,
                protein=0.0,
                carbohydrates=0.0,
                fat=0.0,
                fiber=None,
                sugar=None,
                sodium=None,
            )

        # 保存处理后的食物信息，供前端调用 /api/food/record 创建记录
        processed_foods.append(
            ProcessedFoodItem(
                food_id=food_id,
                food_name=item.food_name,
                serving_amount=serving_amount,
                serving_size=item.serving_size,
                serving_unit=item.serving_unit,
                nutrition_per_serving=nutrition_data,
                source=source,
            )
        )

    success = len(processed_foods) > 0
    message = (
        f"成功识别并处理 {len(processed_foods)} 种食物，请调用 /api/food/record 创建饮食记录"
        if success
        else "未能处理任何识别项（可能所有识别项都无法创建或匹配到食物）"
    )

    return FoodRecognitionConfirmResponse(
        success=success,
        message=message,
        processed_foods=processed_foods,
        total_foods=len(processed_foods),
    )

async def answer_question(
    user_email: str,
    payload: QuestionRequest,
) -> QuestionResponse:
    """
    统一的健康知识问答接口。
    
    AI 会自动识别问题类型（营养/运动/综合健康），并基于用户档案信息给出个性化回答。
    
    Args:
        user_email: 用户邮箱
        payload: 问答请求
    
    Returns:
        问答响应
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
        f"你是一名专业的健康顾问，同时具备营养学和运动科学的专业知识。请基于科学原理回答用户的问题。\n"
        f"用户问题：{payload.question}{context_info}\n\n"
        f"要求：\n"
        f"1. 回答必须基于科学原理和权威指南（如《中国居民膳食指南》、ACSM运动指南等）。\n"
        f"2. 回答要准确、专业、易懂，避免使用过于专业的术语。\n"
        f"3. 如果问题涉及医疗建议，请明确说明需要咨询专业医生。\n"
        f"4. 不要提供任何可能有害的建议（如极端节食、过度训练等）。\n"
        f"5. 回答要客观、中立，避免夸大效果或误导用户。\n"
        f"6. 如果问题超出健康知识范畴，请礼貌地说明并建议咨询相关专业人士。\n"
        f"7. 如果问题涉及运动，请强调运动安全。\n"
        f"请直接给出回答，不需要重复问题。"
    )
    
    try:
        # 调用 LLM (使用 qwen3-vl-flash，纯文本输入)
        raw_response = call_qwen_vl_with_url(
            image_url=None,
            prompt=prompt,
            model="qwen3-vl-flash"
        )
        
        # 过滤敏感信息
        filtered_answer = _filter_sensitive_health_content(raw_response)
        
        # 提取相关话题和来源
        related_topics = _extract_health_related_topics(filtered_answer)
        sources = _extract_health_sources(filtered_answer)
        
        return QuestionResponse(
            success=True,
            question=payload.question,
            answer=filtered_answer,
            related_topics=related_topics if related_topics else None,
            sources=sources if sources else ["健康科学知识库", "中国居民膳食指南"],
            confidence=0.9
        )
        
    except Exception as e:
        return QuestionResponse(
            success=False,
            question=payload.question,
            answer=f"抱歉，无法回答您的问题：{str(e)}",
            related_topics=None,
            sources=None,
            confidence=None
        )


def _filter_sensitive_health_content(content: str) -> str:
    """
    过滤健康知识回答中的敏感信息。
    
    过滤规则：
    1. 移除可能有害的建议（如极端节食、过度训练）
    2. 移除医疗诊断相关内容
    3. 移除可能误导用户的内容
    """
    if not content:
        return content
    
    # 敏感关键词列表（营养和运动相关）
    sensitive_patterns = [
        "可以治愈",
        "一定能治好",
        "绝对有效",
        "包治百病",
        "立即见效",
        "三天瘦十斤",
        "一周瘦十斤",
        "不吃饭",
        "只喝水",
        "不休息",
        "每天训练",
        "极限训练",
    ]
    
    filtered = content
    for pattern in sensitive_patterns:
        if pattern in filtered:
            filtered = filtered.replace(pattern, f"[已过滤：{pattern}]")
    
    # 如果检测到可能有害的建议，添加免责声明
    if any(keyword in filtered.lower() for keyword in ["极端", "偏方", "秘方", "神药", "过度", "极限", "危险"]):
        disclaimer = "\n\n【重要提示】以上信息仅供参考，如有健康问题请咨询专业医生或相关专家。"
        if disclaimer not in filtered:
            filtered += disclaimer
    
    return filtered


def _extract_health_related_topics(answer: str) -> List[str]:
    """
    从回答中提取相关健康话题（简单实现）。
    """
    topics = []
    topic_keywords = {
        # 营养相关
        "蛋白质": "蛋白质补充",
        "碳水化合物": "碳水化合物摄入",
        "脂肪": "脂肪摄入",
        "维生素": "维生素补充",
        "矿物质": "矿物质补充",
        "膳食纤维": "膳食纤维摄入",
        "减重": "减重计划",
        # 运动相关
        "有氧": "有氧运动计划",
        "力量": "力量训练",
        "增肌": "增肌训练",
        "拉伸": "拉伸运动",
        "跑步": "跑步训练",
        "瑜伽": "瑜伽练习",
        "游泳": "游泳训练",
        "运动损伤": "运动损伤预防",
    }
    
    answer_lower = answer.lower()
    for keyword, topic in topic_keywords.items():
        if keyword in answer_lower and topic not in topics:
            topics.append(topic)
    
    return topics[:3]  # 最多返回3个相关话题


def _extract_health_sources(answer: str) -> List[str]:
    """
    从回答中提取参考来源（简单实现）。
    """
    mentioned_sources = []
    
    # 营养相关来源
    if "膳食指南" in answer:
        mentioned_sources.append("中国居民膳食指南（2022）")
    
    # 运动相关来源
    if "acsm" in answer.lower() or "美国运动医学会" in answer:
        mentioned_sources.append("ACSM运动指南")
    if "生理学" in answer:
        mentioned_sources.append("运动生理学")
    
    # 通用来源
    if "研究" in answer or "论文" in answer:
        mentioned_sources.append("健康科学研究文献")
    
    # 默认来源
    if not mentioned_sources:
        mentioned_sources = ["健康科学知识库", "中国居民膳食指南"]
    
    return mentioned_sources


# ========== 饮食分析与智能推荐 ==========

async def analyze_recent_diet(
    user_email: str,
    days: int = 7,
) -> DietAnalysisResponse:
    """
    分析用户近期饮食记录，找出卡路里平衡和营养素平衡的问题，给出一句话建议。
    
    关注两个方面：
    1. 消耗和摄入（卡路里）的不平衡
    2. 三种营养素是否平衡：蛋白质、碳水、脂肪
    
    Args:
        user_email: 用户邮箱
        days: 分析最近几天的记录
    
    Returns:
        DietAnalysisResponse
    """
    # 1. 获取用户档案和目标热量
    user_profile = await user_service.get_user_profile(user_email)
    target_calories = 2000.0  # 默认值
    if user_profile and user_profile.get("daily_calorie_goal"):
        target_calories = float(user_profile["daily_calorie_goal"])
    
    # 2. 获取近期饮食记录
    end_date = date.today()
    start_date = end_date - timedelta(days=days - 1)
    
    records, _ = await food_service.get_food_records(
        user_email, start_date=start_date, end_date=end_date, limit=500
    )
    
    if not records:
        return DietAnalysisResponse(
            success=True,
            message="最近还没有饮食记录呢～开始记录你的饮食吧，我会帮你分析的！😊",
            analysis={
                "days_analyzed": days,
                "records_count": 0,
                "main_issue": "无记录"
            }
        )
    
    # 3. 计算营养数据汇总
    total_calories = 0.0
    total_protein = 0.0
    total_carbs = 0.0
    total_fat = 0.0
    days_with_records = set()
    
    for record in records:
        nutrition = record.get("nutrition_data", {})
        total_calories += nutrition.get("calories", 0.0)
        total_protein += nutrition.get("protein", 0.0)
        total_carbs += nutrition.get("carbohydrates", 0.0)
        total_fat += nutrition.get("fat", 0.0)
        # 记录有记录的日期
        recorded_at = record.get("recorded_at")
        if recorded_at:
            if isinstance(recorded_at, datetime):
                days_with_records.add(recorded_at.date())
            elif isinstance(recorded_at, date):
                days_with_records.add(recorded_at)
    
    actual_days = len(days_with_records) if days_with_records else 1
    
    # 计算每日平均值
    avg_calories = total_calories / actual_days
    avg_protein = total_protein / actual_days
    avg_carbs = total_carbs / actual_days
    avg_fat = total_fat / actual_days
    
    # 计算宏量营养素比例（按热量计算）
    # 蛋白质和碳水 4 kcal/g，脂肪 9 kcal/g
    protein_cals = avg_protein * 4
    carbs_cals = avg_carbs * 4
    fat_cals = avg_fat * 9
    total_macro_cals = protein_cals + carbs_cals + fat_cals
    
    if total_macro_cals > 0:
        protein_percent = round(protein_cals / total_macro_cals * 100)
        carbs_percent = round(carbs_cals / total_macro_cals * 100)
        fat_percent = round(fat_cals / total_macro_cals * 100)
    else:
        protein_percent = carbs_percent = fat_percent = 0
    
    # 4. 分析问题
    # 卡路里平衡分析
    calorie_ratio = avg_calories / target_calories if target_calories > 0 else 0
    if calorie_ratio < 0.8:
        calorie_status = "明显不足"
    elif calorie_ratio < 0.95:
        calorie_status = "略有不足"
    elif calorie_ratio <= 1.05:
        calorie_status = "基本平衡"
    elif calorie_ratio <= 1.2:
        calorie_status = "略有超标"
    else:
        calorie_status = "明显超标"
    
    # 营养素平衡分析（推荐比例：蛋白质15-20%，碳水50-60%，脂肪20-30%）
    issues = []
    if protein_percent < 12:
        issues.append("蛋白质摄入严重不足")
    elif protein_percent < 15:
        issues.append("蛋白质摄入偏低")
    elif protein_percent > 25:
        issues.append("蛋白质摄入偏高")
    
    if carbs_percent < 40:
        issues.append("碳水化合物摄入不足")
    elif carbs_percent > 65:
        issues.append("碳水化合物摄入过多")
    
    if fat_percent < 15:
        issues.append("脂肪摄入不足")
    elif fat_percent > 35:
        issues.append("脂肪摄入偏高")
    
    if calorie_status in ["明显不足", "略有不足"]:
        issues.insert(0, f"热量{calorie_status}")
    elif calorie_status in ["略有超标", "明显超标"]:
        issues.insert(0, f"热量{calorie_status}")
    
    # 5. 使用 LLM 生成亲和的一句话建议
    main_issue = issues[0] if issues else "无明显问题"
    
    prompt = (
        f"你是一个亲切的营养师小助手。根据以下用户近{actual_days}天的饮食分析结果，生成一句亲和、温暖的建议或鼓励（不超过50字）。\n\n"
        f"分析数据：\n"
        f"- 平均每日摄入热量：{avg_calories:.0f}千卡（目标：{target_calories:.0f}千卡）\n"
        f"- 热量状态：{calorie_status}\n"
        f"- 营养素配比：蛋白质{protein_percent}%，碳水{carbs_percent}%，脂肪{fat_percent}%\n"
        f"- 发现的问题：{', '.join(issues) if issues else '暂无明显问题'}\n\n"
        f"要求：\n"
        f"1. 语气亲和温暖，像朋友聊天一样\n"
        f"2. 如果有问题，指出最主要的一个问题并给出简短建议\n"
        f"3. 如果没有问题，给予鼓励\n"
        f"4. 可以适当使用 emoji\n"
        f"5. 只输出一句话，不要其他解释"
    )
    
    try:
        message = call_qwen_vl_with_url(
            image_url=None,
            prompt=prompt,
            model="qwen3-vl-flash"
        ).strip()
        # 清理可能的引号
        message = message.strip('"\'')
    except Exception:
        # 如果 LLM 调用失败，使用默认建议
        if not issues:
            message = "你最近的饮食很均衡，继续保持哦！💪"
        elif "蛋白质" in main_issue:
            message = "最近蛋白质摄入偏低哦～建议多吃些鸡蛋、鸡胸肉补充一下！💪"
        elif "热量" in main_issue and "不足" in main_issue:
            message = "最近吃得有点少呢～记得按时吃饭，保证营养哦！🍚"
        elif "热量" in main_issue and "超标" in main_issue:
            message = "最近热量摄入有点多～可以适当控制一下，多运动运动！🏃"
        else:
            message = f"注意一下{main_issue}哦，调整一下会更健康！😊"
    
    return DietAnalysisResponse(
        success=True,
        message=message,
        analysis={
            "days_analyzed": days,
            "actual_days_with_records": actual_days,
            "records_count": len(records),
            "avg_calories_intake": round(avg_calories, 1),
            "avg_calories_target": target_calories,
            "calorie_balance": calorie_status,
            "macro_ratio": {
                "protein_percent": protein_percent,
                "carbs_percent": carbs_percent,
                "fat_percent": fat_percent
            },
            "main_issue": main_issue,
            "all_issues": issues
        }
    )


async def recommend_meal(user_email: str) -> MealRecommendationResponse:
    """
    根据当前时间和用户近期营养摄入情况，推荐具体菜式。
    
    Args:
        user_email: 用户邮箱
    
    Returns:
        MealRecommendationResponse
    """
    # 1. 获取当前时间，确定餐次
    now = datetime.now()
    hour = now.hour
    
    if 5 <= hour < 10:
        meal_type = "早餐"
        time_greeting = "早上好"
        meal_reminder = "新的一天开始了！记得吃早餐哦"
    elif 10 <= hour < 14:
        meal_type = "午餐"
        time_greeting = "中午好"
        meal_reminder = "到中午了！记得吃午饭哦"
    elif 14 <= hour < 17:
        meal_type = "加餐"
        time_greeting = "下午好"
        meal_reminder = "下午茶时间到～来点小零食补充能量吧"
    elif 17 <= hour < 21:
        meal_type = "晚餐"
        time_greeting = "傍晚好"
        meal_reminder = "晚餐时间到了！来顿健康的晚餐吧"
    else:
        meal_type = "加餐"
        time_greeting = "夜深了"
        meal_reminder = "这么晚了～如果饿了可以吃点清淡的"
    
    # 2. 分析近期营养摄入（获取近3天数据）
    end_date = date.today()
    start_date = end_date - timedelta(days=2)
    
    records, _ = await food_service.get_food_records(
        user_email, start_date=start_date, end_date=end_date, limit=100
    )
    
    # 计算营养数据
    total_protein = 0.0
    total_carbs = 0.0
    total_fat = 0.0
    
    for record in records:
        nutrition = record.get("nutrition_data", {})
        total_protein += nutrition.get("protein", 0.0)
        total_carbs += nutrition.get("carbohydrates", 0.0)
        total_fat += nutrition.get("fat", 0.0)
    
    # 计算比例
    protein_cals = total_protein * 4
    carbs_cals = total_carbs * 4
    fat_cals = total_fat * 9
    total_macro_cals = protein_cals + carbs_cals + fat_cals
    
    if total_macro_cals > 0:
        protein_percent = protein_cals / total_macro_cals * 100
        carbs_percent = carbs_cals / total_macro_cals * 100
        fat_percent = fat_cals / total_macro_cals * 100
    else:
        protein_percent = carbs_percent = fat_percent = 33.0  # 无记录时假设均衡
    
    # 3. 确定营养需求
    nutrition_needs = []
    if protein_percent < 15:
        nutrition_needs.append("蛋白质")
    if carbs_percent < 45:
        nutrition_needs.append("碳水化合物")
    if fat_percent < 20:
        nutrition_needs.append("健康脂肪")
    
    if not nutrition_needs:
        nutrition_needs = ["均衡营养"]
    
    # 4. 使用 LLM 推荐菜式
    prompt = (
        f"你是一个亲切的营养师小助手。请根据以下信息推荐一道适合的菜式。\n\n"
        f"当前时间：{now.strftime('%H:%M')}（{meal_type}时间）\n"
        f"用户近期营养配比：蛋白质{protein_percent:.0f}%，碳水{carbs_percent:.0f}%，脂肪{fat_percent:.0f}%\n"
        f"需要补充的营养：{', '.join(nutrition_needs)}\n\n"
        f"请输出一个 JSON 对象，格式如下：\n"
        f'{{\n'
        f'  "dish": "菜式名称",\n'
        f'  "reason": "推荐理由（简短）",\n'
        f'  "highlight": "营养亮点"\n'
        f'}}\n\n'
        f"要求：\n"
        f"1. 推荐的菜式要符合{meal_type}的特点\n"
        f"2. 优先补充用户缺乏的营养素\n"
        f"3. 菜式要常见、易获取\n"
        f"4. 只输出 JSON，不要其他解释"
    )
    
    try:
        raw_response = call_qwen_vl_with_url(
            image_url=None,
            prompt=prompt,
            model="qwen3-vl-flash"
        )
        
        clean_json = raw_response.replace("```json", "").replace("```", "").strip()
        recommendation = json.loads(clean_json)
        
        dish = recommendation.get("dish", "营养套餐")
        reason = recommendation.get("reason", f"可以补充{nutrition_needs[0]}")
        highlight = recommendation.get("highlight", "营养均衡")
        
    except Exception:
        # 默认推荐
        if meal_type == "早餐":
            dish = "鸡蛋牛奶燕麦粥"
            reason = "营养丰富，开启活力一天"
            highlight = "高蛋白、低GI"
        elif meal_type == "午餐":
            dish = "鸡胸肉沙拉"
            reason = "补充优质蛋白质"
            highlight = "高蛋白、低脂肪"
        elif meal_type == "晚餐":
            dish = "清蒸鱼配时蔬"
            reason = "清淡营养，易消化"
            highlight = "优质蛋白、低热量"
        else:
            dish = "希腊酸奶配坚果"
            reason = "健康小食，补充能量"
            highlight = "蛋白质、健康脂肪"
    
    # 5. 生成亲和的推荐语
    need_str = nutrition_needs[0] if nutrition_needs else "营养"
    message = f"{meal_reminder}！向你推荐{dish}，可以补充{need_str}～😋"
    
    return MealRecommendationResponse(
        success=True,
        message=message,
        meal_type=meal_type,
        recommended_dish=dish,
        reason=reason,
        nutrition_highlight=highlight
    )

