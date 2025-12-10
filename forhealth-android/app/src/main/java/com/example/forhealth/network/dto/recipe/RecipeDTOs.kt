package com.example.forhealth.network.dto.recipe

import com.example.forhealth.network.dto.food.FullNutritionData
import com.example.forhealth.network.dto.food.NutritionData

/**
 * 食谱管理相关 DTO
 */

// 食谱中的食物项
data class RecipeFoodItem(
    val food_id: String,
    val food_name: String,
    val serving_amount: Double,
    val serving_size: Double,
    val serving_unit: String = "克",
    val nutrition: NutritionData,
    val full_nutrition: FullNutritionData? = null
)

// 创建食谱请求
data class RecipeCreateRequest(
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val foods: List<RecipeFoodItem>, // 至少1个
    val tags: List<String>? = null,
    val image_url: String? = null,
    val prep_time: Int? = null // 分钟
)

// 更新食谱请求
data class RecipeUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val foods: List<RecipeFoodItem>? = null,
    val tags: List<String>? = null,
    val image_url: String? = null,
    val prep_time: Int? = null
)

// 食谱响应
data class RecipeResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val foods: List<RecipeFoodItem>,
    val total_nutrition: NutritionData,
    val total_full_nutrition: FullNutritionData? = null,
    val tags: List<String>? = null,
    val image_url: String? = null,
    val prep_time: Int? = null,
    val created_by: String? = null,
    val created_at: String, // ISO 8601 date-time
    val updated_at: String // ISO 8601 date-time
)

// 食谱列表响应
data class RecipeListResponse(
    val total: Int,
    val recipes: List<RecipeResponse>
)

// 食谱ID项
data class RecipeIdItem(
    val id: String,
    val name: String,
    val category: String? = null,
    val created_by: String? = null
)

// 食谱ID搜索响应
data class RecipeIdSearchResponse(
    val total: Int,
    val recipes: List<RecipeIdItem>
)

// 创建食谱记录请求
data class RecipeRecordCreateRequest(
    val recipe_id: String,
    val scale: Double = 1.0, // 份量倍数
    val recorded_at: String, // ISO 8601 date-time
    val meal_type: String? = null,
    val notes: String? = null
)

// 食谱记录响应
data class RecipeRecordResponse(
    val message: String,
    val recipe_name: String,
    val batch_id: String,
    val total_records: Int,
    val record_ids: List<String>,
    val total_nutrition: NutritionData
)

// 食谱记录批次项
data class RecipeRecordBatchItem(
    val batch_id: String,
    val recipe_name: String,
    val total_records: Int,
    val recorded_at: String, // ISO 8601 date-time
    val meal_type: String? = null,
    val total_nutrition: NutritionData,
    val notes: String? = null
)

// 食谱记录列表响应
data class RecipeRecordListResponse(
    val total: Int,
    val batches: List<RecipeRecordBatchItem>,
    val total_nutrition: NutritionData
)

// 更新食谱记录请求
data class RecipeRecordUpdateRequest(
    val recorded_at: String? = null,
    val meal_type: String? = null,
    val notes: String? = null
)

// 更新食谱记录响应
data class RecipeRecordUpdateResponse(
    val message: String,
    val recipe_name: String,
    val batch_id: String,
    val updated_count: Int,
    val total_nutrition: NutritionData
)

