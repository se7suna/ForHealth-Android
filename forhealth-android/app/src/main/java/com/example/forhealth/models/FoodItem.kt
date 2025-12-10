package com.example.forhealth.models

data class FoodItem(
    val id: String,
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val unit: String,
    val gramsPerUnit: Double, // Weight in grams for 1 'unit'
    val image: String
)

data class SelectedFoodItem(
    val foodItem: FoodItem,
    var count: Double = 1.0,
    var mode: QuantityMode = QuantityMode.UNIT
)

enum class QuantityMode {
    UNIT,
    GRAM
}

