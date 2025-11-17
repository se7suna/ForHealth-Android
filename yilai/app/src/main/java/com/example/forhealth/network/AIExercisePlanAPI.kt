package com.example.forhealth.network

import com.example.forhealth.model.ExercisePlanRequest
import com.example.forhealth.model.ExercisePlanResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AIExercisePlanAPI {

    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    fun getChatCompletion(@Body request: ExercisePlanRequest): Call<ExercisePlanResponse>
}

