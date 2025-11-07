package com.example.forhealth.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class ExercisePlanRequest(
    val goal: String,
    val fitnessLevel: String,
    val preferredExercise: String,
    val frequency: Int,
    val duration: Int,
    val intensity: String
)

data class ExercisePlanResponse(
    val exerciseType: String,
    val frequency: Int,
    val duration: Int,
    val intensity: String,
    val recommendation: String
)

interface ExercisePlanAPI {
    @POST("generatePlan")
    fun generateExercisePlan(@Body request: ExercisePlanRequest): Call<ExercisePlanResponse>
}
