package com.example.forhealth.models

data class ActivityItem(
    val id: String,
    val name: String,
    val caloriesBurned: Double,
    val duration: Int, // minutes
    val time: String,
    val type: ExerciseType,
    val image: String? = null
) {
    val itemType: ItemType = ItemType.EXERCISE
}

enum class ExerciseType {
    CARDIO,
    STRENGTH,
    FLEXIBILITY,
    SPORTS
}

