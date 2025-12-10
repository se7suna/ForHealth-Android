package com.example.forhealth.models

data class UserProfile(
    val name: String,
    val age: Int,
    val height: Int, // in cm
    val weight: Int, // in kg
    val gender: String, // "Male" or "Female"
    val activityLevel: String
) {
    companion object {
        fun getInitial(): UserProfile {
            return UserProfile(
                name = "User",
                age = 25,
                height = 170,
                weight = 70,
                gender = "Male",
                activityLevel = "Moderate"
            )
        }
    }
}

