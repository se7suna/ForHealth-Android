from typing import Optional, List, Dict, Any
from datetime import datetime
from fastapi import UploadFile, HTTPException, status
from app.database import get_database
from app.models.recipe import RecipeInDB
from app.models.food import NutritionData, FullNutritionData
from app.schemas.recipe import RecipeCreateRequest, RecipeUpdateRequest
from app.utils.image_storage import save_recipe_image, get_image_url, delete_recipe_image
from bson import ObjectId


def calculate_recipe_full_nutrition(foods: list) -> Optional[FullNutritionData]:
    """
    计算食谱的总完整营养信息（与测试脚本格式一致）
    
    将各个食物的完整营养信息汇总
    
    Args:
        foods: 食物列表
    
    Returns:
        总完整营养信息，如果没有任何食物的完整营养信息则返回None
    """
    # 初始化汇总字典
    total_full_nutrition = {
        "calory": [],
        "base_ingredients": [],
        "vitamin": [],
        "mineral": [],
        "amino_acid": [],
        "other_ingredients": []
    }
    
    has_full_nutrition = False
    
    def _to_dict(value: Any) -> Dict[str, Any]:
        if value is None:
            return {}
        if isinstance(value, dict):
            return value
        if hasattr(value, "dict"):
            return value.dict()
        return dict(value)

    for food_item in foods:
        # 获取食物的完整营养信息
        full_nutrition_raw = None
        if hasattr(food_item, 'full_nutrition') and getattr(food_item, 'full_nutrition'):
            full_nutrition_raw = getattr(food_item, 'full_nutrition')
        elif isinstance(food_item, dict) and food_item.get('full_nutrition'):
            full_nutrition_raw = food_item.get('full_nutrition')

        if not full_nutrition_raw:
            continue
        
        full_nutrition = _to_dict(full_nutrition_raw)
        if not full_nutrition:
            continue

        has_full_nutrition = True
        if hasattr(food_item, 'serving_amount'):
            serving_amount = getattr(food_item, 'serving_amount') or 1.0
        elif isinstance(food_item, dict):
            serving_amount = food_item.get('serving_amount', 1.0)
        else:
            serving_amount = 1.0
        
        # 汇总各个营养类别（需要按比例计算）
        if full_nutrition.get('calory'):
            for item in full_nutrition['calory']:
                item_dict = _to_dict(item)
                scaled_item = item_dict.copy()
                scaled_item['value'] = item_dict.get('value', 0) * serving_amount
                total_full_nutrition['calory'].append(scaled_item)
        
        if full_nutrition.get('base_ingredients'):
            for item in full_nutrition['base_ingredients']:
                item_dict = _to_dict(item)
                scaled_item = item_dict.copy()
                scaled_item['value'] = item_dict.get('value', 0) * serving_amount
                if scaled_item.get('items'):
                    scaled_items = []
                    for sub_item in scaled_item['items']:
                        sub_item_dict = _to_dict(sub_item)
                        scaled_sub_item = sub_item_dict.copy()
                        scaled_sub_item['value'] = sub_item_dict.get('value', 0) * serving_amount
                        scaled_items.append(scaled_sub_item)
                    scaled_item['items'] = scaled_items
                total_full_nutrition['base_ingredients'].append(scaled_item)
        
        if full_nutrition.get('vitamin'):
            for item in full_nutrition['vitamin']:
                item_dict = _to_dict(item)
                scaled_item = item_dict.copy()
                scaled_item['value'] = item_dict.get('value', 0) * serving_amount
                total_full_nutrition['vitamin'].append(scaled_item)
        
        if full_nutrition.get('mineral'):
            for item in full_nutrition['mineral']:
                item_dict = _to_dict(item)
                scaled_item = item_dict.copy()
                scaled_item['value'] = item_dict.get('value', 0) * serving_amount
                total_full_nutrition['mineral'].append(scaled_item)
        
        if full_nutrition.get('amino_acid'):
            for item in full_nutrition['amino_acid']:
                item_dict = _to_dict(item)
                scaled_item = item_dict.copy()
                scaled_item['value'] = item_dict.get('value', 0) * serving_amount
                total_full_nutrition['amino_acid'].append(scaled_item)
        
        if full_nutrition.get('other_ingredients'):
            for item in full_nutrition['other_ingredients']:
                item_dict = _to_dict(item)
                scaled_item = item_dict.copy()
                scaled_item['value'] = item_dict.get('value', 0) * serving_amount
                total_full_nutrition['other_ingredients'].append(scaled_item)
    
    if not has_full_nutrition:
        return None
    
    return FullNutritionData(**total_full_nutrition)


def calculate_recipe_nutrition(foods: list) -> NutritionData:
    """计算食谱的总营养"""
    total = {
        "calories": 0.0,
        "protein": 0.0,
        "carbohydrates": 0.0,
        "fat": 0.0,
        "fiber": 0.0,
        "sodium": 0.0,
    }
    
    for food_item in foods:
        nutrition = food_item.nutrition if hasattr(food_item, 'nutrition') else food_item.get('nutrition', {})
        if isinstance(nutrition, dict):
            total["calories"] += nutrition.get("calories", 0)
            total["protein"] += nutrition.get("protein", 0)
            total["carbohydrates"] += nutrition.get("carbohydrates", 0)
            total["fat"] += nutrition.get("fat", 0)
            total["fiber"] += nutrition.get("fiber", 0) or 0
            total["sodium"] += nutrition.get("sodium", 0) or 0
        else:
            total["calories"] += nutrition.calories
            total["protein"] += nutrition.protein
            total["carbohydrates"] += nutrition.carbohydrates
            total["fat"] += nutrition.fat
            total["fiber"] += nutrition.fiber or 0
            total["sodium"] += nutrition.sodium or 0
    
    for key in total:
        total[key] = round(total[key], 2)
    
    return NutritionData(**total)


async def create_recipe(
    recipe_data: RecipeCreateRequest,
    creator_email: Optional[str] = None
) -> dict:
    """
    创建食谱
    
    Args:
        recipe_data: 食谱数据
        creator_email: 创建者邮箱
    
    Returns:
        创建的食谱信息
    
    Raises:
        ValueError: 如果食谱名称已存在
    """
    db = get_database()
    
    # 检查食谱名称是否已存在
    existing_recipe = await db.recipes.find_one({"name": recipe_data.name})
    if existing_recipe:
        raise ValueError(f"食谱名称 '{recipe_data.name}' 已存在，请使用其他名称")
    
    # 计算总营养
    total_nutrition = calculate_recipe_nutrition(recipe_data.foods)
    # 计算总完整营养信息（如果所有食物都有完整营养信息）
    total_full_nutrition = calculate_recipe_full_nutrition(recipe_data.foods)
    
    recipe = RecipeInDB(
        name=recipe_data.name,
        description=recipe_data.description,
        category=recipe_data.category,
        foods=[food.dict() for food in recipe_data.foods],
        total_nutrition=total_nutrition,
        total_full_nutrition=total_full_nutrition,
        tags=recipe_data.tags,
        image_url=None,  # 图片通过文件上传单独处理，不在创建时设置
        prep_time=recipe_data.prep_time,
        created_by=creator_email,
    )
    
    recipe_dict = recipe.dict()
    result = await db.recipes.insert_one(recipe_dict)
    recipe_dict["_id"] = str(result.inserted_id)
    
    return recipe_dict


async def create_recipe_with_image(
    name: str,
    description: Optional[str],
    category: Optional[str],
    foods: List[Dict[str, Any]],
    tags: Optional[List[str]],
    prep_time: Optional[int],
    image: Optional[UploadFile],
    creator_email: str
) -> dict:
    """
    创建食谱（包含图片上传处理）
    
    Args:
        name: 食谱名称
        description: 食谱描述
        category: 分类
        foods: 食物列表
        tags: 标签
        prep_time: 准备时间
        image: 图片文件（可选）
        creator_email: 创建者邮箱
    
    Returns:
        创建的食谱信息
    
    Raises:
        ValueError: 如果食谱名称已存在
        HTTPException: 如果图片处理失败
    """
    # 构建RecipeCreateRequest对象
    from app.models.recipe import RecipeFoodItem
    recipe_foods = [RecipeFoodItem(**food) for food in foods]
    
    recipe_data = RecipeCreateRequest(
        name=name,
        description=description,
        category=category,
        foods=recipe_foods,
        tags=tags,
        image_url=None,  # 图片通过文件上传单独处理
        prep_time=prep_time,
    )
    
    # 先创建食谱（获取recipe_id）
    recipe = await create_recipe(recipe_data, creator_email)
    recipe_id = recipe["_id"]
    
    # 如果有图片，保存图片并更新食谱记录
    if image and image.filename:
        image_url = None
        try:
            # 保存图片文件
            relative_path = await save_recipe_image(image, recipe_id)
            # 生成图片URL
            image_url = get_image_url(relative_path)
            
            # 更新食谱记录，添加图片URL
            await update_recipe_image_url(recipe_id, image_url, creator_email)
            recipe["image_url"] = image_url
        except HTTPException:
            # 如果图片保存失败，删除已创建的食谱记录
            await delete_recipe(recipe_id, creator_email)
            raise
        except ValueError as e:
            # 如果更新图片URL失败，删除已创建的食谱记录和图片文件
            await delete_recipe(recipe_id, creator_email)
            # 删除已保存的图片（如果存在）
            if image_url:
                delete_recipe_image(image_url)
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"更新图片URL失败：{str(e)}"
            )
        except Exception as e:
            # 如果图片保存失败，删除已创建的食谱记录和图片文件
            await delete_recipe(recipe_id, creator_email)
            # 删除已保存的图片（如果存在）
            if image_url:
                delete_recipe_image(image_url)
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"保存图片失败：{str(e)}"
            )
    
    return recipe


async def get_recipe_by_id(recipe_id: str) -> Optional[dict]:
    """根据ID获取食谱"""
    db = get_database()
    try:
        recipe = await db.recipes.find_one({"_id": ObjectId(recipe_id)})
        if recipe:
            recipe["_id"] = str(recipe["_id"])
        return recipe
    except Exception:
        return None


async def search_recipes(
    keyword: Optional[str] = None,
    category: Optional[str] = None,
    tags: Optional[List[str]] = None,
    user_email: Optional[str] = None,
    limit: int = 20,
    offset: int = 0,
) -> tuple[List[dict], int]:
    """
    搜索食谱
    
    Args:
        keyword: 搜索关键词
        category: 分类筛选
        tags: 标签筛选
        user_email: 用户邮箱
        limit: 返回数量限制
        offset: 偏移量
    
    Returns:
        (食谱列表, 总数量)
    """
    db = get_database()
    
    conditions = []
    
    # 权限控制：只能看到系统食谱或自己创建的食谱
    if user_email:
        conditions.append({
            "$or": [
                {"created_by": "all"},  # 所有人可见的食谱
                {"created_by": user_email}  # 自己创建的食谱
            ]
        })
    else:
        # 未登录用户只能看到所有人可见的食谱
        conditions.append({"created_by": "all"})
    
    # 关键词搜索
    if keyword:
        conditions.append({
            "$or": [
                {"name": {"$regex": keyword, "$options": "i"}},
                {"description": {"$regex": keyword, "$options": "i"}},
            ]
        })
    
    # 分类筛选
    if category:
        conditions.append({"category": category})
    
    # 标签筛选
    if tags:
        conditions.append({"tags": {"$in": tags}})
    
    query = {"$and": conditions} if len(conditions) > 1 else conditions[0] if conditions else {}
    
    total = await db.recipes.count_documents(query)
    cursor = db.recipes.find(query).skip(offset).limit(limit).sort("created_at", -1)
    recipes = await cursor.to_list(length=limit)
    
    for recipe in recipes:
        recipe["_id"] = str(recipe["_id"])
    
    return recipes, total


async def get_recipe_categories(user_email: Optional[str] = None) -> List[str]:
    """获取所有食谱分类（只包含系统食谱和自己创建的食谱）"""
    db = get_database()
    
    query = {}
    if user_email:
        query["$or"] = [
            {"created_by": "all"},  # 所有人可见的食谱
            {"created_by": user_email}  # 自己创建的食谱
        ]
    else:
        # 未登录用户只能看到所有人可见的食谱
        query["created_by"] = "all"
    
    categories = await db.recipes.distinct("category", query)
    return [cat for cat in categories if cat]


async def update_recipe(
    recipe_id: str,
    recipe_data: RecipeUpdateRequest,
    user_email: str
) -> Optional[dict]:
    """
    更新食谱
    
    Args:
        recipe_id: 食谱ID
        recipe_data: 更新的食谱数据
        user_email: 用户邮箱
    
    Returns:
        更新后的食谱信息
    
    Raises:
        ValueError: 如果更新的名称与其他食谱重名
    """
    db = get_database()
    
    try:
        recipe = await db.recipes.find_one({
            "_id": ObjectId(recipe_id),
            "created_by": user_email
        })
        if not recipe:
            return None
    except Exception:
        return None
    
    update_data = {}
    
    if recipe_data.name is not None:
        existing_recipe = await db.recipes.find_one({
            "name": recipe_data.name,
            "_id": {"$ne": ObjectId(recipe_id)}
        })
        if existing_recipe:
            raise ValueError(f"食谱名称 '{recipe_data.name}' 已存在，请使用其他名称")
        update_data["name"] = recipe_data.name
    
    if recipe_data.description is not None:
        update_data["description"] = recipe_data.description
    if recipe_data.category is not None:
        update_data["category"] = recipe_data.category
    if recipe_data.foods is not None:
        foods_list = [food.dict() for food in recipe_data.foods]
        update_data["foods"] = foods_list
        # 重新计算总营养
        update_data["total_nutrition"] = calculate_recipe_nutrition(recipe_data.foods).dict()
        # 重新计算总完整营养信息
        total_full_nutrition = calculate_recipe_full_nutrition(recipe_data.foods)
        if total_full_nutrition:
            update_data["total_full_nutrition"] = total_full_nutrition.dict()
        else:
            update_data["total_full_nutrition"] = None
    if recipe_data.tags is not None:
        update_data["tags"] = recipe_data.tags
    if recipe_data.prep_time is not None:
        update_data["prep_time"] = recipe_data.prep_time
    
    if not update_data:
        recipe["_id"] = str(recipe["_id"])
        return recipe
    
    update_data["updated_at"] = datetime.utcnow()
    
    result = await db.recipes.find_one_and_update(
        {"_id": ObjectId(recipe_id), "created_by": user_email},
        {"$set": update_data},
        return_document=True
    )
    
    if result:
        result["_id"] = str(result["_id"])
    
    return result


async def update_recipe_image_url(recipe_id: str, image_url: str, user_email: str) -> bool:
    """
    更新食谱的图片URL
    
    Args:
        recipe_id: 食谱ID
        image_url: 图片URL
        user_email: 用户邮箱
    
    Returns:
        是否更新成功
    
    Raises:
        ValueError: 如果食谱不存在或无权更新
    """
    db = get_database()
    try:
        # 先检查食谱是否存在且有权限
        recipe = await db.recipes.find_one({"_id": ObjectId(recipe_id)})
        if not recipe:
            raise ValueError(f"食谱不存在 (ID: {recipe_id})")
        
        if recipe.get("created_by") != user_email:
            raise ValueError(f"无权更新此食谱的图片 (创建者: {recipe.get('created_by')}, 当前用户: {user_email})")
        
        # 执行更新
        result = await db.recipes.update_one(
            {"_id": ObjectId(recipe_id), "created_by": user_email},
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


async def delete_recipe(recipe_id: str, user_email: str) -> bool:
    """
    删除食谱（仅创建者可删除）
    
    注意：同时会删除关联的图片文件
    
    Args:
        recipe_id: 食谱ID
        user_email: 用户邮箱
    
    Returns:
        是否删除成功
    """
    db = get_database()
    try:
        # 先获取食谱信息，以便删除关联的图片
        recipe = await db.recipes.find_one({
            "_id": ObjectId(recipe_id),
            "created_by": user_email
        })
        
        if not recipe:
            return False
        
        # 删除关联的图片文件
        image_url = recipe.get("image_url")
        if image_url:
            delete_recipe_image(image_url)
        
        # 删除食谱记录
        result = await db.recipes.delete_one({
            "_id": ObjectId(recipe_id),
            "created_by": user_email
        })
        return result.deleted_count > 0
    except Exception:
        return False


async def create_recipe_record(
    user_email: str,
    recipe_id: str,
    scale: float,
    recorded_at: datetime,
    meal_type: Optional[str] = None,
    notes: Optional[str] = None
) -> Dict[str, Any]:
    """
    创建食谱记录（为食谱中的每个食物创建记录）
    
    Args:
        user_email: 用户邮箱
        recipe_id: 食谱ID
        scale: 份量倍数
        recorded_at: 摄入时间
        meal_type: 餐次类型
        notes: 备注
    
    Returns:
        包含创建的记录ID列表和总营养的字典
    """
    from app.models.food import FoodRecordInDB
    from bson import ObjectId as BsonObjectId
    
    db = get_database()
    
    # 生成批次ID，用于关联这次食谱记录的所有食物记录
    batch_id = str(BsonObjectId())
    
    # 获取食谱详情
    recipe = await get_recipe_by_id(recipe_id)
    if not recipe:
        raise ValueError("食谱不存在")
    
    # 权限检查：created_by="all" 所有人可见，否则只有创建者可见
    if recipe.get("created_by") != "all" and recipe.get("created_by") != user_email:
        raise ValueError("无权访问此食谱")
    
    recipe_name = recipe.get("name", "未命名食谱")
    foods = recipe.get("foods", [])
    
    if not foods:
        raise ValueError("食谱中没有食物")
    
    # 为每个食物创建记录
    record_ids = []
    all_nutrition = []
    
    for food_item in foods:
        # 获取食物信息
        food_id = food_item.get("food_id")
        food_name = food_item.get("food_name") or "未命名食物"
        
        # 使用 or 运算符处理 None 值
        serving_amount_raw = food_item.get("serving_amount")
        if serving_amount_raw is None:
            serving_amount_raw = 1.0
        serving_amount = serving_amount_raw * scale
        
        serving_size = food_item.get("serving_size")
        if serving_size is None:
            serving_size = 100.0
            
        serving_unit = food_item.get("serving_unit") or "克"
        
        # 获取营养数据并按 scale 调整
        nutrition_dict = food_item.get("nutrition", {})
        if not nutrition_dict:
            nutrition_dict = {}
            
        if isinstance(nutrition_dict, dict):
            # 使用辅助函数处理 None 值
            def get_nutrition_value(key):
                value = nutrition_dict.get(key, 0)
                return 0 if value is None else value
            
            scaled_nutrition = {
                "calories": get_nutrition_value("calories") * scale,
                "protein": get_nutrition_value("protein") * scale,
                "carbohydrates": get_nutrition_value("carbohydrates") * scale,
                "fat": get_nutrition_value("fat") * scale,
                "fiber": get_nutrition_value("fiber") * scale,
                "sugar": get_nutrition_value("sugar") * scale,
                "sodium": get_nutrition_value("sodium") * scale,
            }
        else:
            # 如果是 Pydantic 模型，转为字典
            nutrition_dict = nutrition_dict.dict() if hasattr(nutrition_dict, 'dict') else {}
            
            def get_nutrition_value(key):
                value = nutrition_dict.get(key, 0)
                return 0 if value is None else value
            
            scaled_nutrition = {
                "calories": get_nutrition_value("calories") * scale,
                "protein": get_nutrition_value("protein") * scale,
                "carbohydrates": get_nutrition_value("carbohydrates") * scale,
                "fat": get_nutrition_value("fat") * scale,
                "fiber": get_nutrition_value("fiber") * scale,
                "sugar": get_nutrition_value("sugar") * scale,
                "sodium": get_nutrition_value("sodium") * scale,
            }
        
        # 四舍五入
        for key in scaled_nutrition:
            scaled_nutrition[key] = round(scaled_nutrition[key], 2)
        
        nutrition_snapshot = NutritionData(**scaled_nutrition)
        
        # 暂不处理完整营养信息（full_nutrition）
        # 因为食谱记录主要关注基础营养数据
        full_nutrition_snapshot = None
        
        # 构建备注（添加来自食谱的标记）
        notes_parts = []
        if notes:
            notes_parts.append(notes)
        notes_parts.append(f"[来自食谱: {recipe_name}]")
        combined_notes = " ".join(notes_parts)
        
        # 创建食物记录
        record = FoodRecordInDB(
            user_email=user_email,
            food_name=food_name,
            serving_amount=serving_amount,
            serving_size=serving_size,
            serving_unit=serving_unit,
            nutrition_data=nutrition_snapshot,
            full_nutrition=full_nutrition_snapshot,
            recorded_at=recorded_at,
            meal_type=meal_type,
            notes=combined_notes,
            food_id=food_id,
            recipe_record_batch_id=batch_id,  # 添加批次ID
        )
        
        record_dict = record.dict()
        result = await db.food_records.insert_one(record_dict)
        record_ids.append(str(result.inserted_id))
        all_nutrition.append(scaled_nutrition)
    
    # 计算总营养
    total_nutrition = {
        "calories": 0.0,
        "protein": 0.0,
        "carbohydrates": 0.0,
        "fat": 0.0,
        "fiber": 0.0,
        "sugar": 0.0,
        "sodium": 0.0,
    }
    
    for nutrition in all_nutrition:
        total_nutrition["calories"] += nutrition.get("calories", 0) or 0
        total_nutrition["protein"] += nutrition.get("protein", 0) or 0
        total_nutrition["carbohydrates"] += nutrition.get("carbohydrates", 0) or 0
        total_nutrition["fat"] += nutrition.get("fat", 0) or 0
        total_nutrition["fiber"] += nutrition.get("fiber", 0) or 0
        total_nutrition["sugar"] += nutrition.get("sugar", 0) or 0
        total_nutrition["sodium"] += nutrition.get("sodium", 0) or 0
    
    # 四舍五入
    for key in total_nutrition:
        total_nutrition[key] = round(total_nutrition[key], 2)
    
    return {
        "message": "食谱记录成功",
        "recipe_name": recipe_name,
        "batch_id": batch_id,  # 返回批次ID，用于后续查询/更新/删除
        "total_records": len(record_ids),
        "record_ids": record_ids,
        "total_nutrition": NutritionData(**total_nutrition),
    }


async def get_recipe_records(
    user_email: str,
    start_date: Optional[datetime] = None,
    end_date: Optional[datetime] = None,
    meal_type: Optional[str] = None,
    limit: int = 100,
    offset: int = 0
) -> tuple[List[Dict[str, Any]], int, Dict[str, float]]:
    """
    批量获取食谱记录（按日期和餐次筛选）

    Args:
        user_email: 用户邮箱
        start_date: 开始日期
        end_date: 结束日期
        meal_type: 餐次类型
        limit: 返回数量限制
        offset: 偏移量

    Returns:
        (批次列表, 总数, 所有批次总营养)
    """
    from datetime import datetime as dt, time
    
    db = get_database()
    
    # 构建查询条件
    query = {
        "user_email": user_email,
        "recipe_record_batch_id": {"$exists": True, "$ne": None}  # 只查询来自食谱的记录
    }
    
    # 日期范围筛选
    if start_date or end_date:
        query["recorded_at"] = {}
        if start_date:
            # 转换 date 到 datetime（当天开始）
            start_datetime = dt.combine(start_date, time.min)
            query["recorded_at"]["$gte"] = start_datetime
        if end_date:
            # 转换 date 到 datetime（当天结束）
            end_datetime = dt.combine(end_date, time.max)
            query["recorded_at"]["$lte"] = end_datetime
    
    # 餐次类型筛选
    if meal_type:
        query["meal_type"] = meal_type
    
    # 查询所有匹配的食物记录
    all_records = await db.food_records.find(query).sort("recorded_at", -1).to_list(length=None)
    
    # 按 batch_id 分组
    batch_dict = {}
    for record in all_records:
        batch_id = record.get("recipe_record_batch_id")
        if not batch_id:
            continue
            
        if batch_id not in batch_dict:
            batch_dict[batch_id] = {
                "batch_id": batch_id,
                "records": [],
                "recorded_at": record.get("recorded_at"),
                "meal_type": record.get("meal_type"),
                "notes": record.get("notes", "")
            }
        batch_dict[batch_id]["records"].append(record)
    
    # 转换为批次列表
    batch_items = []
    for batch_id, batch_data in batch_dict.items():
        records = batch_data["records"]
        
        # 从备注中提取食谱名称
        recipe_name = "未命名食谱"
        notes_text = batch_data["notes"]
        if notes_text and "[来自食谱:" in notes_text:
            start = notes_text.find("[来自食谱:") + len("[来自食谱:")
            end = notes_text.find("]", start)
            if end > start:
                recipe_name = notes_text[start:end].strip()
        
        # 提取用户备注（去除食谱标记）
        user_notes = None
        if notes_text:
            if "[来自食谱:" in notes_text:
                user_notes = notes_text[:notes_text.find("[来自食谱:")].strip()
            else:
                user_notes = notes_text
        
        # 计算总营养
        total_nutrition = {
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
            if not nutrition:
                nutrition = {}
            total_nutrition["calories"] += nutrition.get("calories", 0) or 0
            total_nutrition["protein"] += nutrition.get("protein", 0) or 0
            total_nutrition["carbohydrates"] += nutrition.get("carbohydrates", 0) or 0
            total_nutrition["fat"] += nutrition.get("fat", 0) or 0
            total_nutrition["fiber"] += nutrition.get("fiber", 0) or 0
            total_nutrition["sugar"] += nutrition.get("sugar", 0) or 0
            total_nutrition["sodium"] += nutrition.get("sodium", 0) or 0
        
        # 四舍五入
        for key in total_nutrition:
            total_nutrition[key] = round(total_nutrition[key], 2)
        
        batch_items.append({
            "batch_id": batch_id,
            "recipe_name": recipe_name,
            "total_records": len(records),
            "recorded_at": batch_data["recorded_at"],
            "meal_type": batch_data["meal_type"],
            "total_nutrition": NutritionData(**total_nutrition),
            "notes": user_notes
        })
    
    # 按时间倒序排序
    batch_items.sort(key=lambda x: x["recorded_at"], reverse=True)

    # 统计总数
    total = len(batch_items)

    # 应用分页
    paginated_items = batch_items[offset:offset + limit]

    # 计算所有批次的总营养（仅计算分页后的批次）
    overall_nutrition = {
        "calories": 0.0,
        "protein": 0.0,
        "carbohydrates": 0.0,
        "fat": 0.0,
        "fiber": 0.0,
        "sugar": 0.0,
        "sodium": 0.0,
    }

    for batch in paginated_items:
        nutrition_dict = batch["total_nutrition"].dict() if hasattr(batch["total_nutrition"], 'dict') else batch["total_nutrition"]
        overall_nutrition["calories"] += nutrition_dict.get("calories", 0) or 0
        overall_nutrition["protein"] += nutrition_dict.get("protein", 0) or 0
        overall_nutrition["carbohydrates"] += nutrition_dict.get("carbohydrates", 0) or 0
        overall_nutrition["fat"] += nutrition_dict.get("fat", 0) or 0
        overall_nutrition["fiber"] += nutrition_dict.get("fiber", 0) or 0
        overall_nutrition["sugar"] += nutrition_dict.get("sugar", 0) or 0
        overall_nutrition["sodium"] += nutrition_dict.get("sodium", 0) or 0

    # 四舍五入
    for key in overall_nutrition:
        overall_nutrition[key] = round(overall_nutrition[key], 2)

    return paginated_items, total, overall_nutrition


async def update_recipe_record(
    user_email: str,
    batch_id: str,
    recorded_at: Optional[datetime] = None,
    meal_type: Optional[str] = None,
    notes: Optional[str] = None
) -> Dict[str, Any]:
    """
    更新食谱记录（批量更新该批次的所有食物记录）
    
    Args:
        user_email: 用户邮箱
        batch_id: 批次ID
        recorded_at: 新的摄入时间
        meal_type: 新的餐次类型
        notes: 新的备注（不包括食谱标记）
    
    Returns:
        更新结果
    """
    db = get_database()
    
    # 先查询确认记录存在
    records = await db.food_records.find({
        "user_email": user_email,
        "recipe_record_batch_id": batch_id
    }).to_list(length=None)
    
    if not records:
        raise ValueError("食谱记录不存在")
    
    # 从备注中提取食谱名称
    recipe_name = "未命名食谱"
    if records and records[0].get("notes"):
        notes_text = records[0]["notes"]
        if "[来自食谱:" in notes_text:
            start = notes_text.find("[来自食谱:") + len("[来自食谱:")
            end = notes_text.find("]", start)
            if end > start:
                recipe_name = notes_text[start:end].strip()
    
    # 构建更新字典
    update_dict = {}
    if recorded_at is not None:
        update_dict["recorded_at"] = recorded_at
    if meal_type is not None:
        update_dict["meal_type"] = meal_type
    if notes is not None:
        # 保留食谱标记
        update_dict["notes"] = f"{notes} [来自食谱: {recipe_name}]"
    
    # 批量更新其他字段
    if update_dict:
        await db.food_records.update_many(
            {
                "user_email": user_email,
                "recipe_record_batch_id": batch_id
            },
            {"$set": update_dict}
        )
    
    # 重新查询获取更新后的数据
    updated_records = await db.food_records.find({
        "user_email": user_email,
        "recipe_record_batch_id": batch_id
    }).to_list(length=None)
    
    # 计算总营养
    total_nutrition = {
        "calories": 0.0,
        "protein": 0.0,
        "carbohydrates": 0.0,
        "fat": 0.0,
        "fiber": 0.0,
        "sugar": 0.0,
        "sodium": 0.0,
    }
    
    for record in updated_records:
        nutrition = record.get("nutrition_data", {})
        if not nutrition:
            nutrition = {}
        total_nutrition["calories"] += nutrition.get("calories", 0) or 0
        total_nutrition["protein"] += nutrition.get("protein", 0) or 0
        total_nutrition["carbohydrates"] += nutrition.get("carbohydrates", 0) or 0
        total_nutrition["fat"] += nutrition.get("fat", 0) or 0
        total_nutrition["fiber"] += nutrition.get("fiber", 0) or 0
        total_nutrition["sugar"] += nutrition.get("sugar", 0) or 0
        total_nutrition["sodium"] += nutrition.get("sodium", 0) or 0
    
    # 四舍五入
    for key in total_nutrition:
        total_nutrition[key] = round(total_nutrition[key], 2)
    
    return {
        "message": "食谱记录更新成功",
        "recipe_name": recipe_name,
        "batch_id": batch_id,
        "updated_count": len(updated_records),
        "total_nutrition": NutritionData(**total_nutrition),
    }


async def search_recipe_by_name(user_email: str, keyword: str, limit: int = 10) -> Dict[str, Any]:
    """
    通过食谱名称搜索食谱ID（用于快速查找和自动完成）
    
    Args:
        user_email: 用户邮箱
        keyword: 搜索关键词
        limit: 返回数量限制
    
    Returns:
        包含食谱ID列表的字典
    """
    db = get_database()
    
    # 构建查询条件
    query = {
        "$and": [
            # 权限过滤：created_by="all" 或 user_email
            {"$or": [
                {"created_by": "all"},
                {"created_by": user_email}
            ]},
            # 名称匹配
            {"name": {"$regex": keyword, "$options": "i"}}
        ]
    }
    
    # 分两步查询：先查用户自己的，再查公开的
    # 1. 查询用户创建的食谱
    user_recipes = await db.recipes.find({
        "created_by": user_email,
        "name": {"$regex": keyword, "$options": "i"}
    }).limit(limit).to_list(length=limit)
    
    # 2. 查询公开的食谱
    remaining_limit = limit - len(user_recipes)
    public_recipes = []
    if remaining_limit > 0:
        public_recipes = await db.recipes.find({
            "created_by": "all",
            "name": {"$regex": keyword, "$options": "i"}
        }).limit(remaining_limit).to_list(length=remaining_limit)
    
    # 合并结果（用户创建的在前）
    all_recipes = user_recipes + public_recipes
    
    # 转换为简化格式
    recipe_items = []
    for recipe in all_recipes:
        recipe_items.append({
            "id": str(recipe["_id"]),
            "name": recipe.get("name", "未命名食谱"),
            "category": recipe.get("category"),
            "created_by": recipe.get("created_by")
        })
    
    # 统计总数（用于分页）
    total = await db.recipes.count_documents(query)
    
    return {
        "total": total,
        "recipes": recipe_items
    }


async def delete_recipe_record(user_email: str, batch_id: str) -> Dict[str, Any]:
    """
    删除食谱记录（删除该批次的所有食物记录）
    
    Args:
        user_email: 用户邮箱
        batch_id: 批次ID
    
    Returns:
        删除结果
    """
    db = get_database()
    
    # 先查询确认记录存在
    records = await db.food_records.find({
        "user_email": user_email,
        "recipe_record_batch_id": batch_id
    }).to_list(length=None)
    
    if not records:
        raise ValueError("食谱记录不存在")
    
    # 删除该批次的所有食物记录
    result = await db.food_records.delete_many({
        "user_email": user_email,
        "recipe_record_batch_id": batch_id
    })
    
    return {
        "message": "食谱记录删除成功",
        "deleted_count": result.deleted_count,
    }

