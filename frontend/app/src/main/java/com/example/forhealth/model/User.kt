package com.example.forhealth.model

data class User(
    val email: String? = null,
    val username: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    val age: Int? = null,
    val gender: String? = null,
    val birthdate: String? = null,
    val birth_year: Int? = null,
    val birth_month: Int? = null,
    val birth_day: Int? = null,
    val activity_level: String? = null,
    val health_goal_type: String? = null,
    val goal_type: String? = null,
    val target_weight: Double? = null,
    val goal_weight: Double? = null,
    val goal_period_weeks: Int? = null,
    val goal_weeks: Int? = null,
    val bmr: Double? = null,
    val tdee: Double? = null,
    val daily_calorie_goal: Double? = null
)
