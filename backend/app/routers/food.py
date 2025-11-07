from fastapi import APIRouter, HTTPException, status, Depends, Query
from typing import Optional
from datetime import date
from app.schemas.food import (
    FoodCreateRequest,
    FoodUpdateRequest,
    FoodResponse,
    FoodListResponse,
    FoodRecordCreateRequest,
    FoodRecordUpdateRequest,
    FoodRecordResponse,
    FoodRecordListResponse,
    DailyNutritionSummary,
    MessageResponse,
    BarcodeScanResponse,
)
from app.services import food_service
from app.services import external_api_service
from app.routers.auth import get_current_user

router = APIRouter(prefix="/food", tags=["食物管理"])


# ========== 食物管理 ==========
@router.post("/", response_model=FoodResponse, status_code=status.HTTP_201_CREATED)
async def create_food(
    food_data: FoodCreateRequest,
    current_user: str = Depends(get_current_user)
):
    """
    创建食物信息
    
    - **name**: 食物名称（必填）
    - **category**: 食物分类（可选，如：水果、蔬菜、肉类等）
    - **serving_size**: 标准份量（必填，单位：克）
    - **serving_unit**: 份量单位（默认：克）
    - **nutrition_per_serving**: 每份基础营养数据（必填）
      - calories: 卡路里（千卡）
      - protein: 蛋白质（克）
      - carbohydrates: 碳水化合物（克）
      - fat: 脂肪（克）
      - fiber: 膳食纤维（克，可选）
      - sugar: 糖分（克，可选）
      - sodium: 钠（毫克，可选）
    - **full_nutrition**: 完整营养信息（可选，与测试脚本格式一致）
      - calory: 热量信息数组（包含总热量、总热量Kj、蛋白质热量、脂肪热量、碳水化合物热量等）
      - base_ingredients: 三大营养素数组（包含碳水化合物、蛋白质、脂肪及其子项）
      - vitamin: 维生素数组（15种维生素）
      - mineral: 矿物质数组（14种矿物质）
      - amino_acid: 氨基酸数组（20种氨基酸）
      - other_ingredients: 其它成分数组（嘌呤、酒精度等）
    - **brand**: 品牌（可选）
    - **barcode**: 条形码（可选）
    - **image_url**: 食物图片URL（可选）
    
    用户创建的食物仅创建者自己可见，其他用户无法搜索或查看
    """
    try:
        food = await food_service.create_food(food_data, current_user)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(e)
        )
    
    return FoodResponse(
        id=food["_id"],
        name=food["name"],
        category=food.get("category"),
        serving_size=food["serving_size"],
        serving_unit=food["serving_unit"],
        nutrition_per_serving=food["nutrition_per_serving"],
        full_nutrition=food.get("full_nutrition"),
        brand=food.get("brand"),
        barcode=food.get("barcode"),
        image_url=food.get("image_url"),
        created_by=food.get("created_by"),
        created_at=food["created_at"],
        source=food.get("source"),
        boohee_id=food.get("boohee_id"),
        boohee_code=food.get("boohee_code"),
    )


@router.get("/search", response_model=FoodListResponse)
async def search_foods(
    keyword: Optional[str] = Query(None, description="搜索关键词，对应薄荷API的 q 参数"),
    page: int = Query(1, ge=1, le=10, description="页码（薄荷API最多返回前10页）"),
    include_full_nutrition: bool = Query(
        True,
        description="是否为每个搜索结果请求完整营养信息（调用 /api/v2/foods/ingredients）",
    ),
    current_user: str = Depends(get_current_user)
):
    """
    直接调用薄荷健康官方数据库搜索食物，不再依赖本地食物库。

    - **keyword**: 搜索关键词（建议填写，若为空返回空结果）
    - **page**: 页码（薄荷API默认每页30条，最多10页）
    - **include_full_nutrition**: 是否为每个结果同步拉取完整营养信息
    """
    _ = current_user  # 保留依赖，用于权限控制

    result = await food_service.search_foods(
        keyword=keyword,
        page=page,
        include_full_nutrition=include_full_nutrition,
        user_email=current_user,
    )

    return FoodListResponse(**result)


@router.get("/categories", response_model=list)
async def get_food_categories(current_user: str = Depends(get_current_user)):
    """
    获取所有食物分类
    
    返回系统中已有的所有食物分类列表
    """
    categories = await food_service.get_food_categories(current_user)
    return categories


@router.get("/{food_id}", response_model=FoodResponse)
async def get_food(
    food_id: str,
    current_user: str = Depends(get_current_user)
):
    """
    根据ID获取食物详情（仅本地库）
    
    - **food_id**: 食物ID（本地库 ObjectId）
    """
    food = await food_service.get_food_by_id(food_id)

    if not food:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="食物不存在"
        )

    if food.get("created_by") is not None and food.get("created_by") != current_user:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="无权访问此食物"
        )

    return FoodResponse(
        id=food["_id"],
        name=food["name"],
        category=food.get("category"),
        serving_size=food["serving_size"],
        serving_unit=food["serving_unit"],
        nutrition_per_serving=food["nutrition_per_serving"],
        full_nutrition=food.get("full_nutrition"),
        brand=food.get("brand"),
        barcode=food.get("barcode"),
        image_url=food.get("image_url"),
        created_by=food.get("created_by"),
        created_at=food["created_at"],
        source=food.get("source"),
        boohee_id=food.get("boohee_id"),
        boohee_code=food.get("boohee_code"),
    )


@router.put("/{food_id}", response_model=FoodResponse)
async def update_food(
    food_id: str,
    food_data: FoodUpdateRequest,
    current_user: str = Depends(get_current_user)
):
    """
    更新食物信息
    
    - **food_id**: 食物ID
    
    可更新的字段（所有字段可选）：
    - **name**: 食物名称
    - **category**: 食物分类
    - **serving_size**: 标准份量
    - **serving_unit**: 份量单位
    - **nutrition_per_serving**: 每份基础营养数据
    - **full_nutrition**: 完整营养信息（与测试脚本格式一致）
      - calory: 热量信息数组
      - base_ingredients: 三大营养素数组
      - vitamin: 维生素数组
      - mineral: 矿物质数组
      - amino_acid: 氨基酸数组
      - other_ingredients: 其它成分数组
    - **brand**: 品牌
    - **barcode**: 条形码
    - **image_url**: 食物图片URL
    
    只能更新自己创建的食物
    """
    try:
        food = await food_service.update_food(food_id, food_data, current_user)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(e)
        )
    
    if not food:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="食物不存在或无权更新"
        )
    
    return FoodResponse(
        id=food["_id"],
        name=food["name"],
        category=food.get("category"),
        serving_size=food["serving_size"],
        serving_unit=food["serving_unit"],
        nutrition_per_serving=food["nutrition_per_serving"],
        full_nutrition=food.get("full_nutrition"),
        brand=food.get("brand"),
        barcode=food.get("barcode"),
        image_url=food.get("image_url"),
        created_by=food.get("created_by"),
        created_at=food["created_at"],
        source=food.get("source"),
        boohee_id=food.get("boohee_id"),
        boohee_code=food.get("boohee_code"),
    )


@router.delete("/{food_id}", response_model=MessageResponse)
async def delete_food(
    food_id: str,
    current_user: str = Depends(get_current_user)
):
    """
    删除食物
    
    - **food_id**: 食物ID
    
    只能删除自己创建的食物
    """
    success = await food_service.delete_food(food_id, current_user)
    
    if not success:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="食物不存在或无权删除"
        )
    
    return MessageResponse(message="食物删除成功")


# ========== 食物记录管理 ==========
@router.post("/record", response_model=FoodRecordResponse, status_code=status.HTTP_201_CREATED)
async def create_food_record(
    record_data: FoodRecordCreateRequest,
    current_user: str = Depends(get_current_user)
):
    """
    记录食物摄入（通过食物ID选择）
    
    - **food_id**: 食物ID（本地库 ObjectId）
    - **source**: 数据来源（local / auto），默认 auto
    - **serving_amount**: 食用份量数（如：1.5 表示 1.5 份）
    - **recorded_at**: 摄入时间（用户实际食用的时间）
    - **meal_type**: 餐次类型（可选，如：早餐、午餐、晚餐、加餐）
    - **notes**: 备注（可选）
    
    系统会根据所选食物的标准份量与营养信息自动计算实际摄入的营养数据
    """
    try:
        record = await food_service.create_food_record(current_user, record_data)
    except ValueError as e:
        message = str(e)
        if "不存在" in message:
            status_code = status.HTTP_404_NOT_FOUND
        elif "无权" in message:
            status_code = status.HTTP_403_FORBIDDEN
        else:
            status_code = status.HTTP_400_BAD_REQUEST
        raise HTTPException(status_code=status_code, detail=message)

    return FoodRecordResponse(
        id=record["_id"],
        user_email=record["user_email"],
        food_name=record["food_name"],
        serving_amount=record["serving_amount"],
        serving_size=record["serving_size"],
        serving_unit=record["serving_unit"],
        nutrition_data=record["nutrition_data"],
        full_nutrition=record.get("full_nutrition"),
        recorded_at=record["recorded_at"],
        meal_type=record.get("meal_type"),
        notes=record.get("notes"),
        food_id=record.get("food_id"),
        created_at=record["created_at"],
    )


@router.get("/record/list", response_model=FoodRecordListResponse)
async def get_food_records(
    start_date: Optional[date] = Query(None, description="开始日期（YYYY-MM-DD）"),
    end_date: Optional[date] = Query(None, description="结束日期（YYYY-MM-DD）"),
    meal_type: Optional[str] = Query(None, description="餐次类型"),
    limit: int = Query(100, ge=1, le=500, description="返回数量限制"),
    offset: int = Query(0, ge=0, description="偏移量"),
    current_user: str = Depends(get_current_user)
):
    """
    获取食物记录列表
    
    - **start_date**: 开始日期（可选，格式：YYYY-MM-DD）
    - **end_date**: 结束日期（可选，格式：YYYY-MM-DD）
    - **meal_type**: 餐次类型筛选（可选）
    - **limit**: 返回数量限制（默认100，最大500）
    - **offset**: 偏移量（用于分页，默认0）
    
    返回当前用户的食物记录，按记录时间倒序排列
    """
    records, total = await food_service.get_food_records(
        user_email=current_user,
        start_date=start_date,
        end_date=end_date,
        meal_type=meal_type,
        limit=limit,
        offset=offset,
    )
    
    record_responses = [
        FoodRecordResponse(
            id=record["_id"],
            user_email=record["user_email"],
            food_name=record["food_name"],
            serving_amount=record["serving_amount"],
            serving_size=record["serving_size"],
            serving_unit=record["serving_unit"],
            nutrition_data=record["nutrition_data"],
            full_nutrition=record.get("full_nutrition"),
            recorded_at=record["recorded_at"],
            meal_type=record.get("meal_type"),
            notes=record.get("notes"),
            food_id=record.get("food_id"),
            created_at=record["created_at"],
        )
        for record in records
    ]
    
    # 计算总营养
    total_nutrition = await food_service.calculate_total_nutrition(records)
    
    return FoodRecordListResponse(
        total=total,
        records=record_responses,
        total_nutrition=total_nutrition,
    )


@router.get("/record/daily/{target_date}", response_model=DailyNutritionSummary)
async def get_daily_nutrition(
    target_date: date,
    current_user: str = Depends(get_current_user)
):
    """
    获取某日的营养摘要
    
    - **target_date**: 目标日期（格式：YYYY-MM-DD）
    
    返回指定日期的所有食物记录和营养总计
    """
    summary = await food_service.get_daily_nutrition_summary(current_user, target_date)
    
    record_responses = [
        FoodRecordResponse(
            id=record["_id"],
            user_email=record["user_email"],
            food_name=record["food_name"],
            serving_amount=record["serving_amount"],
            serving_size=record["serving_size"],
            serving_unit=record["serving_unit"],
            nutrition_data=record["nutrition_data"],
            recorded_at=record["recorded_at"],
            meal_type=record.get("meal_type"),
            notes=record.get("notes"),
            food_id=record.get("food_id"),
            created_at=record["created_at"],
        )
        for record in summary["records"]
    ]
    
    return DailyNutritionSummary(
        date=summary["date"],
        total_calories=summary["total_calories"],
        total_protein=summary["total_protein"],
        total_carbohydrates=summary["total_carbohydrates"],
        total_fat=summary["total_fat"],
        meal_count=summary["meal_count"],
        records=record_responses,
    )


@router.put("/record/{record_id}", response_model=FoodRecordResponse)
async def update_food_record(
    record_id: str,
    record_data: FoodRecordUpdateRequest,
    current_user: str = Depends(get_current_user)
):
    """
    更新食物记录
    
    - **record_id**: 记录ID
    
    可更新的字段（所有字段可选）：
    - **food_name**: 食物名称
    - **serving_amount**: 食用份量数
    - **serving_size**: 每份大小（克）
    - **serving_unit**: 份量单位
    - **nutrition_data**: 营养数据
    - **recorded_at**: 摄入时间（用户实际吃这个食物的时间）
    - **meal_type**: 餐次类型（早餐、午餐、晚餐、加餐）
    - **notes**: 备注
    
    只能更新自己的记录
    """
    record = await food_service.update_food_record(record_id, record_data, current_user)
    
    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="记录不存在或无权更新"
        )
    
    return FoodRecordResponse(
        id=record["_id"],
        user_email=record["user_email"],
        food_name=record["food_name"],
        serving_amount=record["serving_amount"],
        serving_size=record["serving_size"],
        serving_unit=record["serving_unit"],
        nutrition_data=record["nutrition_data"],
        full_nutrition=record.get("full_nutrition"),
        recorded_at=record["recorded_at"],
        meal_type=record.get("meal_type"),
        notes=record.get("notes"),
        food_id=record.get("food_id"),
        created_at=record["created_at"],
    )


@router.delete("/record/{record_id}", response_model=MessageResponse)
async def delete_food_record(
    record_id: str,
    current_user: str = Depends(get_current_user)
):
    """
    删除食物记录
    
    - **record_id**: 记录ID
    
    只能删除自己的记录
    """
    success = await food_service.delete_food_record(record_id, current_user)
    
    if not success:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="记录不存在或无权删除"
        )
    
    return MessageResponse(message="记录删除成功")


# ========== 条形码扫描 ==========
@router.get("/barcode/{barcode}", response_model=BarcodeScanResponse)
async def scan_barcode(
    barcode: str,
    current_user: str = Depends(get_current_user)
):
    """
    扫描条形码查询食品信息
    
    工作流程：
    1. 验证条形码格式
    2. 查询本地数据库（优先使用已有数据）
    3. 如果本地没有，调用外部API查询
    4. 返回食品信息供用户确认份量
    
    返回的食品信息包含：
    - 食品名称、品牌、分类
    - 标准份量和单位
    - 每份基础营养数据 (nutrition_per_serving)
    - 完整营养信息 (full_nutrition，如果存在)
      - calory: 热量信息数组（包含总热量、总热量Kj、蛋白质热量、脂肪热量、碳水化合物热量等）
      - base_ingredients: 三大营养素数组（包含碳水化合物、蛋白质、脂肪及其子项）
      - vitamin: 维生素数组（15种维生素）
      - mineral: 矿物质数组（14种矿物质）
      - amino_acid: 氨基酸数组（20种氨基酸）
      - other_ingredients: 其它成分数组（嘌呤、酒精度等）
    - 条形码
    
    前端应该：
    1. 获取到食品信息后，让用户确认或调整份量
    2. 根据份量计算实际营养数据
    3. 调用 POST /api/food/record 保存记录
    
    如果扫描失败或未找到商品，应提供手动录入的选项
    """
    # 1. 验证条形码格式
    if not await external_api_service.validate_barcode(barcode):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="无效的条形码格式"
        )
    
    # 2. 先查询本地数据库
    local_food = await food_service.get_food_by_barcode(barcode)
    if local_food:
        return BarcodeScanResponse(
            found=True,
            message="从本地数据库找到食品信息",
            food_data={
                "id": local_food["_id"],
                "name": local_food["name"],
                "brand": local_food.get("brand"),
                "category": local_food.get("category"),
                "serving_size": local_food["serving_size"],
                "serving_unit": local_food["serving_unit"],
                "nutrition_per_serving": local_food["nutrition_per_serving"],
                "full_nutrition": local_food.get("full_nutrition"),
                "barcode": local_food.get("barcode"),
                "image_url": local_food.get("image_url"),
                "source": "local"
            }
        )
    
    # 3. 调用外部API查询
    try:
        external_data = await external_api_service.query_food_by_barcode(barcode)
        
        if external_data:
            return BarcodeScanResponse(
                found=True,
                message="从外部API找到食品信息",
                food_data={
                    **external_data,
                    "source": "external"
                }
            )
        else:
            return BarcodeScanResponse(
                found=False,
                message="未找到该商品信息，请手动录入",
                food_data=None
            )
    
    except Exception as e:
        # 外部API调用失败
        return BarcodeScanResponse(
            found=False,
            message=f"查询失败：{str(e)}，请手动录入",
            food_data=None
        )

