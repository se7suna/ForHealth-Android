package com.example.forhealth.network.dto

/**
 * AI建议请求DTO
 */
data class AiSuggestionRequest(
    val lastMealName: String,
    val lastMealCalories: Double,
    val currentCalories: Double,
    val caloriesBurned: Double,
    val netCalories: Double,
    val proteinCurrent: Double,
    val proteinTarget: Double
)

