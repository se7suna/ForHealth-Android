from fastapi import HTTPException,status
from datetime import datetime
from app.database import get_database
from app.models.sports import DefaultSports,default_email
from app.services.user_service import get_user_profile
from app.models.sports import SportsLogInDB,SportsTypeInDB

# 初始化运动表，填入默认运动类型和卡路里消耗
async def initialize_sports_table():
    db = get_database()
    for sport in DefaultSports:
        existing = await db["sports"].find_one({"sport_type": sport["sport_type"]})
        if not existing:
            await db["sports"].insert_one(sport)
# 获取用户体重
async def get_user_weight(email: str) -> float:
    user= await get_user_profile(email)

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


async def log_sports(log_request,current_user):
    db = get_database()
    # 查找运动
    sport = await db["sports"].find_one(
        {"sport_type": log_request.sport_type}
    )
    if (not sport) or (sport["email"] != current_user and sport["email"]!=default_email):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="运动类型未找到")
    # 计算消耗卡路里
    user_weight = await get_user_weight(current_user)
    mets = sport["METs"]
    calories_burned = await calculate_calories_burned(
        mets, user_weight, log_request.duration_time
    )
    SportLog=SportsLogInDB(
        email=current_user,
        sport_type=log_request.sport_type,
        crearted_at=log_request.crearted_at,
        duration_time=log_request.duration_time,
        calories_burned=calories_burned
    )
    return await db["sports_log"].insert_one(SportLog.model_dump())# 转为字典插入

async def create_sports(create_request,current_user):
    db = get_database()
    # 检查是否已存在相同运动类型
    existing_sport = await db["sports"].find_one({
        "$or": [{"email": current_user}, {"email": default_email}],
        "sport_type": create_request.sport_type}
    )
    if existing_sport:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="该运动类型已存在"
        )
    SportType=SportsTypeInDB(
        email=current_user,
        sport_type=create_request.sport_type,
        METs=create_request.METs
    )

    return await db["sports"].insert_one(SportType.model_dump())# 转为字典插入

async def update_sports(update_request,current_user):
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
    return await db["sports"].find_one_and_update(
        {"sport_type": update_request.sport_type, "email": current_user},
        {"$set": update_data})# 希望没有语法问题

async def search_sports(search_request,current_user):
    db = get_database()
    query = {
        "$or": [{"email": current_user}, {"email": default_email}]
    }
    if search_request.sport_type:
        query["sport_type"] = {"$regex": search_request.sport_type, "$options": "i"}
    return await db["sports"].find(query,{"sport_type":1,"METs":1,"_id":0}).to_list(length=100)
    

async def history_sports(history_request,current_user):
    db = get_database()
    query = {
        "email": current_user
    }
    if history_request.start_date:
        start_datetime = datetime.combine(history_request.start_date, datetime.min.time())
        query["crearted_at"] = {"$gte": start_datetime}
    if history_request.end_date:
        end_datetime = datetime.combine(history_request.end_date, datetime.max.time())
        if "crearted_at" in query:
            query["crearted_at"]["$lte"] = end_datetime
        else:
            query["crearted_at"] = {"$lte": end_datetime}
    return await db["sports_log"].find(query,
    {"sport_type":1,"crearted_at":1,"duration_time":1,"calories_burned":1,"_id":0}).to_list(length=100)