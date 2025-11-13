from pydantic import BaseModel, Field, ConfigDict
from typing import Optional, List
from datetime import datetime
from app.models.food import NutritionData, FullNutritionData
from app.models.recipe import RecipeFoodItem


# ========== 食谱管理 ==========
class RecipeCreateRequest(BaseModel):
    """创建食谱请求"""
    name: str = Field(..., min_length=1, max_length=100, description="食谱名称")
    description: Optional[str] = Field(None, max_length=500, description="食谱描述")
    category: Optional[str] = Field(None, max_length=50, description="分类")
    foods: List[RecipeFoodItem] = Field(..., min_items=1, description="食物列表（至少1个）")
    tags: Optional[List[str]] = Field(None, description="标签")
    image_url: Optional[str] = Field(None, description="食谱图片URL")
    prep_time: Optional[int] = Field(None, gt=0, description="准备时间（分钟）")

    class Config:
        json_schema_extra = {
            "example": {
                "name": "能量午餐盒",
                "description": "高蛋白、低脂的午餐组合，适合训练日补给",
                "category": "午餐",
                "foods": [
                    {
                        "food_id": "507f1f77bcf86cd799439021",
                        "food_name": "香煎鸡胸肉",
                        "serving_amount": 1.0,
                        "serving_size": 150,
                        "serving_unit": "克",
                        "nutrition": {
                            "calories": 198,
                            "protein": 37.3,
                            "carbohydrates": 0,
                            "fat": 4.5,
                            "fiber": 0,
                            "sodium": 95
                        },
                        "full_nutrition": {
                            "calory": [
                                {"name_en": "total_calory", "name": "总热量", "value": 198, "unit_name": "Kcal"},
                                {"name_en": "total_kj", "name": "总热量", "value": 828, "unit_name": "Kj"},
                                {"name_en": "protein", "name": "蛋白质热量", "value": 149.2, "unit_name": "Kcal", "percent": 75.4},
                                {"name_en": "fat", "name": "脂肪热量", "value": 40.5, "unit_name": "Kcal", "percent": 20.5},
                                {"name_en": "carbohydrate", "name": "碳水化合物热量", "value": 0, "unit_name": "Kcal", "percent": 0}
                            ],
                            "base_ingredients": [
                                {
                                    "name_en": "protein",
                                    "name": "蛋白质",
                                    "value": 37.3,
                                    "unit_name": "g",
                                    "items": [
                                        {"name_en": "good_protein", "name": "优质蛋白", "value": 37.3, "unit_name": "g"}
                                    ]
                                },
                                {
                                    "name_en": "fat",
                                    "name": "脂肪",
                                    "value": 4.5,
                                    "unit_name": "g",
                                    "items": [
                                        {"name_en": "saturated_fat", "name": "饱和脂肪", "value": 1.1, "unit_name": "g"},
                                        {"name_en": "fa_mufa", "name": "单不饱和脂肪酸", "value": 2.0, "unit_name": "g"},
                                        {"name_en": "pufa", "name": "多不饱和脂肪酸", "value": 1.2, "unit_name": "g"}
                                    ]
                                },
                                {
                                    "name_en": "carbohydrate",
                                    "name": "碳水化合物",
                                    "value": 0,
                                    "unit_name": "g",
                                    "items": []
                                }
                            ],
                            "vitamin": [
                                {"name": "维生素B6", "value": 0.8, "unit_name": "mg"},
                                {"name": "烟酸", "value": 18.2, "unit_name": "mg"}
                            ],
                            "mineral": [
                                {"name": "钠", "value": 0.095, "unit_name": "g"},
                                {"name": "钾", "value": 0.42, "unit_name": "g"},
                                {"name": "磷", "value": 0.31, "unit_name": "g"},
                                {"name": "镁", "value": 42, "unit_name": "mg"}
                            ],
                            "amino_acid": [
                                {"name": "异亮氨酸", "value": 2200, "unit_name": "mg"},
                                {"name": "亮氨酸", "value": 3600, "unit_name": "mg"},
                                {"name": "赖氨酸", "value": 3800, "unit_name": "mg"}
                            ],
                            "other_ingredients": []
                        }
                    },
                    {
                        "food_id": "507f1f77bcf86cd799439031",
                        "food_name": "藜麦蔬菜沙拉",
                        "serving_amount": 1.0,
                        "serving_size": 180,
                        "serving_unit": "克",
                        "nutrition": {
                            "calories": 238,
                            "protein": 8.6,
                            "carbohydrates": 32.4,
                            "fat": 8.4,
                            "fiber": 6.8,
                            "sodium": 210
                        },
                        "full_nutrition": {
                            "calory": [
                                {"name_en": "total_calory", "name": "总热量", "value": 238, "unit_name": "Kcal"},
                                {"name_en": "total_kj", "name": "总热量", "value": 996, "unit_name": "Kj"},
                                {"name_en": "protein", "name": "蛋白质热量", "value": 34.4, "unit_name": "Kcal", "percent": 14.5},
                                {"name_en": "fat", "name": "脂肪热量", "value": 75.6, "unit_name": "Kcal", "percent": 31.8},
                                {"name_en": "carbohydrate", "name": "碳水化合物热量", "value": 128.0, "unit_name": "Kcal", "percent": 53.7}
                            ],
                            "base_ingredients": [
                                {
                                    "name_en": "carbohydrate",
                                    "name": "碳水化合物",
                                    "value": 32.4,
                                    "unit_name": "g",
                                    "items": [
                                        {"name_en": "fiber_dietary", "name": "膳食纤维", "value": 6.8, "unit_name": "g"},
                                        {"name_en": "sugar", "name": "糖", "value": 5.1, "unit_name": "g"}
                                    ]
                                },
                                {
                                    "name_en": "protein",
                                    "name": "蛋白质",
                                    "value": 8.6,
                                    "unit_name": "g",
                                    "items": []
                                },
                                {
                                    "name_en": "fat",
                                    "name": "脂肪",
                                    "value": 8.4,
                                    "unit_name": "g",
                                    "items": [
                                        {"name_en": "saturated_fat", "name": "饱和脂肪", "value": 1.1, "unit_name": "g"},
                                        {"name_en": "fa_mufa", "name": "单不饱和脂肪酸", "value": 4.3, "unit_name": "g"},
                                        {"name_en": "pufa", "name": "多不饱和脂肪酸", "value": 2.6, "unit_name": "g"}
                                    ]
                                }
                            ],
                            "vitamin": [
                                {"name": "维生素C", "value": 42, "unit_name": "mg"},
                                {"name": "叶酸", "value": 95, "unit_name": "μg"}
                            ],
                            "mineral": [
                                {"name": "钠", "value": 0.21, "unit_name": "g"},
                                {"name": "钙", "value": 92, "unit_name": "mg"},
                                {"name": "钾", "value": 0.54, "unit_name": "g"},
                                {"name": "镁", "value": 68, "unit_name": "mg"}
                            ],
                            "amino_acid": [
                                {"name": "异亮氨酸", "value": 350, "unit_name": "mg"},
                                {"name": "亮氨酸", "value": 620, "unit_name": "mg"},
                                {"name": "赖氨酸", "value": 480, "unit_name": "mg"}
                            ],
                            "other_ingredients": [
                                {"name": "嘌呤", "value": 22, "unit_name": "mg"}
                            ]
                        }
                    }
                ],
                "tags": ["午餐", "高蛋白", "轻食"]
            }
        }


class RecipeUpdateRequest(BaseModel):
    """更新食谱请求"""
    name: Optional[str] = Field(None, min_length=1, max_length=100, description="食谱名称")
    description: Optional[str] = Field(None, max_length=500, description="食谱描述")
    category: Optional[str] = Field(None, max_length=50, description="分类")
    foods: Optional[List[RecipeFoodItem]] = Field(None, min_items=1, description="食物列表")
    tags: Optional[List[str]] = Field(None, description="标签")
    image_url: Optional[str] = Field(None, description="食谱图片URL")
    prep_time: Optional[int] = Field(None, gt=0, description="准备时间（分钟）")


class RecipeResponse(BaseModel):
    """食谱响应"""
    id: str = Field(..., description="食谱ID")
    name: str
    description: Optional[str] = None
    category: Optional[str] = None
    foods: List[RecipeFoodItem]
    total_nutrition: NutritionData
    total_full_nutrition: Optional[FullNutritionData] = Field(None, description="总完整营养信息（与测试脚本格式一致）")
    tags: Optional[List[str]] = None
    image_url: Optional[str] = None
    prep_time: Optional[int] = None
    created_by: Optional[str] = None
    created_at: datetime
    updated_at: datetime


class RecipeSearchRequest(BaseModel):
    """食谱搜索请求"""
    keyword: Optional[str] = Field(None, description="搜索关键词")
    category: Optional[str] = Field(None, description="分类筛选")
    tags: Optional[List[str]] = Field(None, description="标签筛选")
    limit: int = Field(default=20, ge=1, le=100, description="返回数量限制")
    offset: int = Field(default=0, ge=0, description="偏移量")

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "keyword": "午餐",
                "category": "午餐",
                "tags": ["高蛋白", "轻食"],
                "limit": 20,
                "offset": 0
            }
        }
    )


class RecipeListResponse(BaseModel):
    """食谱列表响应"""
    total: int = Field(..., description="总数量")
    recipes: List[RecipeResponse] = Field(..., description="食谱列表")


# ========== 通用响应 ==========
class MessageResponse(BaseModel):
    """通用消息响应"""
    message: str
    data: Optional[dict] = None

