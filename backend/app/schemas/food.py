from pydantic import BaseModel, Field, field_validator, ConfigDict
from typing import Optional, List, Dict, Any, Union
from datetime import datetime, date
from app.models.food import NutritionData, FullNutritionData, BooheeFoodSearchItem


# ========== 食物管理 ==========
class FoodCreateRequest(BaseModel):
    """创建食物请求"""
    name: str = Field(..., min_length=1, max_length=100, description="食物名称")
    category: Optional[str] = Field(None, max_length=50, description="食物分类")
    serving_size: float = Field(..., gt=0, description="标准份量（克）")
    serving_unit: str = Field(default="克", max_length=20, description="份量单位")
    nutrition_per_serving: NutritionData = Field(..., description="每份基础营养数据")
    full_nutrition: Optional[FullNutritionData] = Field(None, description="完整营养信息（与测试脚本格式一致）")
    brand: Optional[str] = Field(None, max_length=100, description="品牌")
    barcode: Optional[str] = Field(None, max_length=50, description="条形码")
    image_url: Optional[str] = Field(None, description="食物图片URL")

    class Config:
        json_schema_extra = {
            "example": {
                "name": "同济牛排",
                "category": "肉类",
                "serving_size": 100,
                "serving_unit": "克",
                "nutrition_per_serving": {
                    "calories": 250,
                    "protein": 26,
                    "carbohydrates": 0,
                    "fat": 15,
                    "fiber": 0,
                    "sodium": 55
                },
                "full_nutrition": {
                    "calory": [
                        {"name_en": "total_calory", "name": "总热量", "value": 250, "unit_name": "Kcal"},
                        {"name_en": "total_kj", "name": "总热量", "value": 1046, "unit_name": "Kj"},
                        {"name_en": "protein", "name": "蛋白质热量", "value": 104, "unit_name": "Kcal", "percent": 41.6},
                        {"name_en": "fat", "name": "脂肪热量", "value": 135, "unit_name": "Kcal", "percent": 54},
                        {"name_en": "carbohydrate", "name": "碳水化合物热量", "value": 0, "unit_name": "Kcal", "percent": 0}
                    ],
                    "base_ingredients": [
                        {
                            "name_en": "protein",
                            "name": "蛋白质",
                            "value": 26,
                            "unit_name": "g",
                            "items": [
                                {"name_en": "good_protein", "name": "优质蛋白", "value": 26, "unit_name": "g"}
                            ]
                        },
                        {
                            "name_en": "fat",
                            "name": "脂肪",
                            "value": 15,
                            "unit_name": "g",
                            "items": [
                                {"name_en": "saturated_fat", "name": "饱和脂肪", "value": 6, "unit_name": "g"},
                                {"name_en": "fa_mufa", "name": "单不饱和脂肪酸", "value": 7, "unit_name": "g"},
                                {"name_en": "pufa", "name": "多不饱和脂肪酸", "value": 2, "unit_name": "g"}
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
                        {"name": "维生素B12", "value": 2.4, "unit_name": "μg"},
                        {"name": "维生素B6", "value": 0.5, "unit_name": "mg"}
                    ],
                    "mineral": [
                        {"name": "钠", "value": 0.055, "unit_name": "g"},
                        {"name": "磷", "value": 0.22, "unit_name": "g"},
                        {"name": "钾", "value": 0.315, "unit_name": "g"},
                        {"name": "铁", "value": 2.6, "unit_name": "mg"},
                        {"name": "锌", "value": 5.3, "unit_name": "mg"}
                    ],
                    "amino_acid": [
                        {"name": "异亮氨酸", "value": 1200, "unit_name": "mg"},
                        {"name": "亮氨酸", "value": 2100, "unit_name": "mg"},
                        {"name": "赖氨酸", "value": 2200, "unit_name": "mg"}
                    ],
                    "other_ingredients": []
                }
            }
        }


class FoodResponse(BaseModel):
    """食物响应"""
    id: str = Field(..., description="食物ID")
    name: str
    category: Optional[str] = None
    serving_size: float
    serving_unit: str
    nutrition_per_serving: NutritionData
    full_nutrition: Optional[FullNutritionData] = Field(None, description="完整营养信息（与测试脚本格式一致）")
    brand: Optional[str] = None
    barcode: Optional[str] = None
    image_url: Optional[str] = None
    created_by: Optional[str] = None
    created_at: datetime
    source: Optional[str] = Field(None, description="数据来源：local 或 boohee")
    boohee_id: Optional[int] = Field(None, description="薄荷健康食物ID")
    boohee_code: Optional[str] = Field(None, description="薄荷健康食物编码")


class FoodUpdateRequest(BaseModel):
    """更新食物请求"""
    name: Optional[str] = Field(None, min_length=1, max_length=100, description="食物名称")
    category: Optional[str] = Field(None, max_length=50, description="食物分类")
    serving_size: Optional[float] = Field(None, gt=0, description="标准份量（克）")
    serving_unit: Optional[str] = Field(None, max_length=20, description="份量单位")
    nutrition_per_serving: Optional[NutritionData] = Field(None, description="每份基础营养数据")
    full_nutrition: Optional[FullNutritionData] = Field(None, description="完整营养信息（与测试脚本格式一致）")
    brand: Optional[str] = Field(None, max_length=100, description="品牌")
    barcode: Optional[str] = Field(None, max_length=50, description="条形码")
    image_url: Optional[str] = Field(None, description="食物图片URL")

    class Config:
        json_schema_extra = {
            "example": {
                "name": "改良版牛排",
                "nutrition_per_serving": {
                    "calories": 260,
                    "protein": 28,
                    "carbohydrates": 0,
                    "fat": 16,
                    "fiber": 0,
                    "sodium": 60
                },
                "full_nutrition": {
                    "calory": [
                        {"name_en": "total_calory", "name": "总热量", "value": 260, "unit_name": "Kcal"},
                        {"name_en": "total_kj", "name": "总热量", "value": 1088, "unit_name": "Kj"},
                        {"name_en": "protein", "name": "蛋白质热量", "value": 112, "unit_name": "Kcal", "percent": 43.1},
                        {"name_en": "fat", "name": "脂肪热量", "value": 144, "unit_name": "Kcal", "percent": 55.4},
                        {"name_en": "carbohydrate", "name": "碳水化合物热量", "value": 0, "unit_name": "Kcal", "percent": 0}
                    ],
                    "base_ingredients": [
                        {
                            "name_en": "protein",
                            "name": "蛋白质",
                            "value": 28,
                            "unit_name": "g",
                            "items": [
                                {"name_en": "good_protein", "name": "优质蛋白", "value": 28, "unit_name": "g"}
                            ]
                        },
                        {
                            "name_en": "fat",
                            "name": "脂肪",
                            "value": 16,
                            "unit_name": "g",
                            "items": [
                                {"name_en": "saturated_fat", "name": "饱和脂肪", "value": 7, "unit_name": "g"},
                                {"name_en": "fa_mufa", "name": "单不饱和脂肪酸", "value": 7, "unit_name": "g"},
                                {"name_en": "pufa", "name": "多不饱和脂肪酸", "value": 2, "unit_name": "g"}
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
                        {"name": "维生素B12", "value": 2.6, "unit_name": "μg"},
                        {"name": "维生素B6", "value": 0.5, "unit_name": "mg"}
                    ],
                    "mineral": [
                        {"name": "钠", "value": 0.06, "unit_name": "g"},
                        {"name": "磷", "value": 0.24, "unit_name": "g"},
                        {"name": "钾", "value": 0.33, "unit_name": "g"},
                        {"name": "铁", "value": 2.8, "unit_name": "mg"},
                        {"name": "锌", "value": 5.5, "unit_name": "mg"}
                    ],
                    "amino_acid": [
                        {"name": "异亮氨酸", "value": 1250, "unit_name": "mg"},
                        {"name": "亮氨酸", "value": 2200, "unit_name": "mg"},
                        {"name": "赖氨酸", "value": 2300, "unit_name": "mg"}
                    ],
                    "other_ingredients": []
                }
            }
        }


class SimplifiedNutritionData(BaseModel):
    """简化的营养数据（仅主要营养素）"""
    calories: float = Field(..., description="热量（千卡）")
    protein: float = Field(..., description="蛋白质（克）")
    fat: float = Field(..., description="脂肪（克）")
    carbohydrates: float = Field(..., description="碳水化合物（克）")
    sugar: Optional[float] = Field(None, description="糖（克）")
    sodium: Optional[float] = Field(None, description="钠（毫克）")

    class Config:
        json_schema_extra = {
            "example": {
                "calories": 54,
                "protein": 0.3,
                "fat": 0.2,
                "carbohydrates": 13.5,
                "sugar": 10.2,
                "sodium": 1
            }
        }


class FoodSearchItemResponse(BooheeFoodSearchItem):
    """食物搜索结果条目"""
    pass


class SimplifiedFoodSearchItem(BaseModel):
    """简化的食物搜索结果条目（仅显示主要营养信息）"""
    source: str = Field(..., description="数据来源：local 或 boohee")
    food_id: Optional[str] = Field(None, description="本地食物ID（本地食物）")
    boohee_id: Optional[int] = Field(None, description="薄荷健康食物ID（薄荷食物）")
    code: str = Field(..., description="食物编码")
    name: str = Field(..., description="食物名称")
    weight: float = Field(..., description="标准份量")
    weight_unit: str = Field(default="克", description="份量单位")
    brand: Optional[str] = Field(None, description="品牌")
    image_url: Optional[str] = Field(None, description="图片URL")
    nutrition: SimplifiedNutritionData = Field(..., description="简化营养信息（仅主要营养素）")

    class Config:
        json_schema_extra = {
            "example": {
                "source": "boohee",
                "boohee_id": 469,
                "code": "pingguo_junzhi",
                "name": "苹果",
                "weight": 100,
                "weight_unit": "克",
                "brand": None,
                "image_url": "http://s.boohee.cn/house/food_mid/mid_photo_2015126214658469.jpg",
                "nutrition": {
                    "calories": 54,
                    "protein": 0.3,
                    "fat": 0.2,
                    "carbohydrates": 13.5,
                    "sugar": 10.2,
                    "sodium": 1
                }
            }
        }


class FoodSearchRequest(BaseModel):
    """食物搜索请求"""
    keyword: Optional[str] = Field(None, description="搜索关键词，对应薄荷API的 q 参数")
    page: int = Field(default=1, ge=1, le=10, description="页码（薄荷API最多返回前10页）")
    include_full_nutrition: bool = Field(
        default=True,
        description="是否为每个搜索结果请求完整营养信息（调用 /api/v2/foods/ingredients）"
    )
    simplified: bool = Field(
        default=False,
        description="是否返回简化版本（仅主要营养信息：能量、蛋白质、脂肪、碳水化合物、糖、钠）"
    )

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "keyword": "苹果",
                "page": 1,
                "include_full_nutrition": True,
                "simplified": False
            }
        }
    )


class FoodListResponse(BaseModel):
    """薄荷健康数据库搜索结果响应"""
    page: int = Field(..., ge=1, description="当前页码")
    total_pages: int = Field(..., ge=0, description="总页数（最大10）")
    foods: List[FoodSearchItemResponse] = Field(..., description="搜索结果列表")

    class Config:
        json_schema_extra = {
            "example": {
                "page": 1,
                "total_pages": 3,
                "foods": [
                    {
                        "source": "local",
                        "food_id": "64f1f0c2e13e5f7b12345678",
                        "code": "64f1f0c2e13e5f7b12345678",
                        "name": "同济牛排",
                        "weight": 120,
                        "weight_unit": "克",
                        "calory": 260,
                        "brand": "同济食堂",
                        "barcode": "6921234567890",
                        "nutrition_per_serving": {
                            "calories": 260,
                            "protein": 28,
                            "carbohydrates": 0,
                            "fat": 16,
                            "fiber": 0,
                            "sugar": 0,
                            "sodium": 60
                        },
                        "full_nutrition": {
                            "calory": [
                                {"name_en": "total_calory", "name": "总热量", "value": 260, "unit_name": "Kcal"},
                                {"name_en": "total_kj", "name": "总热量", "value": 1088, "unit_name": "Kj"}
                            ],
                            "base_ingredients": [
                                {
                                    "name_en": "protein",
                                    "name": "蛋白质",
                                    "value": 28,
                                    "unit_name": "g",
                                    "items": [
                                        {"name_en": "good_protein", "name": "优质蛋白", "value": 28, "unit_name": "g"}
                                    ]
                                }
                            ],
                            "vitamin": [],
                            "mineral": [],
                            "amino_acid": [],
                            "other_ingredients": []
                        }
                    },
                    {
                        "source": "boohee",
                        "boohee_id": 469,
                        "code": "pingguo_junzhi",
                        "name": "苹果",
                        "weight": 100,
                        "weight_unit": "克",
                        "calory": 54,
                        "image_url": "http://s.boohee.cn/house/food_mid/mid_photo_2015126214658469.jpg",
                        "is_liquid": False,
                        "health_light": 1,
                        "nutrition_per_serving": {
                            "calories": 54,
                            "protein": 0.3,
                            "carbohydrates": 13.5,
                            "fat": 0.2,
                            "fiber": 2.4,
                            "sugar": 10.2,
                            "sodium": 1
                        }
                    }
                ]
            }
        }


class SimplifiedFoodListResponse(BaseModel):
    """简化版食物搜索结果响应（仅主要营养信息）"""
    page: int = Field(..., ge=1, description="当前页码")
    total_pages: int = Field(..., ge=0, description="总页数（最大10）")
    foods: List[SimplifiedFoodSearchItem] = Field(..., description="简化搜索结果列表")

    class Config:
        json_schema_extra = {
            "example": {
                "page": 1,
                "total_pages": 3,
                "foods": [
                    {
                        "source": "local",
                        "food_id": "64f1f0c2e13e5f7b12345678",
                        "code": "64f1f0c2e13e5f7b12345678",
                        "name": "同济牛排",
                        "weight": 100,
                        "weight_unit": "克",
                        "brand": "同济食堂",
                        "image_url": None,
                        "nutrition": {
                            "calories": 260,
                            "protein": 28,
                            "carbohydrates": 0,
                            "fat": 16,
                            "sugar": 0,
                            "sodium": 60
                        }
                    },
                    {
                        "source": "boohee",
                        "boohee_id": 469,
                        "code": "pingguo_junzhi",
                        "name": "苹果",
                        "weight": 100,
                        "weight_unit": "克",
                        "brand": None,
                        "image_url": "http://s.boohee.cn/house/food_mid/mid_photo_2015126214658469.jpg",
                        "nutrition": {
                            "calories": 54,
                            "protein": 0.3,
                            "carbohydrates": 13.5,
                            "fat": 0.2,
                            "sugar": 10.2,
                            "sodium": 1
                        }
                    }
                ]
            }
        }


class FoodNameSearchRequest(BaseModel):
    """食物名称搜索请求（仅返回ID和名称）"""
    keyword: str = Field(..., min_length=1, description="搜索关键词（食物名称）")
    limit: int = Field(default=20, ge=1, le=100, description="返回数量限制")

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "keyword": "苹果",
                "limit": 20
            }
        }
    )


class FoodIdItem(BaseModel):
    """食物ID条目（仅本地数据库）"""
    food_id: str = Field(..., description="食物ID（本地数据库的ObjectId）")
    name: str = Field(..., description="食物名称")
    source: str = Field(..., description="数据来源：固定为 local（本地数据库）")
    brand: Optional[str] = Field(None, description="品牌")
    boohee_id: Optional[int] = Field(None, description="薄荷健康食物ID（如果是缓存的薄荷食物则有此字段）")

    class Config:
        json_schema_extra = {
            "example": {
                "food_id": "64f1f0c2e13e5f7b12345678",
                "name": "苹果",
                "source": "local",
                "brand": "红富士",
                "boohee_id": 469
            }
        }


class FoodIdSearchResponse(BaseModel):
    """食物ID搜索响应（仅本地数据库）"""
    total: int = Field(..., description="总数量")
    foods: List[FoodIdItem] = Field(..., description="食物ID列表（仅来自本地数据库）")

    class Config:
        json_schema_extra = {
            "example": {
                "total": 3,
                "foods": [
                    {
                        "food_id": "64f1f0c2e13e5f7b12345678",
                        "name": "苹果（红富士）",
                        "source": "local",
                        "brand": "红富士",
                        "boohee_id": None
                    },
                    {
                        "food_id": "64f1f0c2e13e5f7b12345679",
                        "name": "苹果",
                        "source": "local",
                        "brand": None,
                        "boohee_id": 469
                    },
                    {
                        "food_id": "64f1f0c2e13e5f7b87654321",
                        "name": "苹果汁",
                        "source": "local",
                        "brand": None,
                        "boohee_id": None
                    }
                ]
            }
        }


# ========== 食物记录 ==========
class FoodRecordCreateRequest(BaseModel):
    """创建食物记录请求"""
    food_id: str = Field(..., min_length=1, description="食物ID（本地库ObjectId）")
    source: Optional[str] = Field(
        default="auto",
        pattern="^(?i)(local|auto)$",
        description="数据来源：local（仅本地食物库）或 auto（默认）",
    )
    serving_amount: float = Field(..., gt=0, description="食用份量数（如：1.5份）")
    recorded_at: datetime = Field(..., description="摄入时间")
    meal_type: Optional[str] = Field(None, description="餐次类型")
    notes: Optional[str] = Field(None, max_length=500, description="备注")

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "food_id": "64f1f0c2e13e5f7b12345678",
                "source": "auto",
                "serving_amount": 1.5,
                "recorded_at": "2025-11-03T12:30:00",
                "meal_type": "午餐",
                "notes": "训练前补给"
            }
        },
    )

    @field_validator("meal_type")
    @classmethod
    def validate_meal_type(cls, v):
        if v and v not in ["早餐", "午餐", "晚餐", "加餐", "breakfast", "lunch", "dinner", "snack"]:
            raise ValueError("餐次类型必须是：早餐、午餐、晚餐、加餐 之一")
        return v


class FoodRecordResponse(BaseModel):
    """食物记录响应"""
    id: str = Field(..., description="记录ID")
    user_email: str
    food_name: str
    serving_amount: float
    serving_size: float
    serving_unit: str
    nutrition_data: NutritionData
    full_nutrition: Optional[FullNutritionData] = Field(None, description="完整营养信息（与测试脚本格式一致）")
    recorded_at: datetime = Field(..., description="摄入时间")
    meal_type: Optional[str] = None
    notes: Optional[str] = None
    food_id: Optional[str] = None
    created_at: datetime = Field(..., description="记录时间")


class FoodRecordUpdateRequest(BaseModel):
    """更新食物记录请求"""
    food_name: Optional[str] = Field(None, min_length=1, max_length=100, description="食物名称")
    serving_amount: Optional[float] = Field(None, gt=0, description="食用份量数")
    serving_size: Optional[float] = Field(None, gt=0, description="每份大小（克）")
    serving_unit: Optional[str] = Field(None, description="份量单位")
    nutrition_data: Optional[NutritionData] = Field(None, description="基础营养数据")
    full_nutrition: Optional[FullNutritionData] = Field(None, description="完整营养信息（与测试脚本格式一致）")
    recorded_at: Optional[datetime] = Field(None, description="记录时间")
    meal_type: Optional[str] = Field(None, description="餐次类型")
    notes: Optional[str] = Field(None, max_length=500, description="备注")

    @field_validator("meal_type")
    @classmethod
    def validate_meal_type(cls, v):
        if v and v not in ["早餐", "午餐", "晚餐", "加餐", "breakfast", "lunch", "dinner", "snack"]:
            raise ValueError("餐次类型必须是：早餐、午餐、晚餐、加餐 之一")
        return v

    class Config:
        json_schema_extra = {
            "example": {
                "food_name": "牛油果三明治",
                "serving_amount": 1.5,
                "serving_size": 120,
                "serving_unit": "克",
                "nutrition_data": {
                    "calories": 342,
                    "protein": 12.3,
                    "carbohydrates": 28.6,
                    "fat": 20.5,
                    "fiber": 6.4,
                    "sugar": 3.1,
                    "sodium": 310
                },
                "full_nutrition": {
                    "calory": [
                        {"name_en": "total_calory", "name": "总热量", "value": 342, "unit_name": "Kcal"},
                        {"name_en": "total_kj", "name": "总热量", "value": 1430, "unit_name": "Kj"},
                        {"name_en": "protein", "name": "蛋白质热量", "value": 49.2, "unit_name": "Kcal", "percent": 14.4},
                        {"name_en": "fat", "name": "脂肪热量", "value": 184.5, "unit_name": "Kcal", "percent": 53.9},
                        {"name_en": "carbohydrate", "name": "碳水化合物热量", "value": 108.4, "unit_name": "Kcal", "percent": 31.7}
                    ],
                    "base_ingredients": [
                        {
                            "name_en": "protein",
                            "name": "蛋白质",
                            "value": 12.3,
                            "unit_name": "g",
                            "items": [
                                {"name_en": "good_protein", "name": "优质蛋白", "value": 12.3, "unit_name": "g"}
                            ]
                        },
                        {
                            "name_en": "fat",
                            "name": "脂肪",
                            "value": 20.5,
                            "unit_name": "g",
                            "items": [
                                {"name_en": "saturated_fat", "name": "饱和脂肪", "value": 4.1, "unit_name": "g"},
                                {"name_en": "fa_mufa", "name": "单不饱和脂肪酸", "value": 11.8, "unit_name": "g"},
                                {"name_en": "pufa", "name": "多不饱和脂肪酸", "value": 2.9, "unit_name": "g"}
                            ]
                        },
                        {
                            "name_en": "carbohydrate",
                            "name": "碳水化合物",
                            "value": 28.6,
                            "unit_name": "g",
                            "items": []
                        }
                    ],
                    "vitamin": [
                        {"name": "维生素E", "value": 3.4, "unit_name": "mg"},
                        {"name": "叶酸", "value": 64.2, "unit_name": "μg"}
                    ],
                    "mineral": [
                        {"name": "钠", "value": 0.31, "unit_name": "g"},
                        {"name": "钾", "value": 0.42, "unit_name": "g"},
                        {"name": "镁", "value": 52.8, "unit_name": "mg"},
                        {"name": "铁", "value": 2.3, "unit_name": "mg"},
                        {"name": "锌", "value": 1.5, "unit_name": "mg"}
                    ],
                    "amino_acid": [
                        {"name": "异亮氨酸", "value": 720, "unit_name": "mg"},
                        {"name": "亮氨酸", "value": 1280, "unit_name": "mg"},
                        {"name": "赖氨酸", "value": 1340, "unit_name": "mg"}
                    ],
                    "other_ingredients": []
                },
                "recorded_at": "2025-11-07T08:15:00",
                "meal_type": "早餐",
                "notes": "加了生菜和柠檬汁"
            }
        }


class FoodRecordQueryRequest(BaseModel):
    """食物记录查询请求"""
    start_date: Optional[date] = Field(None, description="开始日期（YYYY-MM-DD）")
    end_date: Optional[date] = Field(None, description="结束日期（YYYY-MM-DD）")
    meal_type: Optional[str] = Field(None, description="餐次类型")
    limit: int = Field(default=100, ge=1, le=500, description="返回数量限制")
    offset: int = Field(default=0, ge=0, description="偏移量")

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
                "start_date": "2025-11-01",
                "end_date": "2025-11-13",
                "meal_type": "午餐",
                "limit": 50,
                "offset": 0
            }
        }
    )


class FoodRecordListResponse(BaseModel):
    """食物记录列表响应"""
    total: int = Field(..., description="总记录数")
    records: List[FoodRecordResponse] = Field(..., description="食物记录列表")
    total_nutrition: NutritionData = Field(..., description="总营养摄入")


class DailyNutritionSummary(BaseModel):
    """每日营养摘要"""
    date: str = Field(..., description="日期（YYYY-MM-DD）")
    total_calories: float = Field(..., description="总卡路里")
    total_protein: float = Field(..., description="总蛋白质（克）")
    total_carbohydrates: float = Field(..., description="总碳水化合物（克）")
    total_fat: float = Field(..., description="总脂肪（克）")
    meal_count: int = Field(..., description="进食次数")
    records: List[FoodRecordResponse] = Field(..., description="当天的所有记录")


# ========== 搜索和查询 ==========
class FoodSearchQuery(BaseModel):
    """食物搜索查询参数"""
    keyword: Optional[str] = Field(None, description="关键词（搜索名称、品牌）")
    category: Optional[str] = Field(None, description="分类筛选")
    limit: int = Field(default=20, ge=1, le=100, description="返回数量限制")
    offset: int = Field(default=0, ge=0, description="偏移量")


# ========== 通用响应 ==========
class MessageResponse(BaseModel):
    """通用消息响应"""
    message: str
    data: Optional[dict] = None


# ========== 条形码扫描 ==========
class BarcodeScanResponse(BaseModel):
    """条形码扫描响应"""
    found: bool = Field(..., description="是否找到食品信息")
    message: str = Field(..., description="响应消息")
    food_data: Optional[dict] = Field(None, description="食品信息（如果找到）")
    
    class Config:
        json_schema_extra = {
            "example": {
                "found": True,
                "message": "成功找到食品信息",
                "food_data": {
                    "name": "红烧牛肉面",
                    "brand": "康师傅",
                    "category": "方便食品",
                    "serving_size": 100,
                    "serving_unit": "克",
                    "nutrition_per_serving": {
                        "calories": 473,
                        "protein": 9.8,
                        "carbohydrates": 62.3,
                        "fat": 20.1
                    },
                    "full_nutrition": {
                        "calory": [
                            {"name_en": "total_calory", "name": "总热量", "value": 473, "unit_name": "Kcal"},
                            {"name_en": "total_kj", "name": "总热量", "value": 1978, "unit_name": "Kj"},
                            {"name_en": "protein", "name": "蛋白质热量", "value": 39.2, "unit_name": "Kcal", "percent": 8.3},
                            {"name_en": "fat", "name": "脂肪热量", "value": 180.9, "unit_name": "Kcal", "percent": 38.2},
                            {"name_en": "carbohydrate", "name": "碳水化合物热量", "value": 249.2, "unit_name": "Kcal", "percent": 52.7}
                        ],
                        "base_ingredients": [
                            {
                                "name_en": "carbohydrate",
                                "name": "碳水化合物",
                                "value": 62.3,
                                "unit_name": "g",
                                "items": [
                                    {"name_en": "sugar", "name": "糖", "value": 3.5, "unit_name": "g"}
                                ]
                            },
                            {
                                "name_en": "protein",
                                "name": "蛋白质",
                                "value": 9.8,
                                "unit_name": "g",
                                "items": []
                            },
                            {
                                "name_en": "fat",
                                "name": "脂肪",
                                "value": 20.1,
                                "unit_name": "g",
                                "items": [
                                    {"name_en": "saturated_fat", "name": "饱和脂肪", "value": 9.2, "unit_name": "g"}
                                ]
                            }
                        ],
                        "vitamin": [],
                        "mineral": [
                            {"name": "钠", "value": 2.3, "unit_name": "g"}
                        ],
                        "amino_acid": [],
                        "other_ingredients": []
                    },
                    "barcode": "6901939613702"
                }
            }
        }

