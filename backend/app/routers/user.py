from fastapi import APIRouter, HTTPException, status, Depends, Query
from typing import Optional
from datetime import date
from app.schemas.user import (
    BodyDataRequest,
    ActivityLevelRequest,
    HealthGoalRequest,
    UserProfileResponse,
    UserProfileUpdate,
    MessageResponse,
)
from app.schemas.weight import (
    WeightRecordCreateRequest,
    WeightRecordUpdateRequest,
    WeightRecordResponse,
    WeightRecordListResponse,
)
from app.services import user_service, weight_service
from app.routers.auth import get_current_user

router = APIRouter(prefix="/user", tags=["用户管理"])


@router.post("/body-data", response_model=MessageResponse)
async def update_body_data(
    body_data: BodyDataRequest, current_user: str = Depends(get_current_user)
):
    """
    更新用户身体基本数据

    - **height**: 身高（厘米，50-250）
    - **weight**: 体重（公斤，20-300）
    - **birthdate**: 出生日期（格式：YYYY-MM-DD）
    - **gender**: 性别（male/female）

    系统会自动根据出生日期计算年龄，并计算保存基础代谢率（BMR）
    """
    user = await user_service.update_body_data(current_user, body_data)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="用户不存在"
        )

    return MessageResponse(
        message="身体数据更新成功",
        data={
            "bmr": user.get("bmr"),
        },
    )


@router.post("/activity-level", response_model=MessageResponse)
async def update_activity_level(
    activity_data: ActivityLevelRequest, current_user: str = Depends(get_current_user)
):
    """
    更新活动水平

    - **activity_level**: 活动水平
      - sedentary: 久坐（PAL=1.2）
      - lightly_active: 轻度活动（PAL=1.375）
      - moderately_active: 中度活动（PAL=1.55）
      - very_active: 重度活动（PAL=1.725）
      - extremely_active: 极重度活动（PAL=1.9）

    系统会根据 BMR 和活动水平计算每日总能量消耗（TDEE）
    """
    user = await user_service.update_activity_level(current_user, activity_data)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="请先完成身体基本数据收集",
        )

    return MessageResponse(
        message="活动水平更新成功",
        data={
            "activity_level": user.get("activity_level"),
            "tdee": user.get("tdee"),
        },
    )


@router.post("/health-goal", response_model=MessageResponse)
async def update_health_goal(
    goal_data: HealthGoalRequest, current_user: str = Depends(get_current_user)
):
    """
    设定健康目标

    - **health_goal_type**: 健康目标类型
      - lose_weight: 减重（每日卡路里 = TDEE - 500）
      - gain_weight: 增重（每日卡路里 = TDEE + 500）
      - maintain_weight: 保持体重（每日卡路里 = TDEE）
    - **target_weight**: 目标体重（公斤，减重/增重时必填）
    - **goal_period_weeks**: 目标周期（周，1-104，减重/增重时必填）

    系统会根据健康目标计算每日卡路里摄入目标
    """
    user = await user_service.update_health_goal(current_user, goal_data)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="请先完成活动水平选择",
        )

    return MessageResponse(
        message="健康目标设定成功",
        data={
            "health_goal_type": user.get("health_goal_type"),
            "daily_calorie_goal": user.get("daily_calorie_goal"),
        },
    )


@router.get("/profile", response_model=UserProfileResponse)
async def get_profile(current_user: str = Depends(get_current_user)):
    """
    获取用户完整资料

    返回用户的所有个人信息，包括：
    - 基本信息（邮箱、用户名）
    - 身体数据（身高、体重、年龄、性别）
    - 活动水平
    - 健康目标
    - 计算结果（BMR、TDEE、每日卡路里目标）

    注意：年龄是根据出生日期动态计算的
    """
    user = await user_service.get_user_profile(current_user)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="用户不存在"
        )

    # 移除敏感信息
    user.pop("hashed_password", None)
    user.pop("_id", None)

    # 将birthdate从datetime转换为字符串格式（YYYY-MM-DD）
    if "birthdate" in user and user["birthdate"]:
        from datetime import datetime as dt
        if isinstance(user["birthdate"], dt):
            user["birthdate"] = user["birthdate"].strftime("%Y-%m-%d")

    return UserProfileResponse(**user)


@router.put("/profile", response_model=UserProfileResponse)
async def update_profile(
    profile_data: UserProfileUpdate, current_user: str = Depends(get_current_user)
):
    """
    更新用户资料

    可以更新以下字段（所有字段可选）：
    - **username**: 用户名
    - **height**: 身高
    - **weight**: 体重
    - **birthdate**: 出生日期（格式：YYYY-MM-DD）
    - **gender**: 性别
    - **activity_level**: 活动水平
    - **health_goal_type**: 健康目标类型
    - **target_weight**: 目标体重
    - **goal_period_weeks**: 目标周期

    如果修改了出生日期，系统会自动重新计算年龄
    如果修改了相关字段，系统会自动重新计算 BMR、TDEE 和每日卡路里目标
    """
    user = await user_service.update_user_profile(current_user, profile_data)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="用户不存在"
        )

    # 移除敏感信息
    user.pop("hashed_password", None)
    user.pop("_id", None)

    # 将birthdate从datetime转换为字符串格式（YYYY-MM-DD）
    if "birthdate" in user and user["birthdate"]:
        from datetime import datetime as dt
        if isinstance(user["birthdate"], dt):
            user["birthdate"] = user["birthdate"].strftime("%Y-%m-%d")

    return UserProfileResponse(**user)


# ========== 体重记录管理 ==========
@router.post("/weight-record", response_model=WeightRecordResponse, status_code=status.HTTP_201_CREATED)
async def create_weight_record(
    record_data: WeightRecordCreateRequest,
    current_user: str = Depends(get_current_user)
):
    """
    记录体重

    - **weight**: 体重（公斤，0-500kg）
    - **recorded_at**: 记录时间
    - **notes**: 备注（可选，最多200字符）

    用于记录用户的历史体重数据，支持体重趋势分析
    """
    record = await weight_service.create_weight_record(current_user, record_data)

    return WeightRecordResponse(
        id=record["_id"],
        weight=record["weight"],
        recorded_at=record["recorded_at"],
        notes=record.get("notes"),
        created_at=record["created_at"]
    )


@router.get("/weight-records", response_model=WeightRecordListResponse)
async def get_weight_records(
    start_date: Optional[date] = Query(None, description="开始日期（格式：YYYY-MM-DD）"),
    end_date: Optional[date] = Query(None, description="结束日期（格式：YYYY-MM-DD）"),
    limit: int = Query(100, ge=1, le=500, description="返回数量限制"),
    current_user: str = Depends(get_current_user)
):
    """
    获取体重记录列表

    - **start_date**: 开始日期（可选，格式：YYYY-MM-DD）
    - **end_date**: 结束日期（可选，格式：YYYY-MM-DD）
    - **limit**: 返回数量限制（默认100，最大500）

    返回当前用户的体重记录，按记录时间倒序排列
    """
    records, total = await weight_service.get_weight_records(
        user_email=current_user,
        start_date=start_date,
        end_date=end_date,
        limit=limit
    )

    record_responses = [
        WeightRecordResponse(
            id=record["_id"],
            weight=record["weight"],
            recorded_at=record["recorded_at"],
            notes=record.get("notes"),
            created_at=record["created_at"]
        )
        for record in records
    ]

    return WeightRecordListResponse(total=total, records=record_responses)


@router.put("/weight-record/{record_id}", response_model=WeightRecordResponse)
async def update_weight_record(
    record_id: str,
    record_data: WeightRecordUpdateRequest,
    current_user: str = Depends(get_current_user)
):
    """
    更新体重记录

    - **record_id**: 记录ID

    可更新的字段（所有字段可选）：
    - **weight**: 体重（公斤，0-500kg）
    - **recorded_at**: 记录时间
    - **notes**: 备注

    只能更新自己的记录
    """
    record = await weight_service.update_weight_record(record_id, current_user, record_data)

    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="记录不存在或无权更新"
        )

    return WeightRecordResponse(
        id=record["_id"],
        weight=record["weight"],
        recorded_at=record["recorded_at"],
        notes=record.get("notes"),
        created_at=record["created_at"]
    )


@router.delete("/weight-record/{record_id}", response_model=MessageResponse)
async def delete_weight_record(
    record_id: str,
    current_user: str = Depends(get_current_user)
):
    """
    删除体重记录

    - **record_id**: 记录ID

    只能删除自己的记录
    """
    success = await weight_service.delete_weight_record(record_id, current_user)

    if not success:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="记录不存在或无权删除"
        )

    return MessageResponse(message="体重记录删除成功")
