from fastapi import APIRouter, HTTPException, status, Depends, Query
from typing import Optional, List
from app.schemas.recipe import (
    RecipeCreateRequest,
    RecipeUpdateRequest,
    RecipeResponse,
    RecipeSearchRequest,
    RecipeListResponse,
    RecipeIdSearchRequest,
    RecipeIdSearchResponse,
    MessageResponse,
    RecipeRecordQueryRequest,
    RecipeRecordBatchItem,
    RecipeRecordListResponse,
    RecipeRecordCreateRequest,
    RecipeRecordResponse,
    RecipeRecordUpdateRequest,
    RecipeRecordUpdateResponse,
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
    tags: Optional[List[str]] = Query(None, description="标签筛选（可以传递多个）"),
    limit: int = Query(20, ge=1, le=100, description="返回数量限制"),
    offset: int = Query(0, ge=0, description="偏移量"),
    current_user: str = Depends(get_current_user)
):
    """
    搜索食谱
    
    - **keyword**: 搜索关键词（可选，搜索名称、描述）
    - **category**: 分类筛选（可选）
    - **tags**: 标签筛选（可选，可以传递多个 tags 参数）
    - **limit**: 返回数量限制（默认20，最大100）
    - **offset**: 偏移量（用于分页，默认0）
    """
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


@router.get("/search-id", response_model=RecipeIdSearchResponse)
async def search_recipe_by_name(
    keyword: str = Query(..., min_length=1, description="搜索关键词（食谱名称）"),
    limit: int = Query(10, ge=1, le=50, description="返回数量限制"),
    current_user: str = Depends(get_current_user)
):
    """
    通过食谱名称搜索食谱ID（用于快速查找和自动完成）
    
    - **keyword**: 搜索关键词（食谱名称）
    - **limit**: 返回数量限制（默认10，最大50）
    
    返回简化的食谱信息（仅ID、名称、分类），优先显示用户创建的食谱
    """
    result = await recipe_service.search_recipe_by_name(
        user_email=current_user,
        keyword=keyword,
        limit=limit
    )
    
    return RecipeIdSearchResponse(
        total=result["total"],
        recipes=result["recipes"]
    )


@router.get("/categories", response_model=list)
async def get_recipe_categories(current_user: str = Depends(get_current_user)):
    """获取所有食谱分类"""
    categories = await recipe_service.get_recipe_categories(current_user)
    return categories


# ========== 食谱记录管理（必须在 /{recipe_id} 之前，避免路由冲突） ==========

@router.post("/record", response_model=RecipeRecordResponse, status_code=status.HTTP_201_CREATED)
async def create_recipe_record(
    record_data: RecipeRecordCreateRequest,
    current_user: str = Depends(get_current_user)
):
    """
    记录食谱摄入（为食谱中的每个食物创建记录）
    
    - **recipe_id**: 食谱ID（本地库 ObjectId）
    - **scale**: 份量倍数（默认1.0，如：0.5表示半份，2.0表示2份）
    - **recorded_at**: 摄入时间（用户实际食用的时间）
    - **meal_type**: 餐次类型（可选，如：早餐、午餐、晚餐、加餐）
    - **notes**: 备注（可选）
    
    系统会为食谱中的每个食物创建一条记录，并在备注中自动添加"[来自食谱: {食谱名称}]"标记
    """
    try:
        result = await recipe_service.create_recipe_record(
            user_email=current_user,
            recipe_id=record_data.recipe_id,
            scale=record_data.scale,
            recorded_at=record_data.recorded_at,
            meal_type=record_data.meal_type,
            notes=record_data.notes
        )
    except ValueError as e:
        message = str(e)
        if "不存在" in message:
            status_code = status.HTTP_404_NOT_FOUND
        elif "无权" in message:
            status_code = status.HTTP_403_FORBIDDEN
        else:
            status_code = status.HTTP_400_BAD_REQUEST
        raise HTTPException(status_code=status_code, detail=message)
    except Exception as e:
        # 捕获所有其他异常并返回详细错误信息
        import traceback
        error_detail = f"创建食谱记录时发生错误: {str(e)}\n{traceback.format_exc()}"
        print(error_detail)  # 打印到控制台
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"创建食谱记录失败: {str(e)}"
        )
    
    return RecipeRecordResponse(
        message=result["message"],
        recipe_name=result["recipe_name"],
        batch_id=result["batch_id"],
        total_records=result["total_records"],
        record_ids=result["record_ids"],
        total_nutrition=result["total_nutrition"],
    )


@router.get("/record", response_model=RecipeRecordListResponse)
async def get_recipe_records(
    query: RecipeRecordQueryRequest = Depends(),
    current_user: str = Depends(get_current_user)
):
    """
    获取食谱记录列表（批量查询）
    
    - **start_date**: 开始日期（可选，格式：YYYY-MM-DD）
    - **end_date**: 结束日期（可选，格式：YYYY-MM-DD）
    - **meal_type**: 餐次类型筛选（可选，早餐、午餐、晚餐、加餐）
    - **limit**: 返回数量限制（默认100，最大500）
    - **offset**: 偏移量（用于分页，默认0）
    
    返回当前用户的食谱记录批次列表，按记录时间倒序排列。
    每个批次包含该次食谱记录的汇总信息（食谱名称、总营养等）。
    """
    batches, total, overall_nutrition = await recipe_service.get_recipe_records(
        user_email=current_user,
        start_date=query.start_date,
        end_date=query.end_date,
        meal_type=query.meal_type,
        limit=query.limit,
        offset=query.offset,
    )

    batch_responses = []
    for batch in batches:
        batch_responses.append(RecipeRecordBatchItem(
            batch_id=batch["batch_id"],
            recipe_name=batch["recipe_name"],
            total_records=batch["total_records"],
            recorded_at=batch["recorded_at"],
            meal_type=batch["meal_type"],
            total_nutrition=batch["total_nutrition"],
            notes=batch["notes"]
        ))

    from app.models.food import NutritionData
    return RecipeRecordListResponse(
        total=total,
        batches=batch_responses,
        total_nutrition=NutritionData(**overall_nutrition)
    )


@router.put("/record/{batch_id}", response_model=RecipeRecordUpdateResponse)
async def update_recipe_record(
    batch_id: str,
    update_data: RecipeRecordUpdateRequest,
    current_user: str = Depends(get_current_user)
):
    """
    更新食谱记录（批量更新该批次的所有食物记录）
    
    - **batch_id**: 食谱记录批次ID（创建食谱记录时返回）
    - **recorded_at**: 新的摄入时间
    - **meal_type**: 新的餐次类型
    - **notes**: 新的备注（不包括自动添加的食谱标记）
    
    注意：不支持修改份量倍数，如需修改份量请删除后重新创建食谱记录
    """
    try:
        result = await recipe_service.update_recipe_record(
            user_email=current_user,
            batch_id=batch_id,
            recorded_at=update_data.recorded_at,
            meal_type=update_data.meal_type,
            notes=update_data.notes
        )
    except ValueError as e:
        message = str(e)
        if "不存在" in message:
            status_code = status.HTTP_404_NOT_FOUND
        else:
            status_code = status.HTTP_400_BAD_REQUEST
        raise HTTPException(status_code=status_code, detail=message)
    
    return RecipeRecordUpdateResponse(
        message=result["message"],
        recipe_name=result["recipe_name"],
        batch_id=result["batch_id"],
        updated_count=result["updated_count"],
        total_nutrition=result["total_nutrition"],
    )


@router.delete("/record/{batch_id}", response_model=MessageResponse)
async def delete_recipe_record(
    batch_id: str,
    current_user: str = Depends(get_current_user)
):
    """
    删除食谱记录（删除该批次的所有食物记录）
    
    - **batch_id**: 食谱记录批次ID（创建食谱记录时返回）
    
    此操作将删除该批次的所有食物记录，不可恢复
    """
    try:
        result = await recipe_service.delete_recipe_record(
            user_email=current_user,
            batch_id=batch_id
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )
    
    return MessageResponse(
        message=result["message"],
        data={"deleted_count": result["deleted_count"]}
    )


# ========== 食谱管理 ==========

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
    
    # 检查权限：created_by="all" 所有人可见，否则只有创建者可见
    if recipe.get("created_by") != "all" and recipe.get("created_by") != current_user:
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
