package com.example.forhealth.model

import com.google.gson.annotations.SerializedName

/**
 * 食谱详情（展示用）
 */
data class Recipe(
    val id: String,

    @SerializedName("recipe_name")
    val recipeName: String,

    val description: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    // 食谱中的食材（含营养）
    val ingredients: List<RecipeIngredient>,

    // 食谱总营养（后端可计算，也可客户端求和）
    @SerializedName("total_nutrition")
    val totalNutrition: NutritionData? = null,

    @SerializedName("created_by")
    val createdBy: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)

/**
 * 食谱食材
 */
data class RecipeIngredient(
    @SerializedName("food_id")
    val foodId: String,

    @SerializedName("food_name")
    val foodName: String,

    val amount: Double,
    val unit: String,

    // 每种食材单份营养信息
    @SerializedName("nutrition_data")
    val nutritionData: NutritionData? = null
)

/**
 * 食谱列表响应（分页）
 */
data class RecipeListResponse(
    val page: Int,

    @SerializedName("total_pages")
    val totalPages: Int,

    val recipes: List<RecipeListItem>
)

/**
 * 简化列表项
 */
data class RecipeListItem(
    val id: String,

    @SerializedName("recipe_name")
    val recipeName: String,

    @SerializedName("image_url")
    val imageUrl: String?,

    val description: String?,

    @SerializedName("ingredient_count")
    val ingredientCount: Int,

    @SerializedName("total_calories")
    val totalCalories: Double? = null
)
