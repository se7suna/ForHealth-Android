from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, Field
from app.models.food import NutritionData, FullNutritionData


class RecipeFoodItem(BaseModel):
    """食谱中的食物项"""
    food_id: str = Field(..., description="食物ID")
    food_name: str = Field(..., description="食物名称")
    serving_amount: float = Field(..., gt=0, description="份量数")
    serving_size: float = Field(..., gt=0, description="每份大小")
    serving_unit: str = Field(default="克", description="份量单位")
    nutrition: NutritionData = Field(..., description="该食物在此食谱中的基础营养数据")
    full_nutrition: Optional[FullNutritionData] = Field(None, description="该食物在此食谱中的完整营养信息（与测试脚本格式一致）")

    class Config:
        json_schema_extra = {
            "example": {
                "food_id": "507f1f77bcf86cd799439011",
                "food_name": "鸡蛋",
                "serving_amount": 2,
                "serving_size": 50,
                "serving_unit": "克",
                "nutrition": {
                    "calories": 148,
                    "protein": 12.6,
                    "carbohydrates": 1.2,
                    "fat": 10,
                    "fiber": 0,
                    "sodium": 120
                },
                "full_nutrition": {
                    "calory": [
                        {"name_en": "total_calory", "name": "总热量", "value": 148, "unit_name": "Kcal"},
                        {"name_en": "total_kj", "name": "总热量", "value": 619, "unit_name": "Kj"},
                        {"name_en": "protein", "name": "蛋白质热量", "value": 50.4, "unit_name": "Kcal", "percent": 34.1},
                        {"name_en": "fat", "name": "脂肪热量", "value": 90, "unit_name": "Kcal", "percent": 60.8},
                        {"name_en": "carbohydrate", "name": "碳水化合物热量", "value": 4.8, "unit_name": "Kcal", "percent": 3.2}
                    ],
                    "base_ingredients": [
                        {
                            "name_en": "protein",
                            "name": "蛋白质",
                            "value": 12.6,
                            "unit_name": "g",
                            "items": [
                                {"name_en": "good_protein", "name": "优质蛋白", "value": 12.6, "unit_name": "g"}
                            ]
                        },
                        {
                            "name_en": "fat",
                            "name": "脂肪",
                            "value": 10,
                            "unit_name": "g",
                            "items": [
                                {"name_en": "saturated_fat", "name": "饱和脂肪", "value": 3, "unit_name": "g"},
                                {"name_en": "fa_mufa", "name": "单不饱和脂肪酸", "value": 4, "unit_name": "g"},
                                {"name_en": "pufa", "name": "多不饱和脂肪酸", "value": 3, "unit_name": "g"}
                            ]
                        },
                        {
                            "name_en": "carbohydrate",
                            "name": "碳水化合物",
                            "value": 1.2,
                            "unit_name": "g",
                            "items": []
                        }
                    ],
                    "vitamin": [
                        {"name": "维生素B12", "value": 0.6, "unit_name": "μg"},
                        {"name": "维生素B6", "value": 0.1, "unit_name": "mg"}
                    ],
                    "mineral": [
                        {"name": "钠", "value": 0.12, "unit_name": "g"},
                        {"name": "磷", "value": 0.1, "unit_name": "g"},
                        {"name": "钾", "value": 0.07, "unit_name": "g"},
                        {"name": "铁", "value": 0.9, "unit_name": "mg"},
                        {"name": "锌", "value": 0.6, "unit_name": "mg"}
                    ],
                    "amino_acid": [
                        {"name": "异亮氨酸", "value": 600, "unit_name": "mg"},
                        {"name": "亮氨酸", "value": 1050, "unit_name": "mg"},
                        {"name": "赖氨酸", "value": 1100, "unit_name": "mg"}
                    ],
                    "other_ingredients": []
                }
            }
        }


class RecipeInDB(BaseModel):
    """数据库中的食谱模型"""
    name: str = Field(..., min_length=1, max_length=100, description="食谱名称")
    description: Optional[str] = Field(None, max_length=500, description="食谱描述")
    category: Optional[str] = Field(None, max_length=50, description="分类（早餐、午餐、晚餐等）")
    
    # 食谱包含的食物列表
    foods: List[RecipeFoodItem] = Field(..., min_items=1, description="食物列表（至少1个）")
    
    # 总营养（自动计算所有食物的营养总和）
    total_nutrition: NutritionData = Field(..., description="总基础营养数据")
    
    # 完整营养信息（可选，与测试脚本格式一致）
    total_full_nutrition: Optional[FullNutritionData] = Field(None, description="总完整营养信息（与测试脚本格式一致）")
    
    # 可选信息
    tags: Optional[List[str]] = Field(None, description="标签（如：健康、快手、高蛋白）")
    image_url: Optional[str] = Field(None, description="食谱图片URL")
    prep_time: Optional[int] = Field(None, gt=0, description="准备时间（分钟）")
    
    # 权限和元数据
    created_by: Optional[str] = Field(None, description="创建者邮箱（系统食谱为None）")
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

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
                            "sugar": 5.1,
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
                "total_nutrition": {
                    "calories": 436,
                    "protein": 45.9,
                    "carbohydrates": 32.4,
                    "fat": 12.9,
                    "fiber": 6.8,
                    "sugar": 5.1,
                    "sodium": 305
                },
                "total_full_nutrition": {
                    "calory": [
                        {"name_en": "total_calory", "name": "总热量", "value": 198, "unit_name": "Kcal"},
                        {"name_en": "total_kj", "name": "总热量", "value": 828, "unit_name": "Kj"},
                        {"name_en": "protein", "name": "蛋白质热量", "value": 149.2, "unit_name": "Kcal", "percent": 75.4},
                        {"name_en": "fat", "name": "脂肪热量", "value": 40.5, "unit_name": "Kcal", "percent": 20.5},
                        {"name_en": "carbohydrate", "name": "碳水化合物热量", "value": 0, "unit_name": "Kcal", "percent": 0},
                        {"name_en": "total_calory", "name": "总热量", "value": 238, "unit_name": "Kcal"},
                        {"name_en": "total_kj", "name": "总热量", "value": 996, "unit_name": "Kj"},
                        {"name_en": "protein", "name": "蛋白质热量", "value": 34.4, "unit_name": "Kcal", "percent": 14.5},
                        {"name_en": "fat", "name": "脂肪热量", "value": 75.6, "unit_name": "Kcal", "percent": 31.8},
                        {"name_en": "carbohydrate", "name": "碳水化合物热量", "value": 128.0, "unit_name": "Kcal", "percent": 53.7}
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
                        },
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
                                {"name_en": "fa_mufa", "name": "单不饱和脂肪酸", 
"value": 4.3, "unit_name": "g"},
                                {"name_en": "pufa", "name": "多不饱和脂肪酸", "value": 2.6, "unit_name": "g"}
                            ]
                        }
                    ],
                    "vitamin": [
                        {"name": "维生素B6", "value": 0.8, "unit_name": "mg"},
                        {"name": "烟酸", "value": 18.2, "unit_name": "mg"},
                        {"name": "维生素C", "value": 42, "unit_name": "mg"},
                        {"name": "叶酸", "value": 95, "unit_name": "μg"}
                    ],
                    "mineral": [
                        {"name": "钠", "value": 0.095, "unit_name": "g"},
                        {"name": "钾", "value": 0.42, "unit_name": "g"},
                        {"name": "磷", "value": 0.31, "unit_name": "g"},
                        {"name": "镁", "value": 42, "unit_name": "mg"},
                        {"name": "钠", "value": 0.21, "unit_name": "g"},
                        {"name": "钙", "value": 92, "unit_name": "mg"},
                        {"name": "钾", "value": 0.54, "unit_name": "g"},
                        {"name": "镁", "value": 68, "unit_name": "mg"}
                    ],
                    "amino_acid": [
                        {"name": "异亮氨酸", "value": 2200, "unit_name": "mg"},
                        {"name": "亮氨酸", "value": 3600, "unit_name": "mg"},
                        {"name": "赖氨酸", "value": 3800, "unit_name": "mg"},
                        {"name": "异亮氨酸", "value": 350, "unit_name": "mg"},
                        {"name": "亮氨酸", "value": 620, "unit_name": "mg"},
                        {"name": "赖氨酸", "value": 480, "unit_name": "mg"}
                    ],
                    "other_ingredients": [
                        {"name": "嘌呤", "value": 22, "unit_name": "mg"}
                    ]
                },
                "tags": ["午餐", "高蛋白", "轻食"]
            }
        }

