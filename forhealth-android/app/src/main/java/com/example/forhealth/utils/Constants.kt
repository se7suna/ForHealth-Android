package com.example.forhealth.utils

import com.example.forhealth.models.ExerciseItem
import com.example.forhealth.models.ExerciseType
import com.example.forhealth.models.FoodItem

object Constants {
    
    // Food Database (对应前端的 FOOD_DB)
    val FOOD_DB: List<FoodItem> = listOf(
        FoodItem(
            id = "1",
            name = "Boiled Egg",
            calories = 78.0,
            protein = 6.0,
            carbs = 0.6,
            fat = 5.0,
            unit = "1 large",
            gramsPerUnit = 50.0,
            image = "https://images.unsplash.com/photo-1482049016688-2d3e1b311543?w=150&q=80"
        ),
        FoodItem(
            id = "2",
            name = "Chicken Breast",
            calories = 165.0,
            protein = 31.0,
            carbs = 0.0,
            fat = 3.6,
            unit = "100g",
            gramsPerUnit = 100.0,
            image = "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=150&q=80"
        ),
        FoodItem(
            id = "3",
            name = "Oatmeal",
            calories = 150.0,
            protein = 5.0,
            carbs = 27.0,
            fat = 3.0,
            unit = "1 cup cooked",
            gramsPerUnit = 234.0,
            image = "https://images.unsplash.com/photo-1517673132405-a56a62b18caf?w=150&q=80"
        ),
        FoodItem(
            id = "4",
            name = "Banana",
            calories = 105.0,
            protein = 1.3,
            carbs = 27.0,
            fat = 0.3,
            unit = "1 medium",
            gramsPerUnit = 118.0,
            image = "https://images.unsplash.com/photo-1571771896331-1041621c310f?w=150&q=80"
        ),
        FoodItem(
            id = "5",
            name = "Rice (White)",
            calories = 130.0,
            protein = 2.7,
            carbs = 28.0,
            fat = 0.3,
            unit = "100g",
            gramsPerUnit = 100.0,
            image = "https://images.unsplash.com/photo-1516684732162-798a0062be99?w=150&q=80"
        ),
        FoodItem(
            id = "6",
            name = "Avocado",
            calories = 160.0,
            protein = 2.0,
            carbs = 8.5,
            fat = 15.0,
            unit = "1/2 fruit",
            gramsPerUnit = 100.0,
            image = "https://images.unsplash.com/photo-1523049673856-4287bf676329?w=150&q=80"
        ),
        FoodItem(
            id = "7",
            name = "Salmon Fillet",
            calories = 208.0,
            protein = 20.0,
            carbs = 0.0,
            fat = 13.0,
            unit = "100g",
            gramsPerUnit = 100.0,
            image = "https://images.unsplash.com/photo-1599084993091-1cb5c0721cc6?w=150&q=80"
        ),
        FoodItem(
            id = "8",
            name = "Greek Yogurt",
            calories = 100.0,
            protein = 10.0,
            carbs = 3.6,
            fat = 0.0,
            unit = "1 cup",
            gramsPerUnit = 245.0,
            image = "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=150&q=80"
        ),
        FoodItem(
            id = "9",
            name = "Almonds",
            calories = 164.0,
            protein = 6.0,
            carbs = 6.0,
            fat = 14.0,
            unit = "1 oz",
            gramsPerUnit = 28.0,
            image = "https://images.unsplash.com/photo-1508061253366-f7da158b6d61?w=150&q=80"
        ),
        FoodItem(
            id = "10",
            name = "Apple",
            calories = 95.0,
            protein = 0.5,
            carbs = 25.0,
            fat = 0.3,
            unit = "1 medium",
            gramsPerUnit = 182.0,
            image = "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=150&q=80"
        ),
        FoodItem(
            id = "11",
            name = "Whole Wheat Bread",
            calories = 80.0,
            protein = 4.0,
            carbs = 13.0,
            fat = 1.0,
            unit = "1 slice",
            gramsPerUnit = 43.0,
            image = "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=150&q=80"
        ),
        FoodItem(
            id = "12",
            name = "Peanut Butter",
            calories = 190.0,
            protein = 8.0,
            carbs = 6.0,
            fat = 16.0,
            unit = "2 tbsp",
            gramsPerUnit = 32.0,
            image = "https://images.unsplash.com/photo-1514660882326-89c09641753b?w=150&q=80"
        ),
        FoodItem(
            id = "13",
            name = "Caesar Salad",
            calories = 350.0,
            protein = 12.0,
            carbs = 15.0,
            fat = 28.0,
            unit = "1 bowl",
            gramsPerUnit = 300.0,
            image = "https://images.unsplash.com/photo-1550304943-4f24f54ddde9?w=150&q=80"
        ),
        FoodItem(
            id = "14",
            name = "Protein Shake",
            calories = 120.0,
            protein = 25.0,
            carbs = 3.0,
            fat = 1.0,
            unit = "1 scoop",
            gramsPerUnit = 30.0,
            image = "https://images.unsplash.com/photo-1593095948071-474c5cc2989d?w=150&q=80"
        ),
        FoodItem(
            id = "15",
            name = "Sweet Potato",
            calories = 112.0,
            protein = 2.0,
            carbs = 26.0,
            fat = 0.1,
            unit = "1 medium",
            gramsPerUnit = 130.0,
            image = "https://images.unsplash.com/photo-1596097635121-14b63b8a66cf?w=150&q=80"
        )
    )
    
    // Exercise Database (对应前端的 EXERCISE_DB)
    val EXERCISE_DB: List<ExerciseItem> = listOf(
        ExerciseItem(
            id = "e1",
            name = "Running (Moderate)",
            caloriesPerUnit = 11.0,
            unit = "min",
            category = ExerciseType.CARDIO,
            image = "https://images.unsplash.com/photo-1502904550040-7534597429ae?w=150&q=80"
        ),
        ExerciseItem(
            id = "e2",
            name = "Cycling (Indoor)",
            caloriesPerUnit = 7.0,
            unit = "min",
            category = ExerciseType.CARDIO,
            image = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=150&q=80"
        ),
        ExerciseItem(
            id = "e3",
            name = "Weight Lifting",
            caloriesPerUnit = 6.0,
            unit = "min",
            category = ExerciseType.STRENGTH,
            image = "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=150&q=80"
        ),
        ExerciseItem(
            id = "e4",
            name = "Yoga",
            caloriesPerUnit = 4.0,
            unit = "min",
            category = ExerciseType.FLEXIBILITY,
            image = "https://images.unsplash.com/photo-1544367563-12123d8966cd?w=150&q=80"
        ),
        ExerciseItem(
            id = "e5",
            name = "Swimming",
            caloriesPerUnit = 10.0,
            unit = "min",
            category = ExerciseType.CARDIO,
            image = "https://images.unsplash.com/photo-1530549387789-4c1017266635?w=150&q=80"
        ),
        ExerciseItem(
            id = "e6",
            name = "HIIT Workout",
            caloriesPerUnit = 13.0,
            unit = "min",
            category = ExerciseType.CARDIO,
            image = "https://images.unsplash.com/photo-1601422407692-ec4eeec1d9b3?w=150&q=80"
        ),
        ExerciseItem(
            id = "e7",
            name = "Walking (Brisk)",
            caloriesPerUnit = 4.0,
            unit = "min",
            category = ExerciseType.CARDIO,
            image = "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=150&q=80"
        ),
        ExerciseItem(
            id = "e8",
            name = "Basketball",
            caloriesPerUnit = 8.0,
            unit = "min",
            category = ExerciseType.SPORTS,
            image = "https://images.unsplash.com/photo-1546519638-68e109498ee3?w=150&q=80"
        ),
        ExerciseItem(
            id = "e9",
            name = "Jump Rope",
            caloriesPerUnit = 12.0,
            unit = "min",
            category = ExerciseType.CARDIO,
            image = "https://images.unsplash.com/photo-1599058945522-28d584b6f0ff?w=150&q=80"
        ),
        ExerciseItem(
            id = "e10",
            name = "Pilates",
            caloriesPerUnit = 5.0,
            unit = "min",
            category = ExerciseType.FLEXIBILITY,
            image = "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=150&q=80"
        )
    )
}

