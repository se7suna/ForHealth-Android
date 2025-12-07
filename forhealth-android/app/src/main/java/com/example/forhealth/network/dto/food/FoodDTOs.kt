package com.example.forhealth.network.dto.food

/**
 * 食物管理相关 DTO
 */

// 营养数据
data class NutritionData(
    val calories: Double,
    val protein: Double,
    val carbohydrates: Double,
    val fat: Double,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val sodium: Double? = null // 毫克
)

// 完整营养信息（简化版，实际结构更复杂）
data class FullNutritionData(
    val calory: List<Map<String, Any>>? = null,
    val base_ingredients: List<Map<String, Any>>? = null,
    val vitamin: List<Map<String, Any>>? = null,
    val mineral: List<Map<String, Any>>? = null,
    val amino_acid: List<Map<String, Any>>? = null,
    val other_ingredients: List<Map<String, Any>>? = null
)

// 创建食物请求
data class FoodCreateRequest(
    val name: String,
    val category: String? = null,
    val serving_size: Double, // 克
    val serving_unit: String = "克",
    val nutrition_per_serving: NutritionData,
    val full_nutrition: FullNutritionData? = null,
    val brand: String? = null,
    val barcode: String? = null,
    val image_url: String? = null
)

// 更新食物请求
data class FoodUpdateRequest(
    val name: String? = null,
    val category: String? = null,
    val serving_size: Double? = null,
    val serving_unit: String? = null,
    val nutrition_per_serving: NutritionData? = null,
    val full_nutrition: FullNutritionData? = null,
    val brand: String? = null,
    val barcode: String? = null,
    val image_url: String? = null
)

// 食物响应
data class FoodResponse(
    val id: String,
    val name: String,
    val category: String? = null,
    val serving_size: Double,
    val serving_unit: String,
    val nutrition_per_serving: NutritionData,
    val full_nutrition: FullNutritionData? = null,
    val brand: String? = null,
    val barcode: String? = null,
    val image_url: String? = null,
    val created_by: String? = null,
    val created_at: String, // ISO 8601 date-time
    val source: String? = null, // "local" or "boohee"
    val boohee_id: Int? = null,
    val boohee_code: String? = null
)

// 食物搜索结果条目
data class FoodSearchItemResponse(
    val source: String, // "boohee" or "local"
    val boohee_id: Int? = null,
    val food_id: String? = null,
    val boohee_code: String? = null,
    val code: String,
    val name: String,
    val weight: Double,
    val weight_unit: String = "克",
    val calory: Double,
    val image_url: String? = null,
    val is_liquid: Boolean? = null,
    val health_light: Int? = null, // 0-3
    val brand: String? = null,
    val barcode: String? = null,
    val nutrition_per_serving: NutritionData,
    val full_nutrition: FullNutritionData? = null
)

// 食物列表响应
data class FoodListResponse(
    val page: Int,
    val total_pages: Int,
    val foods: List<FoodSearchItemResponse>
)

// 简化营养数据
data class SimplifiedNutritionData(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrates: Double,
    val sugar: Double? = null,
    val sodium: Double? = null
)

// 简化食物搜索结果
data class SimplifiedFoodSearchItem(
    val source: String,
    val food_id: String? = null,
    val boohee_id: Int? = null,
    val code: String,
    val name: String,
    val weight: Double,
    val weight_unit: String = "克",
    val brand: String? = null,
    val image_url: String? = null,
    val nutrition: SimplifiedNutritionData
)

// 简化食物列表响应
data class SimplifiedFoodListResponse(
    val page: Int,
    val total_pages: Int,
    val foods: List<SimplifiedFoodSearchItem>
)

// 食物ID搜索条目
data class FoodIdItem(
    val food_id: String,
    val name: String,
    val source: String = "local",
    val brand: String? = null,
    val boohee_id: Int? = null
)

// 食物ID搜索响应
data class FoodIdSearchResponse(
    val total: Int,
    val foods: List<FoodIdItem>
)

// 创建食物记录请求
data class FoodRecordCreateRequest(
    val food_id: String,
    val source: String = "auto", // "local" or "auto"
    val serving_amount: Double, // 份量数
    val recorded_at: String, // ISO 8601 date-time
    val meal_type: String? = null, // "早餐", "午餐", "晚餐", "加餐"
    val notes: String? = null
)

// 食物记录响应
data class FoodRecordResponse(
    val id: String,
    val user_email: String,
    val food_name: String,
    val serving_amount: Double,
    val serving_size: Double,
    val serving_unit: String,
    val nutrition_data: NutritionData,
    val full_nutrition: FullNutritionData? = null,
    val recorded_at: String, // ISO 8601 date-time
    val meal_type: String? = null,
    val notes: String? = null,
    val food_id: String? = null,
    val created_at: String // ISO 8601 date-time
)

// 食物记录列表响应
data class FoodRecordListResponse(
    val total: Int,
    val records: List<FoodRecordResponse>,
    val total_nutrition: NutritionData
)

// 每日营养摘要
data class DailyNutritionSummary(
    val date: String, // YYYY-MM-DD
    val total_calories: Double,
    val total_protein: Double,
    val total_carbohydrates: Double,
    val total_fat: Double,
    val meal_count: Int,
    val records: List<FoodRecordResponse>
)

// 更新食物记录请求
data class FoodRecordUpdateRequest(
    val food_name: String? = null,
    val serving_amount: Double? = null,
    val serving_size: Double? = null,
    val serving_unit: String? = null,
    val nutrition_data: NutritionData? = null,
    val full_nutrition: FullNutritionData? = null,
    val recorded_at: String? = null,
    val meal_type: String? = null,
    val notes: String? = null
)

// 条形码图片识别响应
data class BarcodeImageRecognitionResponse(
    val success: Boolean,
    val message: String,
    val barcode: String? = null,
    val barcode_type: String? = null // "EAN13", "CODE128" 等
)

// 条形码扫描响应
data class BarcodeScanResponse(
    val found: Boolean,
    val message: String,
    val food_data: FoodResponse? = null
)

