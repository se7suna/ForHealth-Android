package com.example.forhealth.model

data class ExercisePlanResponse(
    val id: String,
    val objectType: String,
    val created: Int,
    val model: String,
    val choices: List<Choice>
)

data class Choice(
    val message: Message,
    val finish_reason: String,
    val index: Int
)
