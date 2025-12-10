package com.example.forhealth.network.dto

import com.example.forhealth.models.ExerciseType

/**
 * 添加运动的请求DTO
 */
data class ExerciseRequest(
    val name: String,
    val caloriesBurned: Double,
    val duration: Int, // minutes
    val type: String, // "CARDIO", "STRENGTH", "FLEXIBILITY", "SPORTS"
    val time: String,
    val image: String? = null
) {
    companion object {
        fun fromExerciseType(type: ExerciseType): String {
            return when (type) {
                ExerciseType.CARDIO -> "CARDIO"
                ExerciseType.STRENGTH -> "STRENGTH"
                ExerciseType.FLEXIBILITY -> "FLEXIBILITY"
                ExerciseType.SPORTS -> "SPORTS"
            }
        }
    }
}

