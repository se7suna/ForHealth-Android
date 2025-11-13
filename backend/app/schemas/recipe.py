from pydantic import BaseModel, Field, ConfigDict, field_validator
from typing import Optional, List
from datetime import datetime, date
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


class RecipeIdSearchRequest(BaseModel):
    """食谱ID搜索请求（用于快速查找和自动完成）"""
    keyword: str = Field(..., min_length=1, description="搜索关键词（食谱名称）")
    limit: int = Field(default=10, ge=1, le=50, description="返回数量限制")

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "example": {
                "keyword": "早餐",
                "limit": 10
            }
        }
    )


class RecipeIdItem(BaseModel):
    """食谱ID项（用于快速查找和自动完成）"""
    id: str = Field(..., description="食谱ID（本地数据库ObjectId）")
    name: str = Field(..., description="食谱名称")
    category: Optional[str] = Field(None, description="分类")
    created_by: Optional[str] = Field(None, description="创建者（'all'表示公开，用户邮箱表示私有）")

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "id": "64f1f0c2e13e5f7b12345678",
                "name": "健康早餐套餐",
                "category": "早餐",
                "created_by": "all"
            }
        }
    )


class RecipeIdSearchResponse(BaseModel):
    """食谱ID搜索响应"""
    total: int = Field(..., description="匹配的食谱总数")
    recipes: List[RecipeIdItem] = Field(..., description="食谱ID列表（优先显示用户创建的食谱）")

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "total": 2,
                "recipes": [
                    {
                        "id": "64f1f0c2e13e5f7b12345678",
                        "name": "健康早餐套餐",
                        "category": "早餐",
                        "created_by": "user@example.com"
                    },
                    {
                        "id": "64f1f0c2e13e5f7b12345679",
                        "name": "营养早餐组合",
                        "category": "早餐",
                        "created_by": "all"
                    }
                ]
            }
        }
    )


class RecipeListResponse(BaseModel):
    """食谱列表响应"""
    total: int = Field(..., description="总数量")
    recipes: List[RecipeResponse] = Field(..., description="食谱列表")


# ========== 食谱记录 ==========
class RecipeRecordQueryRequest(BaseModel):
    """食谱记录查询请求"""
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
                "limit": 100,
                "offset": 0
            }
        }
    )


class RecipeRecordBatchItem(BaseModel):
    """食谱记录批次项"""
    batch_id: str = Field(..., description="批次ID")
    recipe_name: str = Field(..., description="食谱名称")
    total_records: int = Field(..., description="该批次的记录数量")
    recorded_at: datetime = Field(..., description="记录时间")
    meal_type: Optional[str] = Field(None, description="餐次类型")
    total_nutrition: NutritionData = Field(..., description="该批次的总营养")
    notes: Optional[str] = Field(None, description="备注（不含食谱标记）")


class RecipeRecordListResponse(BaseModel):
    """食谱记录列表响应"""
    total: int = Field(..., description="总批次数")
    batches: List[RecipeRecordBatchItem] = Field(..., description="食谱记录批次列表")
    total_nutrition: NutritionData = Field(..., description="所有批次的总营养摄入")

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "total": 3,
                "batches": [
                    {
                        "batch_id": "64f1f0c2e13e5f7b12345abc",
                        "recipe_name": "健康早餐套餐",
                        "total_records": 3,
                        "recorded_at": "2025-11-13T08:00:00",
                        "meal_type": "早餐",
                        "total_nutrition": {
                            "calories": 493,
                            "protein": 26.6,
                            "carbohydrates": 46.2,
                            "fat": 20,
                            "fiber": 2.4,
                            "sodium": 260
                        },
                        "notes": "按食谱准备的早餐"
                    }
                ],
                "total_nutrition": {
                    "calories": 1479,
                    "protein": 79.8,
                    "carbohydrates": 138.6,
                    "fat": 60,
                    "fiber": 7.2,
                    "sodium": 780
                }
            }
        }
    )


class RecipeRecordCreateRequest(BaseModel):
    """创建食谱记录请求"""
    recipe_id: str = Field(..., min_length=1, description="食谱ID（本地库ObjectId）")
    scale: float = Field(default=1.0, gt=0, description="份量倍数（如：1.0表示1份，0.5表示半份）")
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
                "recipe_id": "64f1f0c2e13e5f7b12345678",
                "scale": 1.0,
                "recorded_at": "2025-11-13T12:30:00",
                "meal_type": "午餐",
                "notes": "按食谱准备的午餐"
            }
        }
    )


class RecipeRecordUpdateRequest(BaseModel):
    """更新食谱记录请求"""
    recorded_at: Optional[datetime] = Field(None, description="新的摄入时间")
    meal_type: Optional[str] = Field(None, description="新的餐次类型")
    notes: Optional[str] = Field(None, max_length=500, description="新的备注（不包括自动添加的食谱标记）")

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
                "recorded_at": "2025-11-13T18:30:00",
                "meal_type": "晚餐",
                "notes": "更新备注"
            }
        }
    )


class RecipeRecordResponse(BaseModel):
    """食谱记录响应"""
    message: str = Field(..., description="操作消息")
    recipe_name: str = Field(..., description="食谱名称")
    batch_id: str = Field(..., description="批次ID（用于查询/更新/删除此次食谱记录）")
    total_records: int = Field(..., description="创建的记录数量")
    record_ids: List[str] = Field(..., description="创建的食物记录ID列表")
    total_nutrition: NutritionData = Field(..., description="总营养数据")

    class Config:
        json_schema_extra = {
            "example": {
                "message": "食谱记录成功",
                "recipe_name": "健康早餐套餐",
                "batch_id": "64f1f0c2e13e5f7b12345abc",
                "total_records": 3,
                "record_ids": ["64f1f0c2e13e5f7b12345678", "64f1f0c2e13e5f7b12345679"],
                "total_nutrition": {
                    "calories": 493,
                    "protein": 26.6,
                    "carbohydrates": 46.2,
                    "fat": 20,
                    "fiber": 2.4,
                    "sodium": 260
                }
            }
        }


class RecipeRecordUpdateResponse(BaseModel):
    """更新食谱记录响应"""
    message: str = Field(..., description="操作消息")
    recipe_name: str = Field(..., description="食谱名称")
    batch_id: str = Field(..., description="批次ID")
    updated_count: int = Field(..., description="更新的记录数量")
    total_nutrition: NutritionData = Field(..., description="更新后的总营养数据")

    class Config:
        json_schema_extra = {
            "example": {
                "message": "食谱记录更新成功",
                "recipe_name": "健康早餐套餐",
                "batch_id": "64f1f0c2e13e5f7b12345abc",
                "updated_count": 3,
                "total_nutrition": {
                    "calories": 739.5,
                    "protein": 39.9,
                    "carbohydrates": 69.3,
                    "fat": 30,
                    "fiber": 3.6,
                    "sodium": 390
                }
            }
        }


# ========== 通用响应 ==========
class MessageResponse(BaseModel):
    """通用消息响应"""
    message: str
    data: Optional[dict] = None

