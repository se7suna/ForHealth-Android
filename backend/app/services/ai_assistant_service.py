from __future__ import annotations

import json
from pathlib import Path
from typing import List, Dict, Any

from fastapi import UploadFile, HTTPException, status

from app.config import settings
from app.models.food import NutritionData
from app.schemas.ai_assistant import (
    FoodImageRecognitionResponse,
    RecognizedFoodItemResponse,
    FoodRecognitionConfirmRequest,
    FoodRecognitionConfirmResponse,
)
from app.schemas.food import FoodRecordCreateRequest, FoodCreateRequest
from app.services import food_service
from app.utils.image_storage import save_food_image, get_image_url, validate_image_file
from app.utils.qwen_vl_client import call_qwen_vl_with_local_file


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
    确认识别结果并将食物添加到饮食日志。

    仅对带有 food_id 的项创建正式食物记录：
    - 根据 food_id 查询本地食物，读取其标准 serving_size；
    - 使用 recognized_food.serving_size / 标准份量 得到 serving_amount；
    - 调用已有的 create_food_record 逻辑，保证与其它入口行为一致。
    """
    created_ids: List[str] = []

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

        record_req = FoodRecordCreateRequest(
            food_id=food_id,
            source="local",
            serving_amount=serving_amount,
            recorded_at=payload.recorded_at,
            meal_type=payload.meal_type,
            notes=payload.notes,
        )

        record = await food_service.create_food_record(user_email, record_req)
        created_ids.append(record["_id"])

    success = len(created_ids) > 0
    message = (
        f"成功添加 {len(created_ids)} 条食物记录到饮食日志"
        if success
        else "未能创建任何食物记录（可能所有识别项都缺少 food_id）"
    )

    return FoodRecognitionConfirmResponse(
        success=success,
        message=message,
        created_records=created_ids,
        total_records=len(created_ids),
    )


