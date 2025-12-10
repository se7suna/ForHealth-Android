package com.example.forhealth.network.dto

import com.example.forhealth.models.MealType

/**
 * 添加餐食的请求DTO
 * 根据后端API要求调整字段
 */
data class MealRequest(
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val type: String, // "BREAKFAST", "LUNCH", "DINNER", "SNACK"
    val time: String, // ISO 8601格式或自定义格式
    val image: String? = null
) {
    companion object {
        fun fromMealType(type: MealType): String {
            return when (type) {
                MealType.BREAKFAST -> "BREAKFAST"
                MealType.LUNCH -> "LUNCH"
                MealType.DINNER -> "DINNER"
                MealType.SNACK -> "SNACK"
            }
        }
    }
}

