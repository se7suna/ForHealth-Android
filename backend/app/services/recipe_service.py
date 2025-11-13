from typing import Optional, List, Dict, Any
from datetime import datetime
from app.database import get_database
from app.models.recipe import RecipeInDB
from app.models.food import NutritionData, FullNutritionData
from app.schemas.recipe import RecipeCreateRequest, RecipeUpdateRequest
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
        image_url=recipe_data.image_url,
        prep_time=recipe_data.prep_time,
        created_by=creator_email,
    )
    
    recipe_dict = recipe.dict()
    result = await db.recipes.insert_one(recipe_dict)
    recipe_dict["_id"] = str(result.inserted_id)
    
    return recipe_dict


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
    if recipe_data.image_url is not None:
        update_data["image_url"] = recipe_data.image_url
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


async def delete_recipe(recipe_id: str, user_email: str) -> bool:
    """
    删除食谱
    
    Args:
        recipe_id: 食谱ID
        user_email: 用户邮箱
    
    Returns:
        是否删除成功
    """
    db = get_database()
    try:
        result = await db.recipes.delete_one({
            "_id": ObjectId(recipe_id),
            "created_by": user_email
        })
        return result.deleted_count > 0
    except Exception:
        return False

