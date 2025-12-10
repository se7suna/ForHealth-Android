package com.example.forhealth.utils

import com.example.forhealth.models.MealItem
import com.example.forhealth.models.ActivityItem
import com.example.forhealth.models.SelectedFoodItem

object CalculationUtils {
    
    /**
     * 计算选中食物的宏量营养素
     */
    fun calculateFoodMacros(item: SelectedFoodItem): FoodMacros {
        val ratio = when (item.mode) {
            com.example.forhealth.models.QuantityMode.GRAM -> item.count / item.foodItem.gramsPerUnit
            com.example.forhealth.models.QuantityMode.UNIT -> item.count
        }
        
        return FoodMacros(
            calories = item.foodItem.calories * ratio,
            protein = item.foodItem.protein * ratio,
            carbs = item.foodItem.carbs * ratio,
            fat = item.foodItem.fat * ratio
        )
    }
    
    /**
     * 计算运动消耗的卡路里
     */
    fun calculateExerciseCalories(exerciseItem: com.example.forhealth.models.ExerciseItem, duration: Double): Double {
        return exerciseItem.caloriesPerUnit * duration
    }
    
    /**
     * 计算宏量营养素比例（用于图表显示）
     */
    fun calculateMacroRatios(meals: List<MealItem>): MacroRatios {
        val macros = meals.fold(FoodMacros(0.0, 0.0, 0.0, 0.0)) { acc, meal ->
            acc.copy(
                calories = acc.calories + meal.calories,
                protein = acc.protein + meal.protein,
                carbs = acc.carbs + meal.carbs,
                fat = acc.fat + meal.fat
            )
        }
        
        // Convert to calories (Protein/Carbs = 4kcal, Fat = 9kcal)
        val pCal = macros.protein * 4
        val cCal = macros.carbs * 4
        val fCal = macros.fat * 9
        val totalMacroCal = pCal + cCal + fCal
        
        if (totalMacroCal == 0.0) {
            return MacroRatios(0.0, 0.0, 0.0)
        }
        
        return MacroRatios(
            proteinPercent = (pCal / totalMacroCal) * 100,
            carbsPercent = (cCal / totalMacroCal) * 100,
            fatPercent = (fCal / totalMacroCal) * 100
        )
    }
    
    /**
     * 计算各餐类型的卡路里分布
     */
    fun calculateMealTypeDistribution(meals: List<MealItem>): Map<com.example.forhealth.models.MealType, Double> {
        return meals.groupBy { it.type }
            .mapValues { (_, mealList) -> mealList.sumOf { it.calories } }
    }
}

data class FoodMacros(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class MacroRatios(
    val proteinPercent: Double,
    val carbsPercent: Double,
    val fatPercent: Double
)

