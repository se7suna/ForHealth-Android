from fastapi import HTTPException,status
from datetime import datetime
from app.database import get_database
from app.config import settings

from app.services.user_service import get_user_profile
from app.models.sports import SportsLogInDB,SportsTypeInDB
from bson import ObjectId

###工具计算函数
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

###服务调用函数


# 创建自定义运动类型
async def create_sports(create_request,current_user):
    db = get_database()
    # 检查是否已存在相同运动类型
    existing_sport = await db["sports"].find_one({
        "$or": [{"email": current_user}, {"email": settings.DEFAULT_SPORT_EMAIL}],
        "sport_type": create_request.sport_type}
    )
    if existing_sport:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="该运动类型已存在"
        )
    # 检查是否缺失值
    if not create_request.sport_type or not create_request.describe or not create_request.METs:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="运动类型、描述和卡路里消耗不能为空"
        )
    SportType=SportsTypeInDB(
        email=current_user,
        describe=create_request.describe,
        sport_type=create_request.sport_type,
        METs=create_request.METs
    )

    return await db["sports"].insert_one(SportType.model_dump())# 转为字典插入

# 更新自定义运动类型
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
    # 只添加非None的值到更新数据中
    if update_request.new_sport_type is not None:
        update_data["sport_type"] = update_request.new_sport_type
    if update_request.METs is not None:
        update_data["METs"] = update_request.METs
    if update_request.describe is not None:
        update_data["describe"] = update_request.describe
    
    # 如果没有要更新的字段（字典为空），返回原始记录
    if not update_data:
        return sport
    
    # 执行更新
    result = await db["sports"].find_one_and_update(
        {"sport_type": update_request.sport_type, "email": current_user},
        {"$set": update_data},
        return_document=True  # 返回更新后的文档
    )
    
    return result

# 删除自定义运动类型
async def delete_sports(sport_type: str, current_user: str):
    """
    删除自定义运动类型
    """
    db = get_database()
    # 查找并删除用户自定义的运动类型
    result = await db["sports"].delete_one({
        "sport_type": sport_type,
        "email": current_user
    })

    # 返回删除是否成功（删除了至少一条记录）
    return result.deleted_count > 0

# 获取用户可用运动类型列表
async def get_available_sports_types(current_user: str):
    """
    获取用户可用的运动类型列表，包括默认运动类型和用户自定义的运动类型
    """
    db = get_database()
    # 查询默认运动类型和用户自定义的运动类型
    sports_types = await db["sports"].find(
        {"$or": [{"email": settings.DEFAULT_SPORT_EMAIL}, {"email": current_user}]},
        {"sport_type": 1, "describe": 1, "METs": 1, "_id": 0}
    ).to_list(length=1000)

    return sports_types

# 记录运动
async def log_sports_record(log_request,current_user):
    db = get_database()
    # 查找运动
    sport = await db["sports"].find_one({
        "sport_type": log_request.sport_type,
        "$or": [
            {"email": settings.DEFAULT_SPORT_EMAIL},
            {"email": current_user}
        ]
    })
    if (not sport) :
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="运动类型未找到")
    # 检查是否缺失值
    if not log_request.created_at or not log_request.duration_time:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="创建时间和运动时长不能为空"
        )
    # 计算消耗卡路里
    user_weight = await get_user_weight(current_user)
    mets = sport["METs"]
    calories_burned = await calculate_calories_burned(
        mets, user_weight, log_request.duration_time
    )
    SportLog=SportsLogInDB(
        email=current_user,
        sport_type=log_request.sport_type,
        created_at=log_request.created_at,
        duration_time=log_request.duration_time,
        calories_burned=calories_burned
    )
    # print(log_request.created_at,"111")
    record_dict=SportLog.model_dump()
    result = await db["sports_log"].insert_one(record_dict)# 转为字典插入_id
    record_dict["record_id"] = result.inserted_id
    return record_dict

# 更新运动记录
async def update_sports_record(update_request,current_user):
    db = get_database()
    # 查找运动记录
    record = await db["sports_log"].find_one(
        {"_id": ObjectId(update_request.record_id), "email": current_user}
    )
    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="运动记录未找到"
        )
    update_data = {}
    # 只添加非None的值到更新数据中
    if update_request.sport_type is not None:
        update_data["sport_type"] = update_request.sport_type
    if update_request.created_at is not None:
        update_data["created_at"] = update_request.created_at
    if update_request.duration_time is not None:
        update_data["duration_time"] = update_request.duration_time
        # 重新计算消耗卡路里
        user_weight = await get_user_weight(current_user)
        # 查找运动
        sport = await db["sports"].find_one(
            {"sport_type": update_request.sport_type}
        )
        mets = sport["METs"]
        calories_burned = await calculate_calories_burned(
            mets, user_weight, update_request.duration_time
        )
        update_data["calories_burned"] = calories_burned
    # 如果没有要更新的字段（字典为空），返回原始记录
    if not update_data:
        return record
    # 执行更新
    result = await db["sports_log"].find_one_and_update(
        {"_id": ObjectId(update_request.record_id), "email": current_user},
        {"$set": update_data},
        return_document=True  # 返回更新后的文档
    )
    return result

# 删除运动记录
async def delete_sports_record(record_id: str, current_user: str):
    """
    删除运动记录
    """
    db = get_database()
    # 查找并删除用户的运动记录
    result = await db["sports_log"].delete_one({
        "_id": ObjectId(record_id),
        "email": current_user
    })
    
    # 返回删除是否成功（删除了至少一条记录）
    return result.deleted_count > 0
    
# 根据开始和结束时间检索运动记录
async def search_sports_record(search_request,current_user):
    db = get_database()
    query = {
        "email": current_user
    }
    if search_request.start_date:
        start_datetime = datetime.combine(search_request.start_date, datetime.min.time())
        query["created_at"] = {"$gte": start_datetime}
    if search_request.end_date:
        end_datetime = datetime.combine(search_request.end_date, datetime.max.time())
        if "created_at" in query:
            query["created_at"]["$lte"] = end_datetime
        else:
            query["created_at"] = {"$lte": end_datetime}
    if search_request.sport_type:
        query["sport_type"] = search_request.sport_type

    # 查询记录
    records = await db["sports_log"].find(query,
    {"sport_type":1,"created_at":1,"duration_time":1,"calories_burned":1,"_id":1}).sort("created_at", -1).to_list(length=100)
    # 创建时间最新的排在前面
    # records.sort(key=lambda x: x["created_at"], reverse=False)

    # 将 ObjectId 转换为字符串,以便 Pydantic 可以正确序列化
    for record in records:
        if "_id" in record:
            record["record_id"] = str(record["_id"])
            del record["_id"]  # 删除原始的 _id 字段

    return records
    # _id作为记录唯一标识符，对于定位记录很重要！

# 生成运动报告
async def generate_sports_report(email: str):
    """
    生成用户运动报告，包括：
    - 总运动次数
    - 总运动时长
    - 总消耗卡路里
    - 最常进行的运动类型
    - 按运动类型统计的详情
    """
    from datetime import datetime, timedelta
    # 计算上一周的日期范围
    today = datetime.now().date()
    # 找到上周一
    last_day = today - timedelta(days=1)
    # 上周日
    last_7day = last_day - timedelta(days=6)
    
    # 获取用户上一周的运动记录
    search_request = type('obj', (object,), {})()
    search_request.start_date = last_7day
    search_request.end_date = last_day
    search_request.sport_type = None
    
    sports_logs = await search_sports_record(search_request, email)
    
    if not sports_logs:
        return {
            "total_activities": 0,
            "total_duration": 0,
            "total_calories": 0,
            "favorite_sport": None,
            "sport_details": {}
        }
    
    # 计算各项统计数据
    total_activities = len(sports_logs)
    total_duration = sum(log["duration_time"] for log in sports_logs)
    total_calories = sum(log["calories_burned"] for log in sports_logs)
    
    # 统计各运动类型的详情
    sport_details = {}
    for log in sports_logs:
        sport_type = log["sport_type"]
        if sport_type not in sport_details:
            sport_details[sport_type] = {
                "count": 0,
                "total_duration": 0,
                "total_calories": 0,
                "avg_duration": 0,
                "avg_calories": 0
            }
        
        sport_details[sport_type]["count"] += 1
        sport_details[sport_type]["total_duration"] += log["duration_time"]
        sport_details[sport_type]["total_calories"] += log["calories_burned"]
    
    # 计算平均值
    for sport_type, details in sport_details.items():
        details["avg_duration"] = details["total_duration"] / details["count"]
        details["avg_calories"] = details["total_calories"] / details["count"]
    
    # 找出最常进行的运动类型
    favorite_sport = max(sport_details.items(), key=lambda x: x[1]["count"])[0] if sport_details else None
    
    return {
        "total_activities": total_activities,
        "total_duration": total_duration,
        "total_calories": round(total_calories, 2),
        "favorite_sport": favorite_sport,
        "sport_details": sport_details
    }

