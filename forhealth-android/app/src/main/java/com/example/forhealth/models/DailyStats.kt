package com.example.forhealth.models

data class DailyStats(
    val calories: Macro,
    val protein: Macro,
    val carbs: Macro,
    val fat: Macro,
    var burned: Double = 0.0 // Calories burned
) {
    companion object {
        fun getInitial(): DailyStats {
            return DailyStats(
                calories = Macro(current = 0.0, target = 2200.0, unit = "kcal"),
                protein = Macro(current = 0.0, target = 150.0, unit = "g"),
                carbs = Macro(current = 0.0, target = 250.0, unit = "g"),
                fat = Macro(current = 0.0, target = 70.0, unit = "g"),
                burned = 0.0
            )
        }
    }
}

