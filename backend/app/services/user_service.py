from typing import Optional
from datetime import datetime
from app.database import get_database
from app.models.user import UserInDB, ActivityLevel, HealthGoalType
from app.schemas.user import (
    BodyDataRequest,
    ActivityLevelRequest,
    HealthGoalRequest,
    UserProfileUpdate,
)
from app.services.calculation_service import (
    calculate_bmr,
    calculate_tdee,
    calculate_daily_calorie_goal,
)


async def get_user_by_email(email: str) -> Optional[dict]:
    """根据邮箱获取用户"""
    db = get_database()
    user = await db.users.find_one({"email": email})
    return user


async def create_user(email: str, username: str, hashed_password: str) -> dict:
    """创建用户"""
    db = get_database()
    user_data = UserInDB(
        email=email, username=username, hashed_password=hashed_password
    ).dict()

    result = await db.users.insert_one(user_data)
    user_data["_id"] = str(result.inserted_id)
    return user_data


async def update_body_data(email: str, body_data: BodyDataRequest) -> Optional[dict]:
    """更新用户身体基本数据并计算 BMR"""
    db = get_database()

    # 计算 BMR
    bmr = calculate_bmr(
        body_data.weight, body_data.height, body_data.age, body_data.gender
    )

    # 更新数据
    update_data = {
        "height": body_data.height,
        "weight": body_data.weight,
        "age": body_data.age,
        "gender": body_data.gender.value,
        "bmr": bmr,
        "updated_at": datetime.utcnow(),
    }

    result = await db.users.find_one_and_update(
        {"email": email},
        {"$set": update_data},
        return_document=True,
    )

    return result


async def update_activity_level(
    email: str, activity_data: ActivityLevelRequest
) -> Optional[dict]:
    """更新活动水平并计算 TDEE"""
    db = get_database()

    # 获取用户当前的 BMR
    user = await get_user_by_email(email)
    if not user or user.get("bmr") is None:
        return None

    # 计算 TDEE
    tdee = calculate_tdee(user["bmr"], activity_data.activity_level)

    # 更新数据
    update_data = {
        "activity_level": activity_data.activity_level.value,
        "tdee": tdee,
        "updated_at": datetime.utcnow(),
    }

    result = await db.users.find_one_and_update(
        {"email": email},
        {"$set": update_data},
        return_document=True,
    )

    return result


async def update_health_goal(
    email: str, goal_data: HealthGoalRequest
) -> Optional[dict]:
    """更新健康目标并计算每日卡路里目标"""
    db = get_database()

    # 获取用户当前的 TDEE
    user = await get_user_by_email(email)
    if not user or user.get("tdee") is None:
        return None

    # 计算每日卡路里目标
    daily_calorie_goal = calculate_daily_calorie_goal(
        user["tdee"], goal_data.health_goal_type
    )

    # 更新数据
    update_data = {
        "health_goal_type": goal_data.health_goal_type.value,
        "target_weight": goal_data.target_weight,
        "goal_period_weeks": goal_data.goal_period_weeks,
        "daily_calorie_goal": daily_calorie_goal,
        "updated_at": datetime.utcnow(),
    }

    result = await db.users.find_one_and_update(
        {"email": email},
        {"$set": update_data},
        return_document=True,
    )

    return result


async def get_user_profile(email: str) -> Optional[dict]:
    """获取用户完整资料"""
    return await get_user_by_email(email)


async def update_user_profile(
    email: str, profile_data: UserProfileUpdate
) -> Optional[dict]:
    """更新用户资料"""
    db = get_database()

    # 只更新提供的字段
    update_data = {
        k: v for k, v in profile_data.dict(exclude_unset=True).items() if v is not None
    }

    if not update_data:
        return await get_user_profile(email)

    # 获取当前用户数据
    user = await get_user_by_email(email)
    if not user:
        return None

    # 如果更新了身体数据，重新计算 BMR
    if any(k in update_data for k in ["height", "weight", "age", "gender"]):
        height = update_data.get("height", user.get("height"))
        weight = update_data.get("weight", user.get("weight"))
        age = update_data.get("age", user.get("age"))
        gender = update_data.get("gender", user.get("gender"))

        if all([height, weight, age, gender]):
            bmr = calculate_bmr(weight, height, age, gender)
            update_data["bmr"] = bmr

            # 如果有活动水平，重新计算 TDEE
            activity_level = user.get("activity_level")
            if activity_level:
                tdee = calculate_tdee(bmr, ActivityLevel(activity_level))
                update_data["tdee"] = tdee

                # 如果有健康目标，重新计算每日卡路里目标
                health_goal_type = user.get("health_goal_type")
                if health_goal_type:
                    daily_calorie_goal = calculate_daily_calorie_goal(
                        tdee, HealthGoalType(health_goal_type)
                    )
                    update_data["daily_calorie_goal"] = daily_calorie_goal

    # 如果更新了活动水平，重新计算 TDEE
    if "activity_level" in update_data:
        bmr = user.get("bmr")
        if bmr:
            tdee = calculate_tdee(bmr, update_data["activity_level"])
            update_data["tdee"] = tdee

            # 如果有健康目标，重新计算每日卡路里目标
            health_goal_type = update_data.get(
                "health_goal_type", user.get("health_goal_type")
            )
            if health_goal_type:
                daily_calorie_goal = calculate_daily_calorie_goal(
                    tdee, HealthGoalType(health_goal_type)
                )
                update_data["daily_calorie_goal"] = daily_calorie_goal

    # 如果更新了健康目标，重新计算每日卡路里目标
    if "health_goal_type" in update_data:
        tdee = user.get("tdee")
        if tdee:
            daily_calorie_goal = calculate_daily_calorie_goal(
                tdee, update_data["health_goal_type"]
            )
            update_data["daily_calorie_goal"] = daily_calorie_goal

    update_data["updated_at"] = datetime.utcnow()

    result = await db.users.find_one_and_update(
        {"email": email},
        {"$set": update_data},
        return_document=True,
    )

    return result


async def update_password(email: str, new_hashed_password: str) -> bool:
    """更新用户密码"""
    db = get_database()

    result = await db.users.update_one(
        {"email": email},
        {"$set": {"hashed_password": new_hashed_password, "updated_at": datetime.utcnow()}},
    )

    return result.modified_count > 0
