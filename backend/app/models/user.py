from datetime import datetime
from typing import Optional
from pydantic import BaseModel, EmailStr, Field
from enum import Enum


class ActivityLevel(str, Enum):
    """活动水平枚举"""
    SEDENTARY = "sedentary"  # 久坐 (PAL=1.2)
    LIGHTLY_ACTIVE = "lightly_active"  # 轻度活动 (PAL=1.375)
    MODERATELY_ACTIVE = "moderately_active"  # 中度活动 (PAL=1.55)
    VERY_ACTIVE = "very_active"  # 重度活动 (PAL=1.725)
    EXTREMELY_ACTIVE = "extremely_active"  # 极重度活动 (PAL=1.9)


class HealthGoalType(str, Enum):
    """健康目标类型"""
    LOSE_WEIGHT = "lose_weight"  # 减重
    GAIN_WEIGHT = "gain_weight"  # 增重
    MAINTAIN_WEIGHT = "maintain_weight"  # 保持体重


class Gender(str, Enum):
    """性别"""
    MALE = "male"
    FEMALE = "female"


# PAL 系数映射
PAL_COEFFICIENTS = {
    ActivityLevel.SEDENTARY: 1.2,
    ActivityLevel.LIGHTLY_ACTIVE: 1.375,
    ActivityLevel.MODERATELY_ACTIVE: 1.55,
    ActivityLevel.VERY_ACTIVE: 1.725,
    ActivityLevel.EXTREMELY_ACTIVE: 1.9,
}


class UserInDB(BaseModel):
    """数据库中的用户模型"""
    email: EmailStr
    username: str
    hashed_password: str

    # 身体基本数据
    height: Optional[float] = None  # 身高（厘米）
    weight: Optional[float] = None  # 体重（公斤）
    age: Optional[int] = None  # 年龄
    gender: Optional[Gender] = None  # 性别

    # 活动水平
    activity_level: Optional[ActivityLevel] = None

    # 健康目标
    health_goal_type: Optional[HealthGoalType] = None
    target_weight: Optional[float] = None  # 目标体重（公斤）
    goal_period_weeks: Optional[int] = None  # 目标周期（周）

    # 计算结果
    bmr: Optional[float] = None  # 基础代谢率
    tdee: Optional[float] = None  # 每日总能量消耗
    daily_calorie_goal: Optional[float] = None  # 每日卡路里目标

    # 元数据
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

    class Config:
        use_enum_values = True
