package com.example.forhealth.models

data class MealGroup(
    val id: String, // 使用第一个meal的id作为group id
    val meals: List<MealItem>,
    val time: String,
    val type: MealType
) {
    val totalCalories: Double = meals.sumOf { it.calories }
    val totalProtein: Double = meals.sumOf { it.protein }
    val totalCarbs: Double = meals.sumOf { it.carbs }
    val totalFat: Double = meals.sumOf { it.fat }
    val itemType: ItemType = ItemType.MEAL_GROUP
}

