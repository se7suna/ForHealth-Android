from fastapi import APIRouter, HTTPException, status, Depends, Query
from typing import Optional, List
from app.schemas.recipe import (
    RecipeCreateRequest,
    RecipeUpdateRequest,
    RecipeResponse,
    RecipeListResponse,
    MessageResponse,
)
from app.services import recipe_service
from app.routers.auth import get_current_user

router = APIRouter(prefix="/recipe", tags=["食谱管理"])


@router.post("/", response_model=RecipeResponse, status_code=status.HTTP_201_CREATED)
async def create_recipe(
    recipe_data: RecipeCreateRequest,
    current_user: str = Depends(get_current_user)
):
    """
    创建食谱
    
    - **name**: 食谱名称（必填）
    - **description**: 食谱描述（可选）
    - **category**: 分类（可选）
    - **foods**: 食物列表（必填，至少1个食物）
    - **tags**: 标签（可选）
    - **image_url**: 图片URL（可选）
    - **prep_time**: 准备时间（可选）
    
    用户创建的食谱仅创建者自己可见，食谱中的食物必须来自食物库，系统会自动计算总营养
    """
    try:
        recipe = await recipe_service.create_recipe(recipe_data, current_user)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(e)
        )
    
    return RecipeResponse(
        id=recipe["_id"],
        name=recipe["name"],
        description=recipe.get("description"),
        category=recipe.get("category"),
        foods=recipe["foods"],
        total_nutrition=recipe["total_nutrition"],
        total_full_nutrition=recipe.get("total_full_nutrition"),
        tags=recipe.get("tags"),
        image_url=recipe.get("image_url"),
        prep_time=recipe.get("prep_time"),
        created_by=recipe.get("created_by"),
        created_at=recipe["created_at"],
        updated_at=recipe["updated_at"],
    )


@router.get("/search", response_model=RecipeListResponse)
async def search_recipes(
    keyword: Optional[str] = Query(None, description="搜索关键词"),
    category: Optional[str] = Query(None, description="分类筛选"),
    tags: Optional[List[str]] = Query(None, description="标签筛选"),
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    current_user: str = Depends(get_current_user)
):
    """搜索食谱"""
    recipes, total = await recipe_service.search_recipes(
        keyword=keyword,
        category=category,
        tags=tags,
        user_email=current_user,
        limit=limit,
        offset=offset,
    )
    
    recipe_responses = [
        RecipeResponse(
            id=recipe["_id"],
            name=recipe["name"],
            description=recipe.get("description"),
            category=recipe.get("category"),
            foods=recipe["foods"],
            total_nutrition=recipe["total_nutrition"],
            tags=recipe.get("tags"),
            image_url=recipe.get("image_url"),
            prep_time=recipe.get("prep_time"),
            created_by=recipe.get("created_by"),
            created_at=recipe["created_at"],
            updated_at=recipe["updated_at"],
        )
        for recipe in recipes
    ]
    
    return RecipeListResponse(total=total, recipes=recipe_responses)


@router.get("/categories", response_model=list)
async def get_recipe_categories(current_user: str = Depends(get_current_user)):
    """获取所有食谱分类"""
    categories = await recipe_service.get_recipe_categories(current_user)
    return categories


@router.get("/{recipe_id}", response_model=RecipeResponse)
async def get_recipe(
    recipe_id: str,
    current_user: str = Depends(get_current_user)
):
    """获取食谱详情"""
    recipe = await recipe_service.get_recipe_by_id(recipe_id)
    
    if not recipe:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="食谱不存在"
        )
    
    # 检查权限：只能查看系统食谱或自己创建的食谱
    if recipe.get("created_by") is not None and recipe.get("created_by") != current_user:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="无权访问此食谱"
        )
    
    return RecipeResponse(
        id=recipe["_id"],
        name=recipe["name"],
        description=recipe.get("description"),
        category=recipe.get("category"),
        foods=recipe["foods"],
        total_nutrition=recipe["total_nutrition"],
        total_full_nutrition=recipe.get("total_full_nutrition"),
        tags=recipe.get("tags"),
        image_url=recipe.get("image_url"),
        prep_time=recipe.get("prep_time"),
        created_by=recipe.get("created_by"),
        created_at=recipe["created_at"],
        updated_at=recipe["updated_at"],
    )


@router.put("/{recipe_id}", response_model=RecipeResponse)
async def update_recipe(
    recipe_id: str,
    recipe_data: RecipeUpdateRequest,
    current_user: str = Depends(get_current_user)
):
    """更新食谱（仅创建者可更新）"""
    try:
        recipe = await recipe_service.update_recipe(recipe_id, recipe_data, current_user)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(e)
        )
    
    if not recipe:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="食谱不存在或无权更新"
        )
    
    return RecipeResponse(
        id=recipe["_id"],
        name=recipe["name"],
        description=recipe.get("description"),
        category=recipe.get("category"),
        foods=recipe["foods"],
        total_nutrition=recipe["total_nutrition"],
        total_full_nutrition=recipe.get("total_full_nutrition"),
        tags=recipe.get("tags"),
        image_url=recipe.get("image_url"),
        prep_time=recipe.get("prep_time"),
        created_by=recipe.get("created_by"),
        created_at=recipe["created_at"],
        updated_at=recipe["updated_at"],
    )


@router.delete("/{recipe_id}", response_model=MessageResponse)
async def delete_recipe(
    recipe_id: str,
    current_user: str = Depends(get_current_user)
):
    """删除食谱（仅创建者可删除）"""
    success = await recipe_service.delete_recipe(recipe_id, current_user)
    
    if not success:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="食谱不存在或无权删除"
        )
    
    return MessageResponse(message="食谱删除成功")

