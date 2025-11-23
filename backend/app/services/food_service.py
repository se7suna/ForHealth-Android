from typing import Optional, List, Dict, Any
from datetime import datetime, date
from app.database import get_database
from app.models.food import (
    FoodInDB,
    FoodRecordInDB,
    NutritionData,
    FullNutritionData,
    BooheeFoodSearchItem,
)
from app.schemas.food import (
    FoodCreateRequest,
    FoodUpdateRequest,
    FoodRecordCreateRequest,
    FoodRecordUpdateRequest,
)
from app.services import external_api_service
from bson import ObjectId


# ========== 食物管理 ==========
async def create_food(food_data: FoodCreateRequest, creator_email: Optional[str] = None) -> dict:
    """
    创建食物
    
    Args:
        food_data: 食物数据
        creator_email: 创建者邮箱（None表示系统食物）
    
    Returns:
        创建的食物信息
    
    Raises:
        ValueError: 如果食物名称已存在
    """
    db = get_database()
    
    # 检查食物名称是否已存在
    existing_food = await db.foods.find_one({"name": food_data.name})
    if existing_food:
        raise ValueError(f"食物名称 '{food_data.name}' 已存在，请使用其他名称")
    
    food = FoodInDB(
        name=food_data.name,
        category=food_data.category,
        serving_size=food_data.serving_size,
        serving_unit=food_data.serving_unit,
        nutrition_per_serving=food_data.nutrition_per_serving,
        full_nutrition=food_data.full_nutrition,
        brand=food_data.brand,
        barcode=food_data.barcode,
        image_url=None,  # 图片通过文件上传单独处理，不在创建时设置
        source="local",
        created_by=creator_email,
    )
    
    food_dict = food.dict()
    result = await db.foods.insert_one(food_dict)
    food_dict["_id"] = str(result.inserted_id)
    
    return food_dict


async def get_food_by_id(food_id: str) -> Optional[dict]:
    """根据ID获取食物信息"""
    db = get_database()
    try:
        food = await db.foods.find_one({"_id": ObjectId(food_id)})
        if food:
            food["_id"] = str(food["_id"])
        return food
    except Exception:
        return None


async def get_boohee_food_by_identifier(
    identifier: str,
    include_full_nutrition: bool = True,
) -> Optional[Dict[str, Any]]:
    """根据薄荷健康的 boohee_id 获取食物信息"""
    if not identifier:
        return None

    identifier = identifier.strip()
    if not identifier:
        return None

    try:
        boohee_id = int(identifier)
    except (TypeError, ValueError):
        return None

    return await external_api_service.get_food_by_boohee_id(
        boohee_id,
        include_full_nutrition=include_full_nutrition,
    )


async def get_food_by_barcode(barcode: str) -> Optional[dict]:
    """
    根据条形码获取食物信息
    
    Args:
        barcode: 食物条形码
    
    Returns:
        食物信息字典，如果未找到则返回 None
    """
    db = get_database()
    food = await db.foods.find_one({"barcode": barcode})
    if food:
        food["_id"] = str(food["_id"])
    return food


def _normalize_nutrition_for_search(nutrition: Optional[Dict[str, Any]]) -> Dict[str, Any]:
    base = {
        "calories": 0.0,
        "protein": 0.0,
        "carbohydrates": 0.0,
        "fat": 0.0,
        "fiber": None,
        "sugar": None,
        "sodium": None,
    }

    if isinstance(nutrition, NutritionData):
        nutrition = nutrition.dict()

    if isinstance(nutrition, dict):
        for key in base.keys():
            if key in nutrition and nutrition[key] is not None:
                base[key] = nutrition[key]

    return base


def _convert_local_food_to_search_item(
    food: dict,
    include_full_nutrition: bool,
) -> Dict[str, Any]:
    nutrition = _normalize_nutrition_for_search(food.get("nutrition_per_serving"))
    full_nutrition = food.get("full_nutrition") if include_full_nutrition else None
    if isinstance(full_nutrition, FullNutritionData):
        full_nutrition = full_nutrition.dict()

    return {
        "source": food.get("source") or "local",
        "food_id": food.get("_id"),
        "boohee_id": food.get("boohee_id"),
        "boohee_code": food.get("boohee_code"),
        "code": food.get("_id"),
        "name": food.get("name"),
        "weight": float(food.get("serving_size") or 100.0),
        "weight_unit": food.get("serving_unit") or "克",
        "calory": float(nutrition.get("calories", 0.0) or 0.0),
        "image_url": food.get("image_url"),
        "is_liquid": None,
        "health_light": None,
        "brand": food.get("brand"),
        "barcode": food.get("barcode"),
        "nutrition_per_serving": nutrition,
        "full_nutrition": full_nutrition,
    }


def _nutrition_dict_to_model(nutrition: Dict[str, Any]) -> NutritionData:
    normalized = _normalize_nutrition_for_search(nutrition)
    return NutritionData(
        calories=normalized["calories"],
        protein=normalized["protein"],
        carbohydrates=normalized["carbohydrates"],
        fat=normalized["fat"],
        fiber=normalized.get("fiber"),
        sugar=normalized.get("sugar"),
        sodium=normalized.get("sodium"),
    )


async def _cache_boohee_food_item(
    boohee_item: Dict[str, Any]
) -> Optional[dict]:
    db = get_database()

    boohee_id = boohee_item.get("boohee_id")
    boohee_code = boohee_item.get("code")
    barcode = boohee_item.get("barcode")

    filters = []
    if boohee_id is not None:
        filters.append({"boohee_id": boohee_id})
    if boohee_code:
        filters.append({"boohee_code": boohee_code})
    if barcode:
        filters.append({"barcode": barcode})

    existing = None
    if filters:
        existing = await db.foods.find_one({"$or": filters})

    serving_size = float(boohee_item.get("weight") or 100.0)
    serving_unit = boohee_item.get("weight_unit") or "克"

    nutrition_model = _nutrition_dict_to_model(boohee_item.get("nutrition_per_serving") or {})
    full_nutrition_data = None
    if boohee_item.get("full_nutrition"):
        try:
            full_nutrition_data = FullNutritionData(**boohee_item["full_nutrition"])
        except Exception:
            full_nutrition_data = None

    payload = {
        "name": boohee_item.get("name") or "未命名食物",
        "category": None,
        "serving_size": serving_size,
        "serving_unit": serving_unit,
        "nutrition_per_serving": nutrition_model.dict(),
        "full_nutrition": full_nutrition_data.dict() if full_nutrition_data else None,
        "brand": boohee_item.get("brand"),
        "barcode": barcode,
        "image_url": boohee_item.get("image_url"),
        "source": "boohee",
        "boohee_id": boohee_id,
        "boohee_code": boohee_code,
        "updated_at": datetime.utcnow(),
    }

    if existing:
        await db.foods.update_one(
            {"_id": existing["_id"]},
            {"$set": payload}
        )
        existing.update(payload)
        existing["_id"] = str(existing["_id"])
        return existing

    payload["created_at"] = datetime.utcnow()
    payload["created_by"] = "all"  # 薄荷食物所有人可见

    result = await db.foods.insert_one(payload)
    payload["_id"] = str(result.inserted_id)
    return payload


async def _search_local_foods(
    keyword: Optional[str],
    user_email: Optional[str],
    include_full_nutrition: bool,
    limit: int = 20,
) -> List[Dict[str, Any]]:
    db = get_database()
    remaining = max(limit, 0)
    results: List[Dict[str, Any]] = []

    def build_query(created_by_value: str) -> Dict[str, Any]:
        filters = []
        filters.append({"created_by": created_by_value})

        if keyword:
            filters.append({
                "$or": [
                    {"name": {"$regex": keyword, "$options": "i"}},
                    {"brand": {"$regex": keyword, "$options": "i"}},
                ]
            })

        return {"$and": filters} if filters else {}

    async def fetch_and_append(query: Dict[str, Any], remaining_limit: int) -> int:
        if remaining_limit <= 0:
            return 0
        cursor_inner = db.foods.find(query).sort("created_at", -1).limit(remaining_limit)
        foods_inner = await cursor_inner.to_list(length=remaining_limit)
        for food in foods_inner:
            food_id = str(food["_id"])
            food["_id"] = food_id
            results.append(
                _convert_local_food_to_search_item(
                    food,
                    include_full_nutrition=include_full_nutrition,
                )
            )
        return remaining_limit - len(foods_inner)

    # 先获取用户自建食物
    if user_email:
        remaining = await fetch_and_append(build_query(user_email), remaining)

    # 再获取所有人可见的食物 (created_by="all")
    remaining = await fetch_and_append(build_query("all"), remaining)

    # 通过Pydantic规范化数据结构
    normalized: List[Dict[str, Any]] = []
    for item in results:
        try:
            normalized.append(BooheeFoodSearchItem(**item).dict())
        except Exception:
            normalized.append(item)

    return normalized


async def search_local_foods_only(
    keyword: Optional[str],
    user_email: Optional[str],
    limit: int = 20,
) -> List[Dict[str, Any]]:
    """
    仅搜索本地数据库中的食物（不调用薄荷API）
    
    用户自己创建的食物会排在最前面
    
    Args:
        keyword: 搜索关键词
        user_email: 用户邮箱
        limit: 返回数量限制
    
    Returns:
        本地食物列表，用户自己的食物在前，公共食物在后
    """
    db = get_database()
    
    # 构建关键词搜索条件
    keyword_condition = {}
    if keyword:
        keyword_condition = {
            "$or": [
                {"name": {"$regex": keyword, "$options": "i"}},
                {"brand": {"$regex": keyword, "$options": "i"}},
            ]
        }
    
    result = []
    
    # 第一步：查询用户自己创建的食物（优先显示）
    if user_email:
        user_query = {"created_by": user_email}
        if keyword_condition:
            user_query.update(keyword_condition)
        
        user_cursor = db.foods.find(user_query).sort("created_at", -1).limit(limit)
        user_foods = await user_cursor.to_list(length=limit)
        
        for food in user_foods:
            food["_id"] = str(food["_id"])
            food["food_id"] = str(food["_id"])
            result.append(food)
    
    # 第二步：查询公共食物（created_by="all"）
    remaining = limit - len(result)
    if remaining > 0:
        public_query = {"created_by": "all"}
        if keyword_condition:
            public_query.update(keyword_condition)
        
        public_cursor = db.foods.find(public_query).sort("created_at", -1).limit(remaining)
        public_foods = await public_cursor.to_list(length=remaining)
        
        for food in public_foods:
            food["_id"] = str(food["_id"])
            food["food_id"] = str(food["_id"])
            result.append(food)
    
    return result


async def search_foods(
    keyword: Optional[str] = None,
    page: int = 1,
    include_full_nutrition: bool = True,
    user_email: Optional[str] = None,
) -> Dict[str, Any]:
    """优先搜索本地食物，再从薄荷健康补充结果"""

    local_foods = await _search_local_foods(
        keyword=keyword,
        user_email=user_email,
        include_full_nutrition=include_full_nutrition,
    )

    boohee_result = {"page": page, "total_pages": 0, "foods": []}

    # 只有提供关键词时才调用薄荷健康接口
    if keyword:
        boohee_result = await external_api_service.search_foods(
            keyword=keyword,
            page=page,
            include_full_nutrition=include_full_nutrition,
        ) or boohee_result

    cached_boohee_search_items: List[Dict[str, Any]] = []
    for item in boohee_result.get("foods", []):
        try:
            boohee_dict = BooheeFoodSearchItem(**item).dict()
        except Exception:
            boohee_dict = item

        cached_food = await _cache_boohee_food_item(boohee_dict)
        if cached_food:
            cached_boohee_search_items.append(
                _convert_local_food_to_search_item(
                    cached_food,
                    include_full_nutrition=include_full_nutrition,
                )
            )

    combined_foods: List[Dict[str, Any]] = []
    seen_codes = set()

    for item in local_foods + cached_boohee_search_items:
        code = item.get("code")
        if code and code in seen_codes:
            continue
        if code:
            seen_codes.add(code)
        combined_foods.append(item)

    return {
        "page": boohee_result.get("page", page),
        "total_pages": boohee_result.get("total_pages", 0),
        "foods": combined_foods,
    }


def _scale_nutrition(nutrition: Any, factor: float) -> NutritionData:
    if isinstance(nutrition, NutritionData):
        nutrition_dict = nutrition.dict()
    else:
        nutrition_dict = dict(nutrition or {})

    optional_keys = {"fiber", "sugar", "sodium"}
    result: Dict[str, Any] = {}

    for key in ["calories", "protein", "carbohydrates", "fat", "fiber", "sugar", "sodium"]:
        value = nutrition_dict.get(key)
        if value is None:
            result[key] = None if key in optional_keys else 0.0
        else:
            try:
                numeric_value = float(value)
            except (TypeError, ValueError):
                numeric_value = 0.0
            result[key] = round(numeric_value * factor, 2)

    return NutritionData(**result)


def _normalize_full_nutrition(full_nutrition: Any) -> Optional[Dict[str, Any]]:
    if not full_nutrition:
        return None
    if isinstance(full_nutrition, FullNutritionData):
        return full_nutrition.dict()
    return dict(full_nutrition)


def _scale_full_nutrition(full_nutrition: Optional[Dict[str, Any]], factor: float) -> Optional[Dict[str, Any]]:
    if not full_nutrition:
        return None

    factor = float(factor)
    if factor == 1.0:
        return full_nutrition

    def _scale_value(value: Any) -> Any:
        if value is None:
            return None
        try:
            return round(float(value) * factor, 4)
        except (TypeError, ValueError):
            return value

    def _scale_items(items: Any) -> Optional[List[Dict[str, Any]]]:
        if not items:
            return []
        scaled_items: List[Dict[str, Any]] = []
        for item in items:
            item_dict = dict(item)
            item_dict["value"] = _scale_value(item_dict.get("value"))
            if "items" in item_dict and item_dict.get("items"):
                item_dict["items"] = _scale_items(item_dict.get("items"))
            scaled_items.append(item_dict)
        return scaled_items

    scaled = {}
    for key in [
        "calory",
        "base_ingredients",
        "vitamin",
        "mineral",
        "amino_acid",
        "other_ingredients",
    ]:
        items = full_nutrition.get(key)
        if not items:
            scaled[key] = []
            continue
        if key == "base_ingredients":
            scaled[key] = []
            for ingredient in items:
                ingredient_dict = dict(ingredient)
                ingredient_dict["value"] = _scale_value(ingredient_dict.get("value"))
                if ingredient_dict.get("items"):
                    ingredient_dict["items"] = _scale_items(ingredient_dict.get("items"))
                else:
                    ingredient_dict["items"] = [] if ingredient_dict.get("items") is None else ingredient_dict.get("items")
                scaled[key].append(ingredient_dict)
        else:
            scaled[key] = []
            for item in items:
                item_dict = dict(item)
                item_dict["value"] = _scale_value(item_dict.get("value"))
                scaled[key].append(item_dict)

    return scaled


async def update_food(
    food_id: str,
    food_data: FoodUpdateRequest,
    user_email: str
) -> Optional[dict]:
    """
    更新食物信息（仅创建者可更新）
    
    Args:
        food_id: 食物ID
        food_data: 更新的食物数据
        user_email: 用户邮箱
    
    Returns:
        更新后的食物信息，如果失败则返回None
    
    Raises:
        ValueError: 如果更新的名称与其他食物重名
    """
    db = get_database()
    
    # 获取要更新的食物
    try:
        food = await db.foods.find_one({
            "_id": ObjectId(food_id),
            "created_by": user_email
        })
        if not food:
            return None
    except Exception:
        return None
    
    # 构建更新数据（只更新提供的字段）
    update_data = {}
    if food_data.name is not None:
        # 检查新名称是否与其他食物重名
        existing_food = await db.foods.find_one({
            "name": food_data.name,
            "_id": {"$ne": ObjectId(food_id)}
        })
        if existing_food:
            raise ValueError(f"食物名称 '{food_data.name}' 已存在，请使用其他名称")
        update_data["name"] = food_data.name
    
    if food_data.category is not None:
        update_data["category"] = food_data.category
    if food_data.serving_size is not None:
        update_data["serving_size"] = food_data.serving_size
    if food_data.serving_unit is not None:
        update_data["serving_unit"] = food_data.serving_unit
    if food_data.nutrition_per_serving is not None:
        update_data["nutrition_per_serving"] = food_data.nutrition_per_serving.dict()
    if food_data.full_nutrition is not None:
        update_data["full_nutrition"] = food_data.full_nutrition.dict() if food_data.full_nutrition else None
    if food_data.brand is not None:
        update_data["brand"] = food_data.brand
    if food_data.barcode is not None:
        update_data["barcode"] = food_data.barcode
    if food_data.image_url is not None:
        update_data["image_url"] = food_data.image_url
    
    if not update_data:
        # 没有要更新的字段
        food["_id"] = str(food["_id"])
        return food
    
    update_data["updated_at"] = datetime.utcnow()
    
    # 执行更新
    result = await db.foods.find_one_and_update(
        {"_id": ObjectId(food_id), "created_by": user_email},
        {"$set": update_data},
        return_document=True
    )
    
    if result:
        result["_id"] = str(result["_id"])
    
    return result


async def update_food_image_url(food_id: str, image_url: str, user_email: str) -> bool:
    """
    更新食物的图片URL
    
    Args:
        food_id: 食物ID
        image_url: 图片URL
        user_email: 用户邮箱
    
    Returns:
        是否更新成功
    
    Raises:
        ValueError: 如果食物不存在或无权更新
    """
    db = get_database()
    try:
        # 先检查食物是否存在且有权限
        food = await db.foods.find_one({"_id": ObjectId(food_id)})
        if not food:
            raise ValueError(f"食物不存在 (ID: {food_id})")
        
        if food.get("created_by") != user_email:
            raise ValueError(f"无权更新此食物的图片 (创建者: {food.get('created_by')}, 当前用户: {user_email})")
        
        # 执行更新
        result = await db.foods.update_one(
            {"_id": ObjectId(food_id), "created_by": user_email},
            {"$set": {"image_url": image_url, "updated_at": datetime.utcnow()}}
        )
        
        if result.modified_count == 0:
            raise ValueError("更新失败：未找到匹配的记录")
        
        return True
    except ValueError:
        # 重新抛出业务异常
        raise
    except Exception as e:
        # 其他异常转换为ValueError
        raise ValueError(f"更新图片URL时发生错误：{str(e)}")


async def delete_food(food_id: str, user_email: str) -> bool:
    """
    删除食物（仅创建者可删除）
    
    注意：同时会删除关联的图片文件
    
    Args:
        food_id: 食物ID
        user_email: 用户邮箱
    
    Returns:
        是否删除成功
    """
    db = get_database()
    try:
        # 先获取食物信息，以便删除关联的图片
        food = await db.foods.find_one({
            "_id": ObjectId(food_id),
            "created_by": user_email
        })
        
        if not food:
            return False
        
        # 删除关联的图片文件
        image_url = food.get("image_url")
        if image_url:
            from app.utils.image_storage import delete_food_image
            delete_food_image(image_url)
        
        # 删除食物记录
        result = await db.foods.delete_one({
            "_id": ObjectId(food_id),
            "created_by": user_email
        })
        return result.deleted_count > 0
    except Exception:
        return False


# ========== 食物记录管理 ==========
async def create_food_record(
    user_email: str,
    record_data: FoodRecordCreateRequest
) -> dict:
    """
    创建食物记录
    
    Args:
        user_email: 用户邮箱
        record_data: 记录数据
    
    Returns:
        创建的记录信息
    """
    source_option = (record_data.source or "auto").lower()
    if source_option not in {"local", "auto"}:
        raise ValueError("source 参数仅支持 local 或 auto")

    db = get_database()

    local_food = await get_food_by_id(record_data.food_id)
    if not local_food:
        raise ValueError("食物不存在")

    # 权限检查：created_by="all" 所有人可见，否则只有创建者可见
    if local_food.get("created_by") != "all" and local_food.get("created_by") != user_email:
        raise ValueError("无权访问此食物")

    serving_amount = record_data.serving_amount

    base_serving_size = float(local_food.get("serving_size") or 100.0)
    serving_unit = local_food.get("serving_unit") or "克"
    base_nutrition = local_food.get("nutrition_per_serving") or {}
    full_nutrition = _normalize_full_nutrition(local_food.get("full_nutrition"))
    food_name = local_food.get("name", "未命名食物")
    food_identifier = local_food.get("_id")

    nutrition_snapshot = _scale_nutrition(base_nutrition, serving_amount)
    full_nutrition_snapshot_dict = _scale_full_nutrition(full_nutrition, serving_amount)
    full_nutrition_snapshot = (
        FullNutritionData(**full_nutrition_snapshot_dict)
        if full_nutrition_snapshot_dict
        else None
    )

    record = FoodRecordInDB(
        user_email=user_email,
        food_name=food_name,
        serving_amount=serving_amount,
        serving_size=base_serving_size,
        serving_unit=serving_unit,
        nutrition_data=nutrition_snapshot,
        full_nutrition=full_nutrition_snapshot,
        recorded_at=record_data.recorded_at,
        meal_type=record_data.meal_type,
        notes=record_data.notes,
        food_id=food_identifier,
    )

    record_dict = record.dict()
    result = await db.food_records.insert_one(record_dict)
    record_dict["_id"] = str(result.inserted_id)

    return record_dict


async def get_food_records(
    user_email: str,
    start_date: Optional[date] = None,
    end_date: Optional[date] = None,
    meal_type: Optional[str] = None,
    limit: int = 100,
    offset: int = 0,
) -> tuple[List[dict], int]:
    """
    获取用户的食物记录
    
    Args:
        user_email: 用户邮箱
        start_date: 开始日期
        end_date: 结束日期
        meal_type: 餐次类型
        limit: 返回数量限制
        offset: 偏移量
    
    Returns:
        (记录列表, 总数量)
    """
    db = get_database()
    
    # 构建查询条件
    query = {"user_email": user_email}
    
    # 日期范围筛选
    if start_date or end_date:
        query["recorded_at"] = {}
        if start_date:
            query["recorded_at"]["$gte"] = datetime.combine(start_date, datetime.min.time())
        if end_date:
            query["recorded_at"]["$lte"] = datetime.combine(end_date, datetime.max.time())
    
    # 餐次筛选
    if meal_type:
        query["meal_type"] = meal_type
    
    # 查询总数
    total = await db.food_records.count_documents(query)
    
    # 查询记录列表
    cursor = db.food_records.find(query).skip(offset).limit(limit).sort("recorded_at", -1)
    records = await cursor.to_list(length=limit)
    
    # 转换 ObjectId 为字符串
    for record in records:
        record["_id"] = str(record["_id"])
    
    return records, total


async def get_daily_nutrition_summary(
    user_email: str,
    target_date: date
) -> Dict:
    """
    获取某日的营养摘要
    
    Args:
        user_email: 用户邮箱
        target_date: 目标日期
    
    Returns:
        营养摘要数据
    """
    db = get_database()
    
    # 查询当天的所有记录
    start_datetime = datetime.combine(target_date, datetime.min.time())
    end_datetime = datetime.combine(target_date, datetime.max.time())
    
    query = {
        "user_email": user_email,
        "recorded_at": {
            "$gte": start_datetime,
            "$lte": end_datetime
        }
    }
    
    cursor = db.food_records.find(query).sort("recorded_at", 1)
    records = await cursor.to_list(length=None)
    
    # 转换 ObjectId 为字符串
    for record in records:
        record["_id"] = str(record["_id"])
    
    # 计算总营养
    total_calories = 0.0
    total_protein = 0.0
    total_carbohydrates = 0.0
    total_fat = 0.0
    
    for record in records:
        nutrition = record.get("nutrition_data", {})
        total_calories += nutrition.get("calories", 0)
        total_protein += nutrition.get("protein", 0)
        total_carbohydrates += nutrition.get("carbohydrates", 0)
        total_fat += nutrition.get("fat", 0)
    
    return {
        "date": target_date.strftime("%Y-%m-%d"),
        "total_calories": round(total_calories, 2),
        "total_protein": round(total_protein, 2),
        "total_carbohydrates": round(total_carbohydrates, 2),
        "total_fat": round(total_fat, 2),
        "meal_count": len(records),
        "records": records,
    }


async def update_food_record(
    record_id: str,
    record_data: FoodRecordUpdateRequest,
    user_email: str
) -> Optional[dict]:
    """
    更新食物记录（仅记录所有者可更新）
    
    Args:
        record_id: 记录ID
        record_data: 更新的记录数据
        user_email: 用户邮箱
    
    Returns:
        更新后的记录信息，如果失败则返回None
    """
    db = get_database()
    
    # 获取要更新的记录
    try:
        record = await db.food_records.find_one({
            "_id": ObjectId(record_id),
            "user_email": user_email
        })
        if not record:
            return None
    except Exception:
        return None
    
    # 构建更新数据（只更新提供的字段）
    update_data = {}
    if record_data.food_name is not None:
        update_data["food_name"] = record_data.food_name
    if record_data.serving_amount is not None:
        update_data["serving_amount"] = record_data.serving_amount
    if record_data.serving_size is not None:
        update_data["serving_size"] = record_data.serving_size
    if record_data.serving_unit is not None:
        update_data["serving_unit"] = record_data.serving_unit
    if record_data.nutrition_data is not None:
        update_data["nutrition_data"] = record_data.nutrition_data.dict()
    if record_data.full_nutrition is not None:
        update_data["full_nutrition"] = record_data.full_nutrition.dict() if record_data.full_nutrition else None
    if record_data.recorded_at is not None:
        update_data["recorded_at"] = record_data.recorded_at
    if record_data.meal_type is not None:
        update_data["meal_type"] = record_data.meal_type
    if record_data.notes is not None:
        update_data["notes"] = record_data.notes
    
    if not update_data:
        # 没有要更新的字段
        record["_id"] = str(record["_id"])
        return record
    
    # 执行更新
    result = await db.food_records.find_one_and_update(
        {"_id": ObjectId(record_id), "user_email": user_email},
        {"$set": update_data},
        return_document=True
    )
    
    if result:
        result["_id"] = str(result["_id"])
    
    return result


async def delete_food_record(record_id: str, user_email: str) -> bool:
    """
    删除食物记录
    
    Args:
        record_id: 记录ID
        user_email: 用户邮箱
    
    Returns:
        是否删除成功
    """
    db = get_database()
    try:
        result = await db.food_records.delete_one({
            "_id": ObjectId(record_id),
            "user_email": user_email
        })
        return result.deleted_count > 0
    except Exception:
        return False


async def calculate_total_nutrition(records: List[dict]) -> NutritionData:
    """
    计算记录列表的总营养
    
    Args:
        records: 记录列表
    
    Returns:
        总营养数据
    """
    total = {
        "calories": 0.0,
        "protein": 0.0,
        "carbohydrates": 0.0,
        "fat": 0.0,
        "fiber": 0.0,
        "sugar": 0.0,
        "sodium": 0.0,
    }
    
    for record in records:
        nutrition = record.get("nutrition_data", {})
        total["calories"] += nutrition.get("calories", 0)
        total["protein"] += nutrition.get("protein", 0)
        total["carbohydrates"] += nutrition.get("carbohydrates", 0)
        total["fat"] += nutrition.get("fat", 0)
        total["fiber"] += nutrition.get("fiber", 0) or 0
        total["sugar"] += nutrition.get("sugar", 0) or 0
        total["sodium"] += nutrition.get("sodium", 0) or 0
    
    # 四舍五入
    for key in total:
        total[key] = round(total[key], 2)
    
    return NutritionData(**total)

