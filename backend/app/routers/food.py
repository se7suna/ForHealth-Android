from fastapi import APIRouter, HTTPException, status, Depends, Query, UploadFile, File, Form
from typing import Optional, Union
from datetime import date
import os
import tempfile
import json
from app.schemas.food import (
    FoodCreateRequest,
    FoodUpdateRequest,
    FoodResponse,
    FoodSearchRequest,
    FoodListResponse,
    SimplifiedFoodListResponse,
    SimplifiedFoodSearchItem,
    SimplifiedNutritionData,
    FoodNameSearchRequest,
    FoodIdSearchResponse,
    FoodIdItem,
    FoodRecordCreateRequest,
    FoodRecordUpdateRequest,
    FoodRecordResponse,
    FoodRecordQueryRequest,
    FoodRecordListResponse,
    DailyNutritionSummary,
    MessageResponse,
    BarcodeScanResponse,
    BarcodeImageRecognitionResponse,
)
from app.services import food_service
from app.services import external_api_service
from app.routers.auth import get_current_user
from app.utils.barcode_scanner import decode_barcode_from_image
from app.utils.image_storage import save_food_image, get_image_url, delete_food_image

router = APIRouter(prefix="/food", tags=["食物管理"])


# ========== 食物管理 ==========
@router.post("/", response_model=FoodResponse, status_code=status.HTTP_201_CREATED)
async def create_food(
    # 基本信息
    name: str = Form(..., description="食物名称"),
    category: Optional[str] = Form(None, description="食物分类"),
    serving_size: float = Form(..., gt=0, description="标准份量（克）"),
    serving_unit: str = Form("克", description="份量单位"),
    brand: Optional[str] = Form(None, description="品牌"),
    barcode: Optional[str] = Form(None, description="条形码"),
    # 基础营养数据（必填）
    calories: float = Form(..., ge=0, description="卡路里（千卡）"),
    protein: float = Form(..., ge=0, description="蛋白质（克）"),
    carbohydrates: float = Form(..., ge=0, description="碳水化合物（克）"),
    fat: float = Form(..., ge=0, description="脂肪（克）"),
    # 基础营养数据（可选）
    fiber: Optional[float] = Form(None, ge=0, description="膳食纤维（克）"),
    sugar: Optional[float] = Form(None, ge=0, description="糖分（克）"),
    sodium: Optional[float] = Form(None, ge=0, description="钠（毫克）"),
    # 图片文件
    image: UploadFile = File(None, description="食物图片文件（可选，支持 jpg/jpeg/png/webp/gif，最大10MB）"),
    current_user: str = Depends(get_current_user)
):
    """
    创建食物信息（支持图片文件上传）
    
    注意：此接口使用 multipart/form-data 格式
    
    **基本信息**（必填）：
    - **name**: 食物名称
    - **serving_size**: 标准份量（单位：克）
    
    **基本信息**（可选）：
    - **category**: 食物分类（如：水果、蔬菜、肉类等）
    - **serving_unit**: 份量单位（默认：克）
    - **brand**: 品牌
    - **barcode**: 条形码
    
    **基础营养数据**（必填）：
    - **calories**: 卡路里（千卡）
    - **protein**: 蛋白质（克）
    - **carbohydrates**: 碳水化合物（克）
    - **fat**: 脂肪（克）
    
    **基础营养数据**（可选）：
    - **fiber**: 膳食纤维（克）
    - **sugar**: 糖分（克）
    - **sodium**: 钠（毫克）
    
    **图片**（可选）：
    - **image**: 食物图片文件（支持 jpg/jpeg/png/webp/gif，最大10MB）
    
    用户创建的食物仅创建者自己可见，其他用户无法搜索或查看
    """
    try:
        # 调用 service 层处理创建食物（包括图片上传）
        food = await food_service.create_food_with_image(
            name=name,
            category=category,
            serving_size=serving_size,
            serving_unit=serving_unit,
            calories=calories,
            protein=protein,
            carbohydrates=carbohydrates,
            fat=fat,
            fiber=fiber,
            sugar=sugar,
            sodium=sodium,
            brand=brand,
            barcode=barcode,
            image=image,
            creator_email=current_user
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(e)
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"创建食物失败：{str(e)}"
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


@router.get("/search", response_model=Union[FoodListResponse, SimplifiedFoodListResponse])
async def search_foods(
    query: FoodSearchRequest = Depends(),
    current_user: str = Depends(get_current_user)
):
    """
    直接调用薄荷健康官方数据库搜索食物，不再依赖本地食物库。

    - **keyword**: 搜索关键词（建议填写，若为空返回空结果）
    - **page**: 页码（薄荷API默认每页30条，最多10页）
    - **include_full_nutrition**: 是否为每个结果同步拉取完整营养信息
    - **simplified**: 是否返回简化版本（仅主要营养信息：能量、蛋白质、脂肪、碳水化合物、糖、钠）
    
    当 simplified=True 时，返回简化版本，只包含主要营养信息，减少数据传输量。
    """
    _ = current_user  # 保留依赖，用于权限控制

    result = await food_service.search_foods(
        keyword=query.keyword,
        page=query.page,
        include_full_nutrition=query.include_full_nutrition if not query.simplified else False,
        user_email=current_user,
    )

    # 如果请求简化版本，转换数据格式
    if query.simplified:
        simplified_foods = []
        for food in result.get("foods", []):
            # 从 nutrition_per_serving 提取主要营养信息
            nutrition_data = food.get("nutrition_per_serving", {})
            simplified_nutrition = SimplifiedNutritionData(
                calories=nutrition_data.get("calories", 0),
                protein=nutrition_data.get("protein", 0),
                fat=nutrition_data.get("fat", 0),
                carbohydrates=nutrition_data.get("carbohydrates", 0),
                sugar=nutrition_data.get("sugar"),
                sodium=nutrition_data.get("sodium"),
            )
            
            simplified_item = SimplifiedFoodSearchItem(
                source=food.get("source", "local"),
                food_id=food.get("food_id"),
                boohee_id=food.get("boohee_id"),
                code=food.get("code", ""),
                name=food.get("name", ""),
                weight=food.get("weight", 100),
                weight_unit=food.get("weight_unit", "克"),
                brand=food.get("brand"),
                image_url=food.get("image_url"),
                nutrition=simplified_nutrition,
            )
            simplified_foods.append(simplified_item)
        
        return SimplifiedFoodListResponse(
            page=result.get("page", 1),
            total_pages=result.get("total_pages", 0),
            foods=simplified_foods,
    )

    return FoodListResponse(**result)


@router.get("/search-id", response_model=FoodIdSearchResponse)
async def search_food_by_name(
    query: FoodNameSearchRequest = Depends(),
    current_user: str = Depends(get_current_user)
):
    """
    通过食物名称搜索本地数据库，仅返回食物ID和名称
    
    - **keyword**: 搜索关键词（必填）
    - **limit**: 返回数量限制（默认20，最大100）
    
    **仅搜索本地数据库**：
    - 包括用户自己创建的食物（created_by=用户邮箱）
    - 包括所有人可见的食物（created_by="all"，包含薄荷缓存数据）
    - 不包括其他用户的私有食物
    
    **排序规则**：
    - 用户自己创建的食物排在最前面（按创建时间倒序）
    - 公共食物排在后面（按创建时间倒序）
    
    返回本地数据库的 ObjectId，适用于创建食谱、饮食记录等场景。
    """
    # 只搜索本地数据库
    local_foods = await food_service.search_local_foods_only(
        keyword=query.keyword,
        user_email=current_user,
        limit=query.limit,
    )
    
    # 转换为仅包含ID和名称的格式
    food_id_items = []
    for food in local_foods:
        food_id_item = FoodIdItem(
            food_id=food.get("food_id", ""),  # 本地数据库的 ObjectId
            name=food.get("name", ""),
            source="local",  # 全部来自本地库
            brand=food.get("brand"),
            boohee_id=food.get("boohee_id"),  # 如果是缓存的薄荷食物，会有这个字段
        )
        food_id_items.append(food_id_item)
    
    return FoodIdSearchResponse(
        total=len(food_id_items),
        foods=food_id_items,
    )


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

    # 权限检查：created_by="all" 所有人可见，否则只有创建者可见
    if food.get("created_by") != "all" and food.get("created_by") != current_user:
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
    - **brand**: 品牌
    - **barcode**: 条形码
    
    只能更新自己创建的食物
    """
    try:
        updated_food = await food_service.update_food(food_id, food_data, current_user)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(e)
        )
    
    if not updated_food:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="食物不存在或无权更新"
        )
    
    return FoodResponse(
        id=updated_food["_id"],
        name=updated_food["name"],
        category=updated_food.get("category"),
        serving_size=updated_food["serving_size"],
        serving_unit=updated_food["serving_unit"],
        nutrition_per_serving=updated_food["nutrition_per_serving"],
        full_nutrition=updated_food.get("full_nutrition"),
        brand=updated_food.get("brand"),
        barcode=updated_food.get("barcode"),
        created_by=updated_food.get("created_by"),
        created_at=updated_food["created_at"],
        source=updated_food.get("source"),
        boohee_id=updated_food.get("boohee_id"),
        boohee_code=updated_food.get("boohee_code"),
    )


@router.put("/{food_id}/image", response_model=FoodResponse)
async def update_food_image(
    food_id: str,
    image: UploadFile = File(..., description="食物图片文件（支持 jpg/jpeg/png/webp/gif，最大10MB）"),
    current_user: str = Depends(get_current_user)
):
    """
    更新食物图片
    
    - **food_id**: 食物ID
    - **image**: 食物图片文件（必填，支持 jpg/jpeg/png/webp/gif，最大10MB）
    
    只能更新自己创建的食物的图片
    """
    try:
        # 检查食物是否存在且有权限
        food = await food_service.get_food_by_id(food_id)
        if not food:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="食物不存在"
            )
        
        if food.get("created_by") != "all" and food.get("created_by") != current_user:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="无权更新此食物的图片"
            )
        
        # 获取旧图片URL（用于删除）
        old_image_url = food.get("image_url")
        
        # 保存新图片
        relative_path = await save_food_image(image, food_id)
        new_image_url = get_image_url(relative_path)
        
        # 更新图片URL
        success = await food_service.update_food_image_url(food_id, new_image_url, current_user)
        if not success:
            # 如果更新失败，删除已保存的图片
            delete_food_image(new_image_url)
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="更新图片URL失败"
            )
        
        # 删除旧图片
        if old_image_url:
            delete_food_image(old_image_url)
        
        # 重新获取更新后的食物信息
        updated_food = await food_service.get_food_by_id(food_id)
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"更新图片失败：{str(e)}"
        )
    
    return FoodResponse(
        id=updated_food["_id"],
        name=updated_food["name"],
        category=updated_food.get("category"),
        serving_size=updated_food["serving_size"],
        serving_unit=updated_food["serving_unit"],
        nutrition_per_serving=updated_food["nutrition_per_serving"],
        full_nutrition=updated_food.get("full_nutrition"),
        brand=updated_food.get("brand"),
        barcode=updated_food.get("barcode"),
        image_url=updated_food.get("image_url"),
        created_by=updated_food.get("created_by"),
        created_at=updated_food["created_at"],
        source=updated_food.get("source"),
        boohee_id=updated_food.get("boohee_id"),
        boohee_code=updated_food.get("boohee_code"),
    )


@router.delete("/{food_id}", response_model=MessageResponse)
async def delete_food(
    food_id: str,
    current_user: str = Depends(get_current_user)
):
    """
    删除食物
    
    - **food_id**: 食物ID
    
    只能删除自己创建的食物（同时会删除关联的图片文件）
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
    query: FoodRecordQueryRequest = Depends(),
    current_user: str = Depends(get_current_user)
):
    """
    获取食物记录列表
    
    - **start_date**: 开始日期（可选，格式：YYYY-MM-DD）
    - **end_date**: 结束日期（可选，格式：YYYY-MM-DD）
    - **meal_type**: 餐次类型筛选（可选，早餐、午餐、晚餐、加餐）
    - **limit**: 返回数量限制（默认100，最大500）
    - **offset**: 偏移量（用于分页，默认0）
    
    返回当前用户的食物记录，按记录时间倒序排列
    """
    records, total = await food_service.get_food_records(
        user_email=current_user,
        start_date=query.start_date,
        end_date=query.end_date,
        meal_type=query.meal_type,
        limit=query.limit,
        offset=query.offset,
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
@router.post("/barcode/recognize", response_model=BarcodeImageRecognitionResponse)
async def recognize_barcode_from_image(
    file: UploadFile = File(...),
    current_user: str = Depends(get_current_user)
):
    """
    从上传的图片中识别条形码数字
    
    工作流程：
    1. 接收上传的图片文件
    2. 使用图像识别技术识别条形码
    3. 返回识别到的条形码数字
    
    返回内容：
    - success: 是否成功识别
    - barcode: 识别到的条形码数字（如：6901939613702）
    - barcode_type: 条形码类型（如：EAN13, CODE128）
    - message: 响应消息
    
    后续步骤：
    - 前端获取到条形码后，可以调用 GET /api/food/barcode/{barcode} 查询食品信息
    
    支持的图片格式：JPG, PNG, BMP 等常见格式
    支持的条形码类型：EAN-13, EAN-8, UPC-A, CODE128, QR Code 等
    """
    # 验证文件类型
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="请上传图片文件（支持 JPG, PNG, BMP 等格式）"
        )
    
    # 创建临时文件保存上传的图片
    temp_file = None
    try:
        # 创建临时文件
        suffix = os.path.splitext(file.filename)[1] if file.filename else ".jpg"
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
            # 读取上传的文件内容并保存
            content = await file.read()
            temp_file.write(content)
            temp_file_path = temp_file.name
        
        # 识别条形码
        result = decode_barcode_from_image(temp_file_path)
        
        if result:
            # 识别成功
            return BarcodeImageRecognitionResponse(
                success=True,
                barcode=result['barcode'],
                barcode_type=result['type'],
                message="成功识别到条形码"
            )
        else:
            # 识别失败
            return BarcodeImageRecognitionResponse(
                success=False,
                barcode=None,
                barcode_type=None,
                message="未识别到条形码，请确保图片清晰且包含完整的条形码"
            )
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"处理图片时发生错误: {str(e)}"
        )
    
    finally:
        # 清理临时文件
        if temp_file and os.path.exists(temp_file_path):
            try:
                os.unlink(temp_file_path)
            except Exception as e:
                # 临时文件清理失败不影响主流程
                pass


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

