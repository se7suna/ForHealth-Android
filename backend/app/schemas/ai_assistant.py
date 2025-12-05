from __future__ import annotations

from pydantic import BaseModel, Field, field_validator, ConfigDict
from typing import Optional, List, Dict, Any
from datetime import datetime
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


class ProcessedFoodItem(BaseModel):
    """处理后的食物信息"""
    food_id: str = Field(..., description="食物ID（用于创建饮食记录）")
    food_name: str = Field(..., description="食物名称")
    serving_amount: float = Field(..., ge=0, description="建议的食用份量数（基于识别结果计算）")
    serving_size: float = Field(..., ge=0, description="识别到的份量大小")
    serving_unit: str = Field(..., description="份量单位")
    nutrition_per_serving: NutritionData = Field(..., description="每份基础营养数据")
    source: str = Field(..., description="数据来源：ai（AI识别）或 database（数据库匹配）")


class FoodRecognitionConfirmResponse(BaseModel):
    """确认识别结果响应"""
    success: bool = Field(..., description="是否成功处理")
    message: str = Field(..., description="响应消息")
    processed_foods: List[ProcessedFoodItem] = Field(..., description="处理后的食物信息列表（包含 food_id 和 serving_amount 建议）")
    total_foods: int = Field(..., ge=0, description="成功处理的食物数量")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "message": "成功处理3种食物，请调用 /api/food/record 创建饮食记录",
                "processed_foods": [
                    {
                        "food_id": "64f1f0c2e13e5f7b12345678",
                        "food_name": "苹果",
                        "serving_amount": 1.5,
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
                        "source": "database"
                    },
                    {
                        "food_id": "64f1f0c2e13e5f7b12345679",
                        "food_name": "香蕉",
                        "serving_amount": 1.2,
                        "serving_size": 120,
                        "serving_unit": "克",
                        "nutrition_per_serving": {
                            "calories": 105,
                            "protein": 1.3,
                            "carbohydrates": 27,
                            "fat": 0.3,
                            "fiber": 3.1,
                            "sugar": 14.4,
                            "sodium": 1
                        },
                        "source": "ai"
                    }
                ],
                "total_foods": 2
            }
        }


# ========== 健康知识问答 ==========
class QuestionRequest(BaseModel):
    """健康知识问答请求"""
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


class QuestionResponse(BaseModel):
    """健康知识问答响应"""
    success: bool = Field(..., description="是否回答成功")
    question: str = Field(..., description="用户问题")
    answer: str = Field(..., description="AI回答内容")
    related_topics: Optional[List[str]] = Field(None, description="相关话题建议")
    sources: Optional[List[str]] = Field(None, description="参考来源（如：营养学指南、运动科学指南、研究论文等）")
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


# ========== 饮食分析与建议 ==========
class DietAnalysisRequest(BaseModel):
    """饮食分析请求"""
    days: int = Field(default=7, ge=1, le=30, description="分析最近几天的记录（默认7天）")

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "days": 7
            }
        }
    )


class DietAnalysisResponse(BaseModel):
    """饮食分析响应"""
    success: bool = Field(..., description="是否分析成功")
    message: str = Field(..., description="一句话分析建议（亲和语气）")
    analysis: Optional[Dict[str, Any]] = Field(None, description="详细分析数据")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "message": "最近蛋白质摄入偏低哦～建议多吃些鸡蛋、鸡胸肉补充一下！💪",
                "analysis": {
                    "days_analyzed": 7,
                    "avg_calories_intake": 1650,
                    "avg_calories_target": 1800,
                    "calorie_balance": "略有不足",
                    "macro_ratio": {
                        "protein_percent": 15,
                        "carbs_percent": 55,
                        "fat_percent": 30
                    },
                    "main_issue": "蛋白质摄入不足"
                }
            }
        }


# ========== 智能菜式推荐 ==========
class MealRecommendationResponse(BaseModel):
    """智能菜式推荐响应"""
    success: bool = Field(..., description="是否推荐成功")
    message: str = Field(..., description="推荐语（包含时间提醒和菜式推荐）")
    meal_type: str = Field(..., description="推荐的餐次类型：早餐、午餐、晚餐、加餐")
    recommended_dish: str = Field(..., description="推荐的菜式名称")
    reason: str = Field(..., description="推荐理由（基于营养需求）")
    nutrition_highlight: Optional[str] = Field(None, description="营养亮点")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "message": "到中午了！记得吃午饭哦！向你推荐鸡排饭，可以补充蛋白质～🍗",
                "meal_type": "午餐",
                "recommended_dish": "鸡排饭",
                "reason": "最近蛋白质摄入偏低，鸡排富含优质蛋白",
                "nutrition_highlight": "高蛋白、适量碳水"
            }
        }

