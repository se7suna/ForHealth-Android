package com.example.forhealth.models

data class CustomFoodForm(
    var name: String = "",
    var unitName: String = "1 serving",
    var weightPerUnit: String = "100",
    var calories: String = "",
    var protein: String = "",
    var carbs: String = "",
    var fat: String = ""
)

