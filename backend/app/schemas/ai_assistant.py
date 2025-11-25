from __future__ import annotations

from pydantic import BaseModel, Field, field_validator, ConfigDict, ValidationInfo
from typing import Optional, List, Dict, Any
from datetime import datetime, date
from app.models.food import NutritionData, FullNutritionData


# ========== 拍照识别食物 ==========
class RecognizedFoodItemResponse(BaseModel):
    """识别到的食物项（可编辑）"""
    food_name: str = Field(..., min_length=1, max_length=100, description="食物名称")
    serving_size: float = Field(..., gt=0, description="份量（克）")
    serving_unit: str = Field(default="克", max_length=20, description="份量单位")
    nutrition_per_serving: NutritionData = Field(..., description="每份基础营养数据")
    full_nutrition: Optional[FullNutritionData] = Field(None, description="完整营养信息")
    confidence: Optional[float] = Field(None, ge=0, le=1, description="识别置信度（0-1）")
    food_id: Optional[str] = Field(None, description="匹配到的数据库食物ID（如果找到）")
    source: Optional[str] = Field(None, description="数据来源：ai（AI识别）或 database（数据库匹配）")
    category: Optional[str] = Field(None, max_length=50, description="食物分类")
    image_url: Optional[str] = Field(None, description="食物图片URL")

    class Config:
        json_schema_extra = {
            "example": {
                "food_name": "苹果",
                "serving_size": 150,
                "serving_unit": "克",
                "nutrition_per_serving": {
                    "calories": 81,
                    "protein": 0.45,
                    "carbohydrates": 20.25,
                    "fat": 0.3,
                    "fiber": 3.6,
                    "sugar": 15.3,
                    "sodium": 1.5
                },
                "full_nutrition": {
                    "calory": [
                        {"name_en": "total_calory", "name": "总热量", "value": 81, "unit_name": "Kcal"}
                    ],
                    "base_ingredients": [
                        {
                            "name_en": "carbohydrate",
                            "name": "碳水化合物",
                            "value": 20.25,
                            "unit_name": "g",
                            "items": []
                        }
                    ],
                    "vitamin": [],
                    "mineral": [],
                    "amino_acid": [],
                    "other_ingredients": []
                },
                "confidence": 0.92,
                "food_id": "64f1f0c2e13e5f7b12345678",
                "source": "database",
                "category": "水果",
                "image_url": None
            }
        }


class FoodImageRecognitionResponse(BaseModel):
    """食物图片识别响应"""
    success: bool = Field(..., description="是否识别成功")
    message: str = Field(..., description="响应消息")
    recognized_foods: List[RecognizedFoodItemResponse] = Field(..., description="识别到的食物列表")
    total_calories: float = Field(..., ge=0, description="总热量（所有识别食物的总和）")
    total_nutrition: Optional[NutritionData] = Field(None, description="总营养数据（所有识别食物的总和）")
    image_url: Optional[str] = Field(None, description="上传的图片URL（如果保存）")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "message": "成功识别到3种食物",
                "recognized_foods": [
                    {
                        "food_name": "苹果",
                        "serving_size": 150,
                        "serving_unit": "克",
                        "nutrition_per_serving": {
                            "calories": 81,
                            "protein": 0.45,
                            "carbohydrates": 20.25,
                            "fat": 0.3,
                            "fiber": 3.6,
                            "sugar": 15.3,
                            "sodium": 1.5
                        },
                        "confidence": 0.92,
                        "food_id": "64f1f0c2e13e5f7b12345678",
                        "source": "database",
                        "category": "水果"
                    },
                    {
                        "food_name": "白米饭",
                        "serving_size": 200,
                        "serving_unit": "克",
                        "nutrition_per_serving": {
                            "calories": 260,
                            "protein": 5.2,
                            "carbohydrates": 58,
                            "fat": 0.6,
                            "fiber": 0.6,
                            "sugar": 0,
                            "sodium": 2
                        },
                        "confidence": 0.88,
                        "food_id": None,
                        "source": "ai",
                        "category": "主食"
                    }
                ],
                "total_calories": 341,
                "total_nutrition": {
                    "calories": 341,
                    "protein": 5.65,
                    "carbohydrates": 78.25,
                    "fat": 0.9,
                    "fiber": 4.2,
                    "sugar": 15.3,
                    "sodium": 3.5
                },
                "image_url": "https://example.com/uploads/food_image_20251103.jpg"
            }
        }


class FoodRecognitionConfirmRequest(BaseModel):
    """确认识别结果并添加到饮食日志的请求"""
    recognized_foods: List[RecognizedFoodItemResponse] = Field(..., min_length=1, description="确认后的食物列表（用户可编辑）")
    recorded_at: datetime = Field(..., description="摄入时间")
    meal_type: Optional[str] = Field(None, description="餐次类型")
    notes: Optional[str] = Field(None, max_length=500, description="备注")

    @field_validator("meal_type")
    @classmethod
    def validate_meal_type(cls, v):
        if v and v not in ["早餐", "午餐", "晚餐", "加餐", "breakfast", "lunch", "dinner", "snack"]:
            raise ValueError("餐次类型必须是：早餐、午餐、晚餐、加餐 之一")
        return v

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "recognized_foods": [
                    {
                        "food_name": "苹果",
                        "serving_size": 150,
                        "serving_unit": "克",
                        "nutrition_per_serving": {
                            "calories": 81,
                            "protein": 0.45,
                            "carbohydrates": 20.25,
                            "fat": 0.3,
                            "fiber": 3.6,
                            "sugar": 15.3,
                            "sodium": 1.5
                        },
                        "food_id": "64f1f0c2e13e5f7b12345678",
                        "source": "database"
                    }
                ],
                "recorded_at": "2025-11-03T12:30:00",
                "meal_type": "午餐",
                "notes": "AI识别后确认"
            }
        }
    )


class FoodRecognitionConfirmResponse(BaseModel):
    """确认识别结果响应"""
    success: bool = Field(..., description="是否成功添加")
    message: str = Field(..., description="响应消息")
    created_records: List[str] = Field(..., description="创建的食物记录ID列表")
    total_records: int = Field(..., ge=0, description="成功创建的记录数量")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "message": "成功添加3条食物记录到饮食日志",
                "created_records": [
                    "64f1f0c2e13e5f7b12345678",
                    "64f1f0c2e13e5f7b12345679",
                    "64f1f0c2e13e5f7b12345680"
                ],
                "total_records": 3
            }
        }


# ========== 生成个性化饮食计划 ==========
class FoodPreferenceRequest(BaseModel):
    """食物偏好"""
    liked_foods: Optional[List[str]] = Field(None, description="喜欢的食物列表")
    disliked_foods: Optional[List[str]] = Field(None, description="不吃的食物列表")
    allergies: Optional[List[str]] = Field(None, description="过敏食物列表")
    dietary_restrictions: Optional[List[str]] = Field(None, description="饮食限制（如：素食、无麸质、低钠等）")
    preferred_tastes: Optional[List[str]] = Field(None, description="偏好的口味（如：清淡、辛辣、甜味等）")
    cooking_skills: Optional[str] = Field(None, description="烹饪技能水平（如：初级、中级、高级）")

    class Config:
        json_schema_extra = {
            "example": {
                "liked_foods": ["苹果", "鸡胸肉", "西兰花"],
                "disliked_foods": ["茄子", "苦瓜"],
                "allergies": ["花生", "海鲜"],
                "dietary_restrictions": ["低钠"],
                "preferred_tastes": ["清淡", "微甜"],
                "cooking_skills": "初级"
            }
        }


class MealPlanRequest(BaseModel):
    """生成个性化饮食计划请求"""
    plan_duration: str = Field(..., description="计划时间：day（天）、week（周）")
    plan_days: Optional[int] = Field(None, ge=1, le=30, description="计划天数（当plan_duration为day时必填，最多30天）")
    include_budget: bool = Field(default=False, description="是否考虑预算")
    budget_per_day: Optional[float] = Field(None, gt=0, description="每日预算（元，可选）")
    food_preference: Optional[FoodPreferenceRequest] = Field(None, description="食物偏好")
    target_calories: Optional[float] = Field(None, gt=0, description="目标每日热量（千卡，可选，不填则使用用户资料中的目标）")
    meals_per_day: int = Field(default=3, ge=2, le=6, description="每日餐次数量（2-6餐）")

    @field_validator("plan_duration")
    @classmethod
    def validate_plan_duration(cls, v):
        if v not in ["day", "week"]:
            raise ValueError("计划时间必须是：day（天）或 week（周）")
        return v

    @field_validator("plan_days")
    @classmethod
    def validate_plan_days(cls, v, info: ValidationInfo):
        # Pydantic V2: 使用 ValidationInfo 获取同级字段数据
        plan_duration = info.data.get("plan_duration")
        if plan_duration == "day" and v is None:
            raise ValueError("当计划时间为day时，必须指定plan_days")
        return v

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "plan_duration": "week",
                "include_budget": True,
                "budget_per_day": 50,
                "food_preference": {
                    "liked_foods": ["苹果", "鸡胸肉", "西兰花"],
                    "disliked_foods": ["茄子"],
                    "allergies": ["花生"],
                    "dietary_restrictions": ["低钠"],
                    "preferred_tastes": ["清淡"],
                    "cooking_skills": "初级"
                },
                "target_calories": 1800,
                "meals_per_day": 3
            }
        }
    )


class MealItemResponse(BaseModel):
    """单餐食物项"""
    food_name: str = Field(..., description="食物名称")
    serving_size: float = Field(..., gt=0, description="建议份量（克）")
    serving_unit: str = Field(default="克", description="份量单位")
    cooking_method: Optional[str] = Field(None, description="烹饪方法")
    nutrition: NutritionData = Field(..., description="营养数据")
    estimated_cost: Optional[float] = Field(None, ge=0, description="预估成本（元）")

    class Config:
        json_schema_extra = {
            "example": {
                "food_name": "鸡胸肉",
                "serving_size": 150,
                "serving_unit": "克",
                "cooking_method": "清蒸",
                "nutrition": {
                    "calories": 247.5,
                    "protein": 46.5,
                    "carbohydrates": 0,
                    "fat": 5.25,
                    "fiber": 0,
                    "sugar": 0,
                    "sodium": 82.5
                },
                "estimated_cost": 8.5
            }
        }


class DailyMealPlanResponse(BaseModel):
    """每日饮食计划"""
    date: str = Field(..., description="日期（YYYY-MM-DD）")
    meals: Dict[str, List[MealItemResponse]] = Field(..., description="餐次字典，key为餐次名称（如：早餐、午餐、晚餐），value为食物列表")
    daily_nutrition: NutritionData = Field(..., description="每日总营养数据")
    daily_calories: float = Field(..., ge=0, description="每日总热量（千卡）")
    daily_cost: Optional[float] = Field(None, ge=0, description="每日预估成本（元）")
    macro_ratio: Dict[str, float] = Field(..., description="宏量营养素比例（蛋白质、碳水化合物、脂肪的百分比）")

    class Config:
        json_schema_extra = {
            "example": {
                "date": "2025-11-04",
                "meals": {
                    "早餐": [
                        {
                            "food_name": "燕麦片",
                            "serving_size": 50,
                            "serving_unit": "克",
                            "cooking_method": "煮",
                            "nutrition": {
                                "calories": 195,
                                "protein": 6.5,
                                "carbohydrates": 33.5,
                                "fat": 3.5,
                                "fiber": 5,
                                "sugar": 0.5,
                                "sodium": 2
                            },
                            "estimated_cost": 3.5
                        }
                    ],
                    "午餐": [
                        {
                            "food_name": "鸡胸肉",
                            "serving_size": 150,
                            "serving_unit": "克",
                            "cooking_method": "清蒸",
                            "nutrition": {
                                "calories": 247.5,
                                "protein": 46.5,
                                "carbohydrates": 0,
                                "fat": 5.25,
                                "fiber": 0,
                                "sugar": 0,
                                "sodium": 82.5
                            },
                            "estimated_cost": 8.5
                        }
                    ],
                    "晚餐": []
                },
                "daily_nutrition": {
                    "calories": 1800,
                    "protein": 135,
                    "carbohydrates": 180,
                    "fat": 60,
                    "fiber": 25,
                    "sugar": 30,
                    "sodium": 2000
                },
                "daily_calories": 1800,
                "daily_cost": 45.5,
                "macro_ratio": {
                    "protein": 30,
                    "carbohydrates": 40,
                    "fat": 30
                }
            }
        }


class MealPlanResponse(BaseModel):
    """个性化饮食计划响应"""
    success: bool = Field(..., description="是否生成成功")
    message: str = Field(..., description="响应消息")
    plan_duration: str = Field(..., description="计划时间：day（天）或 week（周）")
    plan_days: int = Field(..., ge=1, description="计划天数")
    target_calories: float = Field(..., gt=0, description="目标每日热量（千卡）")
    daily_plans: List[DailyMealPlanResponse] = Field(..., description="每日饮食计划列表")
    total_cost: Optional[float] = Field(None, ge=0, description="总预估成本（元）")
    average_daily_cost: Optional[float] = Field(None, ge=0, description="平均每日成本（元）")
    nutrition_summary: Dict[str, Any] = Field(..., description="营养摘要（平均每日营养数据、宏量营养素比例等）")
    suggestions: Optional[List[str]] = Field(None, description="个性化建议和提醒")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "message": "成功生成7天个性化饮食计划",
                "plan_duration": "week",
                "plan_days": 7,
                "target_calories": 1800,
                "daily_plans": [
                    {
                        "date": "2025-11-04",
                        "meals": {
                            "早餐": [],
                            "午餐": [],
                            "晚餐": []
                        },
                        "daily_nutrition": {
                            "calories": 1800,
                            "protein": 135,
                            "carbohydrates": 180,
                            "fat": 60,
                            "fiber": 25,
                            "sugar": 30,
                            "sodium": 2000
                        },
                        "daily_calories": 1800,
                        "daily_cost": 45.5,
                        "macro_ratio": {
                            "protein": 30,
                            "carbohydrates": 40,
                            "fat": 30
                        }
                    }
                ],
                "total_cost": 318.5,
                "average_daily_cost": 45.5,
                "nutrition_summary": {
                    "average_daily_calories": 1800,
                    "average_daily_protein": 135,
                    "average_daily_carbohydrates": 180,
                    "average_daily_fat": 60,
                    "average_macro_ratio": {
                        "protein": 30,
                        "carbohydrates": 40,
                        "fat": 30
                    }
                },
                "suggestions": [
                    "建议在训练前1小时补充碳水化合物",
                    "保持充足的水分摄入，每天至少2000ml",
                    "可以根据实际情况调整份量，但尽量保持营养比例"
                ]
            }
        }


# ========== 营养知识问答 ==========
class NutritionQuestionRequest(BaseModel):
    """营养知识问答请求"""
    question: str = Field(..., min_length=1, max_length=500, description="用户问题（自然语言）")
    context: Optional[Dict[str, Any]] = Field(
        None, 
        description="上下文信息（可选）。如果未提供，系统会自动从用户档案中读取相关信息（如体重、活动水平、健康目标等）。如果提供，则优先使用请求中的值。支持的字段：user_goal（用户目标）、activity_level（活动水平）、weight（体重）、height（身高）、age（年龄）"
    )

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "question": "蛋白质补充的最佳时间是什么时候？",
                "context": {
                    "user_goal": "增肌",
                    "activity_level": "high"
                }
            }
        }
    )


class NutritionQuestionResponse(BaseModel):
    """营养知识问答响应"""
    success: bool = Field(..., description="是否回答成功")
    question: str = Field(..., description="用户问题")
    answer: str = Field(..., description="AI回答内容")
    related_topics: Optional[List[str]] = Field(None, description="相关话题建议")
    sources: Optional[List[str]] = Field(None, description="参考来源（如：营养学指南、研究论文等）")
    confidence: Optional[float] = Field(None, ge=0, le=1, description="回答置信度（0-1）")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "question": "蛋白质补充的最佳时间是什么时候？",
                "answer": "蛋白质补充的最佳时间主要有以下几个关键时段：\n\n1. **训练后30分钟内**：这是最重要的窗口期，此时肌肉对蛋白质的吸收效率最高，建议摄入20-30克优质蛋白质。\n\n2. **早餐时**：经过一夜的禁食，身体需要蛋白质来启动新陈代谢和维持肌肉质量。\n\n3. **睡前**：摄入缓释蛋白质（如酪蛋白）可以帮助夜间肌肉修复和生长。\n\n4. **餐间**：如果目标是增肌，可以在正餐之间补充蛋白质，保持全天蛋白质摄入的均匀分布。\n\n需要注意的是，总体的每日蛋白质摄入量比单次摄入时间更重要。建议根据您的活动水平和目标，每日摄入1.6-2.2克/公斤体重的蛋白质。",
                "related_topics": [
                    "蛋白质摄入量计算",
                    "训练后营养补充",
                    "增肌饮食计划"
                ],
                "sources": [
                    "中国居民膳食指南（2022）",
                    "运动营养学原理"
                ],
                "confidence": 0.95
            }
        }


# ========== 运动知识问答 ==========
class SportsQuestionRequest(BaseModel):
    """运动知识问答请求"""
    question: str = Field(..., min_length=1, max_length=500, description="用户问题（自然语言）")
    context: Optional[Dict[str, Any]] = Field(
        None, 
        description="上下文信息（可选）。如果未提供，系统会自动从用户档案中读取相关信息（如体重、身高、活动水平、健康目标等）。如果提供，则优先使用请求中的值。支持的字段：user_goal（用户目标）、activity_level（活动水平）、weight（体重）、height（身高）、age（年龄）"
    )

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "question": "如何制定一个有效的减脂运动计划？",
                "context": {
                    "user_goal": "减脂",
                    "activity_level": "moderately_active",
                    "weight": 70,
                    "height": 175
                }
            }
        }
    )


class SportsQuestionResponse(BaseModel):
    """运动知识问答响应"""
    success: bool = Field(..., description="是否回答成功")
    question: str = Field(..., description="用户问题")
    answer: str = Field(..., description="AI回答内容")
    related_topics: Optional[List[str]] = Field(None, description="相关话题建议")
    sources: Optional[List[str]] = Field(None, description="参考来源（如：运动科学指南、研究论文等）")
    confidence: Optional[float] = Field(None, ge=0, le=1, description="回答置信度（0-1）")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "question": "如何制定一个有效的减脂运动计划？",
                "answer": "制定有效的减脂运动计划需要考虑以下几个方面：\n\n1. **有氧运动**：建议每周进行150-300分钟的中等强度有氧运动，如快走、慢跑、游泳、骑行等。\n\n2. **力量训练**：每周进行2-3次力量训练，有助于增加肌肉量，提高基础代谢率。\n\n3. **运动频率**：建议每周至少运动3-5次，保持规律性。\n\n4. **循序渐进**：从低强度开始，逐步增加运动强度和时长。\n\n5. **结合饮食**：运动减脂需要配合合理的饮食控制，创造热量缺口。\n\n需要注意的是，运动计划应根据个人身体状况和目标制定，如有健康问题请咨询专业教练或医生。",
                "related_topics": [
                    "有氧运动计划",
                    "力量训练基础",
                    "运动与营养搭配"
                ],
                "sources": [
                    "运动科学原理",
                    "ACSM运动指南"
                ],
                "confidence": 0.9
            }
        }


# ========== 智能提醒与反馈 ==========
class ReminderSettings(BaseModel):
    """提醒设置"""
    meal_reminders: bool = Field(default=True, description="是否开启餐次提醒")
    meal_reminder_times: Optional[List[str]] = Field(None, description="餐次提醒时间列表（格式：HH:MM，如：['07:00', '12:00', '18:00']）")
    record_reminders: bool = Field(default=True, description="是否开启记录提醒")
    record_reminder_hours: Optional[int] = Field(None, ge=1, le=24, description="未记录提醒间隔（小时）")
    goal_reminders: bool = Field(default=True, description="是否开启目标达成提醒")
    motivational_messages: bool = Field(default=True, description="是否开启鼓励性消息")

    class Config:
        json_schema_extra = {
            "example": {
                "meal_reminders": True,
                "meal_reminder_times": ["07:00", "12:00", "18:00"],
                "record_reminders": True,
                "record_reminder_hours": 3,
                "goal_reminders": True,
                "motivational_messages": True
            }
        }


class ReminderSettingsRequest(BaseModel):
    """更新提醒设置请求"""
    settings: ReminderSettings = Field(..., description="提醒设置")

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "settings": {
                    "meal_reminders": True,
                    "meal_reminder_times": ["07:00", "12:00", "18:00"],
                    "record_reminders": True,
                    "record_reminder_hours": 3,
                    "goal_reminders": True,
                    "motivational_messages": True
                }
            }
        }
    )


class ReminderSettingsResponse(BaseModel):
    """提醒设置响应"""
    success: bool = Field(..., description="是否更新成功")
    message: str = Field(..., description="响应消息")
    settings: ReminderSettings = Field(..., description="当前提醒设置")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "message": "提醒设置已更新",
                "settings": {
                    "meal_reminders": True,
                    "meal_reminder_times": ["07:00", "12:00", "18:00"],
                    "record_reminders": True,
                    "record_reminder_hours": 3,
                    "goal_reminders": True,
                    "motivational_messages": True
                }
            }
        }


class NotificationMessageResponse(BaseModel):
    """通知消息"""
    id: str = Field(..., description="消息ID")
    type: str = Field(..., description="消息类型：meal_reminder（餐次提醒）、record_reminder（记录提醒）、goal_achievement（目标达成）、motivational（鼓励消息）、feedback（反馈消息）")
    title: str = Field(..., description="消息标题")
    content: str = Field(..., description="消息内容")
    created_at: datetime = Field(..., description="创建时间")
    read: bool = Field(default=False, description="是否已读")
    action_url: Optional[str] = Field(None, description="操作链接（可选）")
    priority: str = Field(default="normal", description="优先级：low、normal、high")

    class Config:
        json_schema_extra = {
            "example": {
                "id": "64f1f0c2e13e5f7b12345678",
                "type": "meal_reminder",
                "title": "午餐时间到了",
                "content": "记得享用您的健康午餐！建议摄入约600千卡的热量。",
                "created_at": "2025-11-03T12:00:00",
                "read": False,
                "action_url": "/food/record",
                "priority": "normal"
            }
        }


class NotificationListResponse(BaseModel):
    """通知列表响应"""
    total: int = Field(..., ge=0, description="总消息数")
    unread_count: int = Field(..., ge=0, description="未读消息数")
    notifications: List[NotificationMessageResponse] = Field(..., description="通知消息列表")

    class Config:
        json_schema_extra = {
            "example": {
                "total": 10,
                "unread_count": 3,
                "notifications": [
                    {
                        "id": "64f1f0c2e13e5f7b12345678",
                        "type": "meal_reminder",
                        "title": "午餐时间到了",
                        "content": "记得享用您的健康午餐！",
                        "created_at": "2025-11-03T12:00:00",
                        "read": False,
                        "priority": "normal"
                    }
                ]
            }
        }


class FeedbackDataResponse(BaseModel):
    """反馈数据

    注意：为避免 Pydantic v2 在生成 schema 时与嵌套模型产生兼容性问题，
    这里将日期与营养摘要简化为基础类型，便于前后端解耦。
    """

    # 使用字符串表示日期（YYYY-MM-DD），避免 datetime.date 在某些环境下的 schema 问题
    date: str = Field(..., description="日期（YYYY-MM-DD）")
    daily_calories: float = Field(..., ge=0, description="当日摄入热量")
    target_calories: float = Field(..., gt=0, description="目标热量")
    calories_progress: float = Field(..., ge=0, le=1, description="热量完成进度（0-1）")
    # 使用 dict 表示营养摘要，结构与 NutritionData 一致，由调用方自行约定字段含义
    nutrition_summary: Dict[str, Any] = Field(..., description="营养摘要（结构与 NutritionData 相同）")
    meal_count: int = Field(..., ge=0, description="进食次数")
    goal_status: str = Field(..., description="目标状态：on_track（正常）、exceeded（超标）、below（不足）")
    suggestions: List[str] = Field(..., description="个性化建议")

    class Config:
        json_schema_extra = {
            "example": {
                "date": "2025-11-03",
                "daily_calories": 1750,
                "target_calories": 1800,
                "calories_progress": 0.97,
                "nutrition_summary": {
                    "calories": 1750,
                    "protein": 130,
                    "carbohydrates": 175,
                    "fat": 58,
                    "fiber": 22,
                    "sugar": 28,
                    "sodium": 1950
                },
                "meal_count": 3,
                "goal_status": "on_track",
                "suggestions": [
                    "今日营养摄入良好，继续保持！",
                    "建议增加一些优质蛋白质的摄入",
                    "记得多喝水，保持充足水分"
                ]
            }
        }


class DailyFeedbackResponse(BaseModel):
    """每日反馈响应"""
    success: bool = Field(..., description="是否获取成功")
    feedback: FeedbackDataResponse = Field(..., description="反馈数据")
    notification: Optional[NotificationMessageResponse] = Field(None, description="相关通知消息（如果有）")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "feedback": {
                    "date": "2025-11-03",
                    "daily_calories": 1750,
                    "target_calories": 1800,
                    "calories_progress": 0.97,
                    "nutrition_summary": {
                        "calories": 1750,
                        "protein": 130,
                        "carbohydrates": 175,
                        "fat": 58,
                        "fiber": 22,
                        "sugar": 28,
                        "sodium": 1950
                    },
                    "meal_count": 3,
                    "goal_status": "on_track",
                    "suggestions": [
                        "今日营养摄入良好，继续保持！"
                    ]
                },
                "notification": {
                    "id": "64f1f0c2e13e5f7b12345679",
                    "type": "feedback",
                    "title": "今日目标达成",
                    "content": "恭喜！您今日的营养摄入已接近目标，继续保持！",
                    "created_at": "2025-11-03T20:00:00",
                    "read": False,
                    "priority": "normal"
                }
            }
        }


class NotificationReadRequest(BaseModel):
    """标记通知为已读请求"""
    notification_ids: List[str] = Field(..., min_length=1, description="通知ID列表")

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "notification_ids": [
                    "64f1f0c2e13e5f7b12345678",
                    "64f1f0c2e13e5f7b12345679"
                ]
            }
        }
    )

