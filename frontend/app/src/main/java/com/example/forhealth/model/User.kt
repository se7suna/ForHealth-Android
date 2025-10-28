package com.example.forhealth.model

data class User(
    val id: Int = 0,
    val email: String = "",
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val height: Int = 0,   // cm
    val weight: Int = 0,   // kg
    val activityLevel: String = "",
    val healthGoal: String = ""
)
