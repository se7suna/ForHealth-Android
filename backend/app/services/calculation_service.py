from app.models.user import PAL_COEFFICIENTS, Gender, ActivityLevel, HealthGoalType
from datetime import date


def calculate_age(birthdate: date) -> int:
    """
    根据出生日期计算当前年龄

    Args:
        birthdate: 出生日期

    Returns:
        年龄（周岁）
    """
    today = date.today()
    age = today.year - birthdate.year

    # 如果今年生日还没到，年龄减1
    if (today.month, today.day) < (birthdate.month, birthdate.day):
        age -= 1

    return age


def calculate_bmr(weight: float, height: float, age: int, gender: Gender) -> float:
    """
    计算基础代谢率（BMR）- 使用 Mifflin-St Jeor 公式

    Args:
        weight: 体重（公斤）
        height: 身高（厘米）
        age: 年龄
        gender: 性别

    Returns:
        BMR 值（卡路里/天）
    """
    if gender == Gender.MALE:
        bmr = 10 * weight + 6.25 * height - 5 * age + 5
    else:  # FEMALE
        bmr = 10 * weight + 6.25 * height - 5 * age - 161

    return round(bmr, 2)


def calculate_tdee(bmr: float, activity_level: ActivityLevel) -> float:
    """
    计算每日总能量消耗（TDEE）

    Args:
        bmr: 基础代谢率
        activity_level: 活动水平

    Returns:
        TDEE 值（卡路里/天）
    """
    pal_coefficient = PAL_COEFFICIENTS[activity_level]
    tdee = bmr * pal_coefficient
    return round(tdee, 2)


def calculate_daily_calorie_goal(
    tdee: float,
    health_goal_type: HealthGoalType
) -> float:
    """
    计算每日卡路里目标

    Args:
        tdee: 每日总能量消耗
        health_goal_type: 健康目标类型

    Returns:
        每日卡路里目标（卡路里/天）
    """
    if health_goal_type == HealthGoalType.LOSE_WEIGHT:
        # 减重：TDEE - 500
        goal = tdee - 500
    elif health_goal_type == HealthGoalType.GAIN_WEIGHT:
        # 增重：TDEE + 500
        goal = tdee + 500
    else:  # MAINTAIN_WEIGHT
        # 保持：TDEE
        goal = tdee

    return round(goal, 2)
