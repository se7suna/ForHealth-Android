package com.example.forhealth.models

data class ExerciseItem(
    val id: String,
    val name: String,
    val caloriesPerUnit: Double, // Calories burned per minute (usually)
    val unit: String, // 'min', 'set'
    val image: String,
    val category: ExerciseType
)

data class SelectedExerciseItem(
    val exerciseItem: ExerciseItem,
    var count: Double = 30.0 // Duration in minutes usually
)

