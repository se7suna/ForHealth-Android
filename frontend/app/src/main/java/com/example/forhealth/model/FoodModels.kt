package com.example.forhealth.model

import com.google.gson.annotations.SerializedName

/**
 * 营养数据
 */
data class NutritionData(
    val calories: Double,
    val protein: Double,
    val carbohydrates: Double,
    val fat: Double,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val sodium: Double? = null
)

/**
 * 创建自定义食物请求
 */
data class FoodCreateRequest(
    val name: String,
    val category: String? = null,
    @SerializedName("serving_size") val servingSize: Double,
    @SerializedName("serving_unit") val servingUnit: String = "克",
    @SerializedName("nutrition_per_serving") val nutritionPerServing: NutritionData,
    val brand: String? = null,
    val barcode: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("full_nutrition") val fullNutrition: Any? = null
)

/**
 * 食物详情响应
 */
data class FoodResponse(
    val id: String,
    val name: String,
    val category: String?,
    @SerializedName("serving_size") val servingSize: Double,
    @SerializedName("serving_unit") val servingUnit: String,
    @SerializedName("nutrition_per_serving") val nutritionPerServing: NutritionData,
    val brand: String?,
    val barcode: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("source") val source: String?,
    @SerializedName("boohee_id") val booheeId: Int? = null,
    @SerializedName("boohee_code") val booheeCode: String? = null
)

/**
 * 简化的食物搜索结果
 */
data class SimplifiedFoodSearchItem(
    val source: String,
    @SerializedName("food_id") val foodId: String?,
    @SerializedName("boohee_id") val booheeId: Int?,
    val code: String,
    val name: String,
    val weight: Double,
    @SerializedName("weight_unit") val weightUnit: String,
    val brand: String?,
    @SerializedName("image_url") val imageUrl: String?,
    val nutrition: SimplifiedNutritionData
)

/**
 * 简化的营养数据
 */
data class SimplifiedNutritionData(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrates: Double,
    val sugar: Double?,
    val sodium: Double?
)

/**
 * 简化的食物列表响应
 */
data class SimplifiedFoodListResponse(
    val page: Int,
    @SerializedName("total_pages") val totalPages: Int,
    val foods: List<SimplifiedFoodSearchItem>
)

/**
 * 每日营养摘要
 */
data class DailyNutritionSummary(
    val date: String,
    @SerializedName("total_calories") val totalCalories: Double,
    @SerializedName("total_protein") val totalProtein: Double,
    @SerializedName("total_carbohydrates") val totalCarbohydrates: Double,
    @SerializedName("total_fat") val totalFat: Double,
    @SerializedName("meal_count") val mealCount: Int,
    val records: List<FoodRecord>
)

/**
 * 食物记录
 */
data class FoodRecord(
    val id: String,
    @SerializedName("user_email") val userEmail: String,
    @SerializedName("food_name") val foodName: String,
    @SerializedName("serving_amount") val servingAmount: Double,
    @SerializedName("serving_size") val servingSize: Double,
    @SerializedName("serving_unit") val servingUnit: String,
    @SerializedName("nutrition_data") val nutritionData: NutritionData,
    @SerializedName("recorded_at") val recordedAt: String,
    @SerializedName("meal_type") val mealType: String?,
    val notes: String?,
    @SerializedName("food_id") val foodId: String?,
    @SerializedName("created_at") val createdAt: String,
    val simplifiedFood: SimplifiedFoodSearchItem
)

/**
 * 创建食物记录请求
 */
data class CreateFoodRecordRequest(
    @SerializedName("food_id") val foodId: String,
    val source: String = "auto",
    @SerializedName("serving_amount") val servingAmount: Double,
    @SerializedName("recorded_at") val recordedAt: String,
    @SerializedName("meal_type") val mealType: String?,
    val notes: String? = null
)

/**
 * 食物记录响应
 */
data class FoodRecordResponse(
    val id: String,
    @SerializedName("user_email") val userEmail: String,
    @SerializedName("food_name") val foodName: String,
    @SerializedName("serving_amount") val servingAmount: Double,
    @SerializedName("serving_size") val servingSize: Double,
    @SerializedName("serving_unit") val servingUnit: String,
    @SerializedName("nutrition_data") val nutritionData: NutritionData,
    @SerializedName("recorded_at") val recordedAt: String,
    @SerializedName("meal_type") val mealType: String?,
    val notes: String?,
    @SerializedName("food_id") val foodId: String?,
    @SerializedName("created_at") val createdAt: String
)

/**
 * 食物ID搜索项
 */
data class FoodIdItem(
    @SerializedName("food_id") val foodId: String,
    val name: String,
    val source: String,
    val brand: String?,
    @SerializedName("boohee_id") val booheeId: Int?
)

/**
 * 食物ID搜索响应
 */
data class FoodIdSearchResponse(
    val count: Int,
    val foods: List<FoodIdItem>
)

