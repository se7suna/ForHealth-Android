from pydantic import BaseModel, EmailStr, Field, validator
from typing import Optional, Any, Dict
from datetime import date
from app.models.user import ActivityLevel, HealthGoalType, Gender


# ========== 注册和认证 ==========
class SendRegistrationCodeRequest(BaseModel):
    """发送注册验证码请求"""
    email: EmailStr


class UserRegisterRequest(BaseModel):
    """用户注册请求"""
    email: EmailStr
    username: str = Field(..., min_length=2, max_length=50)
    password: str = Field(..., min_length=6, max_length=100)
    verification_code: str = Field(..., min_length=6, max_length=6, description="邮箱验证码")


class UserLoginRequest(BaseModel):
    """用户登录请求"""
    email: EmailStr
    password: str

    class Config:
        json_schema_extra = {
            "example": {
                "email": "user@example.com",
                "password": "123456"
            }
        }


class TokenResponse(BaseModel):
    """Token 响应"""
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RefreshTokenRequest(BaseModel):
    """刷新 Token 请求"""
    refresh_token: str


# ========== 身体基本数据 ==========
class BodyDataRequest(BaseModel):
    """用户身体基本数据请求"""
    height: float = Field(..., ge=50, le=250, description="身高（厘米）")
    weight: float = Field(..., ge=20, le=300, description="体重（公斤）")
    birthdate: date = Field(..., description="出生日期")
    gender: Gender

    @validator("height")
    def validate_height(cls, v):
        if v < 50 or v > 250:
            raise ValueError("身高必须在 50-250cm 之间")
        return v

    @validator("weight")
    def validate_weight(cls, v):
        if v < 20 or v > 300:
            raise ValueError("体重必须在 20-300kg 之间")
        return v

    @validator("birthdate")
    def validate_birthdate(cls, v):
        from datetime import date
        today = date.today()
        age = today.year - v.year - ((today.month, today.day) < (v.month, v.day))
        if age < 10 or age > 120:
            raise ValueError("年龄必须在 10-120 岁之间")
        return v


# ========== 活动水平 ==========
class ActivityLevelRequest(BaseModel):
    """活动水平选择请求"""
    activity_level: ActivityLevel


# ========== 健康目标 ==========
class HealthGoalRequest(BaseModel):
    """健康目标设定请求"""
    health_goal_type: HealthGoalType
    target_weight: Optional[float] = Field(None, ge=20, le=300, description="目标体重（公斤）")
    goal_period_weeks: Optional[int] = Field(None, ge=1, le=104, description="目标周期（周）")

    @validator("target_weight")
    def validate_target_weight(cls, v, values):
        goal_type = values.get("health_goal_type")
        if goal_type != HealthGoalType.MAINTAIN_WEIGHT and v is None:
            raise ValueError("减重或增重目标必须设置目标体重")
        return v

    @validator("goal_period_weeks")
    def validate_goal_period(cls, v, values):
        goal_type = values.get("health_goal_type")
        if goal_type != HealthGoalType.MAINTAIN_WEIGHT and v is None:
            raise ValueError("减重或增重目标必须设置时间周期")
        if v and (v < 1 or v > 104):
            raise ValueError("时间周期必须在 1-104 周之间")
        return v


# ========== 个人资料 ==========
class UserProfileUpdate(BaseModel):
    """个人资料更新请求"""
    username: Optional[str] = Field(None, min_length=2, max_length=50)
    height: Optional[float] = Field(None, ge=50, le=250)
    weight: Optional[float] = Field(None, ge=20, le=300)
    birthdate: Optional[date] = None
    gender: Optional[Gender] = None
    activity_level: Optional[ActivityLevel] = None
    health_goal_type: Optional[HealthGoalType] = None
    target_weight: Optional[float] = Field(None, ge=20, le=300)
    goal_period_weeks: Optional[int] = Field(None, ge=1, le=104)


class UserProfileResponse(BaseModel):
    """用户资料响应"""
    email: EmailStr
    username: str
    height: Optional[float] = None
    weight: Optional[float] = None
    age: Optional[int] = None
    gender: Optional[Gender] = None
    birthdate: Optional[str] = None  # 出生日期（格式：YYYY-MM-DD）
    activity_level: Optional[ActivityLevel] = None
    health_goal_type: Optional[HealthGoalType] = None
    target_weight: Optional[float] = None
    goal_period_weeks: Optional[int] = None
    bmr: Optional[float] = None
    tdee: Optional[float] = None
    daily_calorie_goal: Optional[float] = None


# ========== 密码重置 ==========
class PasswordResetRequest(BaseModel):
    """发送密码重置验证码请求"""
    email: EmailStr


class PasswordResetVerify(BaseModel):
    """验证并重置密码请求"""
    email: EmailStr
    verification_code: str = Field(..., min_length=6, max_length=6)
    new_password: str = Field(..., min_length=6, max_length=100)
    confirm_password: str = Field(..., min_length=6, max_length=100)

    @validator("confirm_password")
    def passwords_match(cls, v, values):
        if "new_password" in values and v != values["new_password"]:
            raise ValueError("两次输入的密码不一致")
        return v


# ========== 通用响应 ==========
class MessageResponse(BaseModel):
    """通用消息响应"""
    message: str
    data: Optional[dict] = None
