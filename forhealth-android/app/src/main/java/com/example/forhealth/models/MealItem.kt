package com.example.forhealth.models

data class MealItem(
    val id: String, // record id
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val time: String,
    val type: MealType,
    val image: String? = null,
    val foodId: String? = null, // food_id from FoodRecordResponse
    val servingAmount: Double? = null // 份量（与后端serving_amount对齐）
) {
    val itemType: ItemType = ItemType.MEAL
}

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK
}

enum class ItemType {
    MEAL,
    MEAL_GROUP,
    EXERCISE,
    WORKOUT_GROUP
}

