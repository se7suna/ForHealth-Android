package com.example.forhealth.models

data class WorkoutGroup(
    val id: String, // 使用第一个activity的id作为group id
    val activities: List<ActivityItem>,
    val time: String
) {
    val totalCaloriesBurned: Double = activities.sumOf { it.caloriesBurned }
    val totalDuration: Int = activities.sumOf { it.duration }
    val itemType: ItemType = ItemType.WORKOUT_GROUP
}

