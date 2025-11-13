from datetime import datetime
from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field


class NutritionData(BaseModel):
    """基础营养数据（用于兼容性）"""
    calories: float = Field(..., ge=0, description="卡路里（千卡）")
    protein: float = Field(..., ge=0, description="蛋白质（克）")
    carbohydrates: float = Field(..., ge=0, description="碳水化合物（克）")
    fat: float = Field(..., ge=0, description="脂肪（克）")
    fiber: Optional[float] = Field(None, ge=0, description="膳食纤维（克）")
    sugar: Optional[float] = Field(None, ge=0, description="糖分（克）")
    sodium: Optional[float] = Field(None, ge=0, description="钠（毫克）")

    class Config:
        json_schema_extra = {
            "example": {
                "calories": 52,
                "protein": 0.3,
                "carbohydrates": 14,
                "fat": 0.2,
                "fiber": 2.4,
                "sugar": 10,
                "sodium": 1
            }
        }


class CaloryItem(BaseModel):
    """热量信息项"""
    name_en: Optional[str] = Field(None, description="英文名称")
    name: Optional[str] = Field(None, description="中文名称")
    value: float = Field(..., ge=0, description="数值")
    unit_name: str = Field(..., description="单位")
    percent: Optional[float] = Field(None, description="百分比")


class BaseIngredientItem(BaseModel):
    """三大营养素项"""
    name_en: Optional[str] = Field(None, description="英文名称")
    name: Optional[str] = Field(None, description="中文名称")
    value: float = Field(..., ge=0, description="数值")
    unit_name: str = Field(..., description="单位")
    items: Optional[List[Dict[str, Any]]] = Field(None, description="子项列表")


class VitaminItem(BaseModel):
    """维生素项"""
    name: str = Field(..., description="名称")
    value: float = Field(..., ge=0, description="数值")
    unit_name: str = Field(..., description="单位")


class MineralItem(BaseModel):
    """矿物质项"""
    name: str = Field(..., description="名称")
    value: float = Field(..., ge=0, description="数值")
    unit_name: str = Field(..., description="单位")


class AminoAcidItem(BaseModel):
    """氨基酸项"""
    name: str = Field(..., description="名称")
    value: float = Field(..., ge=0, description="数值")
    unit_name: str = Field(..., description="单位")


class OtherIngredientItem(BaseModel):
    """其它成分项"""
    name: str = Field(..., description="名称")
    value: float = Field(..., ge=0, description="数值")
    unit_name: str = Field(..., description="单位")


class FullNutritionData(BaseModel):
    """
    完整营养信息数据（与测试脚本格式一致）
    
    格式与 test_boohee_api_standalone.py 中的 print_nutrition_info 函数处理的格式完全一致
    """
    calory: Optional[List[Dict[str, Any]]] = Field(None, description="热量信息数组")
    base_ingredients: Optional[List[Dict[str, Any]]] = Field(None, description="三大营养素数组")
    vitamin: Optional[List[Dict[str, Any]]] = Field(None, description="维生素数组")
    mineral: Optional[List[Dict[str, Any]]] = Field(None, description="矿物质数组")
    amino_acid: Optional[List[Dict[str, Any]]] = Field(None, description="氨基酸数组")
    other_ingredients: Optional[List[Dict[str, Any]]] = Field(None, description="其它成分数组")

    class Config:
        json_schema_extra = {
            "example": {
                "calory": [
                    {"name_en": "total_calory", "name": "总热量", "value": 1000, "unit_name": "Kcal"},
                    {"name_en": "total_kj", "name": "总热量", "value": 4180, "unit_name": "Kj"},
                    {"name_en": "protein", "name": "蛋白质热量", "value": 30, "unit_name": "Kcal", "percent": 30},
                    {"name_en": "fat", "name": "脂肪热量", "value": 30, "unit_name": "Kcal", "percent": 30},
                    {"name_en": "carbohydrate", "name": "碳水化合物热量", "value": 30, "unit_name": "Kcal", "percent": 40}
                ],
                "base_ingredients": [
                    {
                        "name_en": "carbohydrate",
                        "name": "碳水化合物",
                        "value": 30,
                        "unit_name": "g",
                        "items": [
                            {"name_en": "sugar", "name": "糖", "value": 30, "unit_name": "g"},
                            {"name_en": "fiber_dietary", "name": "膳食纤维", "value": 30, "unit_name": "g"}
                        ]
                    },
                    {
                        "name_en": "protein",
                        "name": "蛋白质",
                        "value": 30,
                        "unit_name": "g",
                        "items": [
                            {"name_en": "good_protein", "name": "优质蛋白", "value": 30, "unit_name": "g"}
                        ]
                    },
                    {
                        "name_en": "fat",
                        "name": "脂肪",
                        "value": 30,
                        "unit_name": "g",
                        "items": [
                            {"name_en": "saturated_fat", "name": "饱和脂肪", "value": 30, "unit_name": "g"},
                            {"name_en": "fatty_acid", "name": "反式脂肪", "value": 30, "unit_name": "g"},
                            {"name_en": "fa_mufa", "name": "单不饱和脂肪酸", "value": 30, "unit_name": "g"},
                            {"name_en": "pufa", "name": "多不饱和脂肪酸", "value": 30, "unit_name": "g"},
                            {"name_en": "cholesterol", "name": "胆固醇", "value": 30, "unit_name": "g"}
                        ]
                    }
                ],
                "vitamin": [
                    {"name": "维生素A", "value": 30, "unit_name": "μg"},
                    {"name": "维生素E", "value": 30, "unit_name": "μg"},
                    {"name": "维生素B2", "value": 30, "unit_name": "μg"},
                    {"name": "胡萝卜素", "value": 30, "unit_name": "μg"},
                    {"name": "维生素K", "value": 30, "unit_name": "μg"},
                    {"name": "维生素B6", "value": 30, "unit_name": "μg"},
                    {"name": "维生素D", "value": 30, "unit_name": "μg"},
                    {"name": "维生素B1", "value": 30, "unit_name": "μg"},
                    {"name": "维生素B12", "value": 30, "unit_name": "μg"},
                    {"name": "维生素C", "value": 30, "unit_name": "μg"},
                    {"name": "烟酸", "value": 30, "unit_name": "μg"},
                    {"name": "叶酸", "value": 30, "unit_name": "μg"},
                    {"name": "泛酸", "value": 30, "unit_name": "μg"},
                    {"name": "生物素", "value": 30, "unit_name": "μg"},
                    {"name": "胆碱", "value": 30, "unit_name": "μg"}
                ],
                "mineral": [
                    {"name": "钠", "value": 30, "unit_name": "g"},
                    {"name": "磷", "value": 30, "unit_name": "g"},
                    {"name": "钾", "value": 30, "unit_name": "g"},
                    {"name": "铁", "value": 30, "unit_name": "g"},
                    {"name": "锌", "value": 30, "unit_name": "g"},
                    {"name": "镁", "value": 30, "unit_name": "g"},
                    {"name": "钙", "value": 30, "unit_name": "g"},
                    {"name": "碘", "value": 30, "unit_name": "g"},
                    {"name": "硒", "value": 30, "unit_name": "g"},
                    {"name": "铜", "value": 30, "unit_name": "g"},
                    {"name": "氟", "value": 30, "unit_name": "g"},
                    {"name": "猛", "value": 30, "unit_name": "g"},
                    {"name": "铬", "value": 30, "unit_name": "g"},
                    {"name": "汞", "value": 30, "unit_name": "g"}
                ],
                "amino_acid": [
                    {"name": "异亮氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "亮氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "赖氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "蛋氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "胱氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "苯丙氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "络氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "苏氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "色氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "缬氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "精氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "组氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "丙氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "天冬氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "谷氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "甘氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "脯氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "丝氨酸", "value": 30, "unit_name": "mg"},
                    {"name": "含硫氨基酸", "value": 30, "unit_name": "mg"},
                    {"name": "芳香族氨基酸", "value": 30, "unit_name": "mg"}
                ],
                "other_ingredients": [
                    {"name": "嘌呤", "value": 30, "unit_name": "mg"},
                    {"name": "酒精度", "value": 30, "unit_name": "mg"},
                    {"name": "叶黄素+玉米黄素", "value": 30, "unit_name": "mg"},
                    {"name": "甜菜碱", "value": 30, "unit_name": "mg"}
                ]
            }
        }


class BooheeFoodSearchItem(BaseModel):
    """食物搜索结果模型（可来自本地或薄荷健康）"""
    source: str = Field(default="boohee", description="数据来源：boohee 或 local")
    boohee_id: Optional[int] = Field(None, description="薄荷健康平台食物ID（source=boohee时可用）")
    food_id: Optional[str] = Field(None, description="本地食物ID（source=local时可用）")
    boohee_code: Optional[str] = Field(None, description="薄荷健康食物编码")
    code: str = Field(..., description="食物编码（薄荷code或本地ID）")
    name: str = Field(..., description="食物名称")
    weight: float = Field(..., ge=0, description="参考重量（克）")
    weight_unit: str = Field(default="克", description="重量单位")
    calory: float = Field(..., ge=0, description="参考卡路里（千卡）")
    image_url: Optional[str] = Field(None, description="缩略图URL")
    is_liquid: Optional[bool] = Field(None, description="是否为液体")
    health_light: Optional[int] = Field(None, description="健康灯等级：0无灯 1绿灯 2黄灯 3红灯")
    brand: Optional[str] = Field(None, description="品牌信息")
    barcode: Optional[str] = Field(None, description="条形码")
    nutrition_per_serving: NutritionData = Field(..., description="每份基础营养数据")
    full_nutrition: Optional[FullNutritionData] = Field(None, description="完整营养信息（与测试脚本格式一致）")

    class Config:
        json_schema_extra = {
            "example": {
                "boohee_id": 469,
                "boohee_code": "pingguo_junzhi",
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
                },
                "full_nutrition": {
                    "calory": [
                        {"name_en": "total_calory", "name": "总热量", "value": 54, "unit_name": "Kcal"},
                        {"name_en": "total_kj", "name": "总热量", "value": 226.02, "unit_name": "Kj"}
                    ],
                    "base_ingredients": [
                        {
                            "name_en": "carbohydrate",
                            "name": "碳水化合物",
                            "value": 13.5,
                            "unit_name": "g",
                            "items": [
                                {"name_en": "sugar", "name": "糖", "value": 10.2, "unit_name": "g"},
                                {"name_en": "fiber_dietary", "name": "膳食纤维", "value": 2.4, "unit_name": "g"}
                            ]
                        },
                        {
                            "name_en": "protein",
                            "name": "蛋白质",
                            "value": 0.3,
                            "unit_name": "g",
                            "items": []
                        },
                        {
                            "name_en": "fat",
                            "name": "脂肪",
                            "value": 0.2,
                            "unit_name": "g",
                            "items": []
                        }
                    ],
                    "vitamin": [],
                    "mineral": [],
                    "amino_acid": [],
                    "other_ingredients": []
                }
            }
        }


class FoodInDB(BaseModel):
    """数据库中的食物模型"""
    name: str = Field(..., min_length=1, max_length=100, description="食物名称")
    category: Optional[str] = Field(None, max_length=50, description="食物分类（如：水果、蔬菜、肉类等）")
    serving_size: float = Field(..., gt=0, description="标准份量（克）")
    serving_unit: str = Field(default="克", max_length=20, description="份量单位")
    nutrition_per_serving: NutritionData = Field(..., description="每份基础营养数据")
    
    # 完整营养信息（与测试脚本格式一致，可选）
    full_nutrition: Optional[FullNutritionData] = Field(None, description="完整营养信息（与测试脚本格式一致）")
    
    # 可选的额外信息
    brand: Optional[str] = Field(None, max_length=100, description="品牌")
    barcode: Optional[str] = Field(None, max_length=50, description="条形码")
    image_url: Optional[str] = Field(None, description="食物图片URL")
    
    # 元数据
    created_by: Optional[str] = Field(None, description="创建者邮箱（'all'表示所有人可见，用户邮箱表示仅创建者可见）")
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

    class Config:
        json_schema_extra = {
            "example": {
                "name": "苹果",
                "category": "水果",
                "serving_size": 100,
                "serving_unit": "克",
                "nutrition_per_serving": {
                    "calories": 52,
                    "protein": 0.3,
                    "carbohydrates": 14,
                    "fat": 0.2,
                    "fiber": 2.4,
                    "sugar": 10,
                    "sodium": 1
                },
                "full_nutrition": {
                    "calory": [
                        {"name_en": "total_calory", "name": "总热量", "value": 52, "unit_name": "Kcal"},
                        {"name_en": "total_kj", "name": "总热量", "value": 218, "unit_name": "Kj"},
                        {"name_en": "protein", "name": "蛋白质热量", "value": 1.2, "unit_name": "Kcal", "percent": 2.3},
                        {"name_en": "fat", "name": "脂肪热量", "value": 1.8, "unit_name": "Kcal", "percent": 3.5},
                        {"name_en": "carbohydrate", "name": "碳水化合物热量", "value": 56, "unit_name": "Kcal", "percent": 107.7}
                    ],
                    "base_ingredients": [
                        {
                            "name_en": "carbohydrate",
                            "name": "碳水化合物",
                            "value": 14,
                            "unit_name": "g",
                            "items": [
                                {"name_en": "sugar", "name": "糖", "value": 10, "unit_name": "g"},
                                {"name_en": "fiber_dietary", "name": "膳食纤维", "value": 2.4, "unit_name": "g"}
                            ]
                        },
                        {
                            "name_en": "protein",
                            "name": "蛋白质",
                            "value": 0.3,
                            "unit_name": "g",
                            "items": []
                        },
                        {
                            "name_en": "fat",
                            "name": "脂肪",
                            "value": 0.2,
                            "unit_name": "g",
                            "items": []
                        }
                    ],
                    "vitamin": [
                        {"name": "维生素C", "value": 4, "unit_name": "mg"},
                        {"name": "维生素K", "value": 2.2, "unit_name": "μg"}
                    ],
                    "mineral": [
                        {"name": "钠", "value": 0.001, "unit_name": "g"},
                        {"name": "钾", "value": 0.119, "unit_name": "g"},
                        {"name": "磷", "value": 0.012, "unit_name": "g"}
                    ],
                    "amino_acid": [],
                    "other_ingredients": []
                }
            }
        }


class FoodRecordInDB(BaseModel):
    """数据库中的食物记录模型"""
    user_email: str = Field(..., description="用户邮箱")
    food_name: str = Field(..., min_length=1, max_length=100, description="食物名称")
    serving_amount: float = Field(..., gt=0, description="食用份量数")
    serving_size: float = Field(..., gt=0, description="每份大小（克）")
    serving_unit: str = Field(default="克", description="份量单位")
    
    # 营养数据（记录时的快照，避免食物信息变更影响历史记录）
    nutrition_data: NutritionData = Field(..., description="本次摄入的基础营养数据")
    full_nutrition: Optional[FullNutritionData] = Field(None, description="本次摄入的完整营养信息（与测试脚本格式一致）")
    
    # 时间字段
    recorded_at: datetime = Field(..., description="摄入时间")
    meal_type: Optional[str] = Field(None, description="餐次类型（早餐、午餐、晚餐、加餐）")
    notes: Optional[str] = Field(None, max_length=500, description="备注")
    
    # 元数据
    food_id: Optional[str] = Field(None, description="关联的食物ID（如果从食物库选择）")
    created_at: datetime = Field(default_factory=datetime.utcnow, description="记录时间")

    class Config:
        json_schema_extra = {
            "example": {
                "user_email": "user@example.com",
                "food_name": "苹果",
                "serving_amount": 1.5,
                "serving_size": 100,
                "serving_unit": "克",
                "nutrition_data": {
                    "calories": 78,
                    "protein": 0.45,
                    "carbohydrates": 21,
                    "fat": 0.3,
                    "fiber": 3.6,
                    "sugar": 15,
                    "sodium": 1.5
                },
                "full_nutrition": {
                    "calory": [
                        {"name_en": "total_calory", "name": "总热量", "value": 78, "unit_name": "Kcal"},
                        {"name_en": "total_kj", "name": "总热量", "value": 327, "unit_name": "Kj"},
                        {"name_en": "protein", "name": "蛋白质热量", "value": 1.8, "unit_name": "Kcal", "percent": 2.3},
                        {"name_en": "fat", "name": "脂肪热量", "value": 2.7, "unit_name": "Kcal", "percent": 3.5},
                        {"name_en": "carbohydrate", "name": "碳水化合物热量", "value": 84, "unit_name": "Kcal", "percent": 107.7}
                    ],
                    "base_ingredients": [
                        {
                            "name_en": "carbohydrate",
                            "name": "碳水化合物",
                            "value": 21,
                            "unit_name": "g",
                            "items": [
                                {"name_en": "sugar", "name": "糖", "value": 15, "unit_name": "g"},
                                {"name_en": "fiber_dietary", "name": "膳食纤维", "value": 3.6, "unit_name": "g"}
                            ]
                        },
                        {
                            "name_en": "protein",
                            "name": "蛋白质",
                            "value": 0.45,
                            "unit_name": "g",
                            "items": []
                        },
                        {
                            "name_en": "fat",
                            "name": "脂肪",
                            "value": 0.3,
                            "unit_name": "g",
                            "items": []
                        }
                    ],
                    "vitamin": [
                        {"name": "维生素C", "value": 6, "unit_name": "mg"},
                        {"name": "维生素K", "value": 3.3, "unit_name": "μg"}
                    ],
                    "mineral": [
                        {"name": "钠", "value": 0.0015, "unit_name": "g"},
                        {"name": "钾", "value": 0.1785, "unit_name": "g"},
                        {"name": "磷", "value": 0.018, "unit_name": "g"}
                    ],
                    "amino_acid": [],
                    "other_ingredients": []
                },
                "recorded_at": "2025-11-03T15:30:00",
                "meal_type": "加餐",
                "notes": "下午茶"
            }
        }

