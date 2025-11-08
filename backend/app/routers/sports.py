from fastapi import APIRouter, HTTPException, status, Depends
from app.routers.auth import get_current_user
from app.services.user_service import get_user_by_email
from app.schemas.sports import (
    LogSportsRequest,
    CreateSportsRequest,
    UpdateSportsRequest,
    SearchSportsRequest,
    HistorySportsRequest,
    SearchSportsResponse,
    HistorySportsResponse,
    SimpleSportsResponse,
)

router = APIRouter(prefix="/sports", tags=["运动记录"])

from app.database import get_database
from app.models.sports import DefaultSports,default_email

# 初始化运动表，填入默认运动类型和卡路里消耗
async def initialize_sports_table():
    db = get_database()
    for sport in DefaultSports:
        existing = await db["sports"].find_one({"sport_type": sport["sport_type"]})
        if not existing:
            await db["sports"].insert_one(sport)
# 获取用户体重
async def get_user_weight(email: str) -> float:
    user=get_user_by_email(email)

    if user and "weight" in user:
        return user["weight"]
    else:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="用户体重数据未找到"
        )
# 计算卡路里消耗
async def calculate_calories_burned(mets: float, weight: float, duration_minutes: int) -> float:
    # 卡路里消耗公式：卡路里 = METs × 体重(kg) × 时间(小时)
    duration_hours = duration_minutes / 60
    calories_burned = mets * weight * duration_hours
    return calories_burned
# 筛选出有权限的运动类型
async def filter_sports_by_user(user_email: str) -> list:
    db = get_database()
    sports = await db["sports"].find(
        {"$or": [{"email": user_email}, {"email": default_email}]}
        ,{"sport_type":1,"METs":1,"_id":0}
    ).to_list(length=100)
    return sports
# 记录运动
@router.post("/log-sports",response_model=SimpleSportsResponse)
async def log_sports(
    log_request: LogSportsRequest, current_user: str = Depends(get_current_user)
):
    """
    记录运动及消耗卡路里
    - **sport_type**: 运动类型
    - **crearted_at**: 运动时间（默认当前时间）
    - **duration_time**: 运动持续时间（分钟，必须大于0）
    """
    db = get_database()
    # 查找运动
    sport = await db["sports"].find_one(
        {"sport_type": log_request.sport_type}
    )
    if (not sport) or (sport["email"] != current_user or sport["email"]!=default_email):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="运动类型未找到")
    # 计算消耗卡路里
    user_weight = await get_user_weight(current_user)
    mets = sport["METs"]
    calories_burned = await calculate_calories_burned(
        mets, user_weight, log_request.duration_time
    )
    SportLog={
        "email":current_user,
        "sport_type":log_request.sport_type,
        "crearted_at":log_request.crearted_at,
        "duration_time":log_request.duration_time,
        "calories_burned":calories_burned
    }
    result=await db["sports_log"].insert_one(SportLog)
    if result.inserted_id:
        return SimpleSportsResponse(success=True, message="运动记录已保存")
    else:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="保存运动记录失败"
        )

# 新建运动类型：自定义并写入表
@router.post("/log-sports",response_model=SimpleSportsResponse)
async def create_sports(
    create_request: CreateSportsRequest, current_user: str = Depends(get_current_user)
):
    """
    创建自定义运动类型
    - **sport_type**: 运动类型
    - **METs**: 运动强度（必须大于0）
    """
    db = get_database()
    # 检查是否已存在相同运动类型
    existing_sport = await db["sports"].find({
        "$or": [{"email": current_user}, {"email": default_email}],
        "sport_type": create_request.sport_type}
    )
    if existing_sport:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="该运动类型已存在"
        )
    SportType={
        "email":current_user,
        "sport_type":create_request.sport_type,
        "METs":create_request.METs
    }
    result=await db["sports"].insert_one(SportType)
    if result.inserted_id:
        return SimpleSportsResponse(success=True, message="自定义运动类型已创建")
    else:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="创建自定义运动类型失败"
        )

# 更新自定义运动类型
@router.post("/update-sports",response_model=SimpleSportsResponse)
async def update_sports(
    update_request: UpdateSportsRequest, current_user: str = Depends(get_current_user)
):
    """
    更新自定义运动类型
    - **sport_type**: 运动类型
    - **METs**: 运动强度（必须大于0）
    """
    db = get_database()
    # 查找运动
    sport = await db["sports"].find_one(
        {"sport_type": update_request.sport_type, "email": current_user}
    )
    if not sport:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="自定义运动类型未找到"
        )
    update_data = {}
    if update_request.METs is not None:
        update_data["METs"] = update_request.METs
    result = await db["sports"].find_one_and_update(
        {"sport_type": update_request.sport_type, "email": current_user},
        {"$set": update_data}
    )# 希望没有语法问题
    if result.modified_count == 1:
        return SimpleSportsResponse(success=True, message="自定义运动类型已更新")
    else:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="更新自定义运动类型失败"
        )

# 搜索运动类型
@router.post("/search-sports",response_model=list[SearchSportsResponse])
async def search_sports(
    search_request: SearchSportsRequest, current_user: str = Depends(get_current_user)
):
    """
    搜索运动类型
    - **sport_type**: 运动类型（可选，模糊搜索）
    """
    db = get_database()
    query = {
        "$or": [{"email": current_user}, {"email": default_email}]
    }
    if search_request.sport_type:
        query["sport_type"] = {"$regex": search_request.sport_type, "$options": "i"}
    sports = await db["sports"].find(query,{"sport_type":1,"METs":1,"_id":0,"email":0}).to_list(length=100)
    return sports