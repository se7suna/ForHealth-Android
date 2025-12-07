package com.example.forhealth.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.forhealth.models.ActivityItem
import com.example.forhealth.models.DailyStats
import com.example.forhealth.models.MealGroup
import com.example.forhealth.models.MealItem
import com.example.forhealth.models.MealGroupTimelineItem
import com.example.forhealth.models.TimelineItem
import com.example.forhealth.models.UserProfile
import com.example.forhealth.models.WorkoutGroup
import com.example.forhealth.models.WorkoutGroupTimelineItem
import com.example.forhealth.utils.DateUtils

class MainViewModel : ViewModel() {
    
    private val _dailyStats = MutableLiveData<DailyStats>(DailyStats.getInitial())
    val dailyStats: LiveData<DailyStats> = _dailyStats
    
    private val _meals = MutableLiveData<List<MealItem>>(emptyList())
    val meals: LiveData<List<MealItem>> = _meals
    
    private val _exercises = MutableLiveData<List<ActivityItem>>(emptyList())
    val exercises: LiveData<List<ActivityItem>> = _exercises
    
    private val _timelineItems = MutableLiveData<List<TimelineItem>>(emptyList())
    val timelineItems: LiveData<List<TimelineItem>> = _timelineItems
    
    private val _aiSuggestion = MutableLiveData<String>("Ready to track! Add your first meal to get AI insights.")
    val aiSuggestion: LiveData<String> = _aiSuggestion
    
    private val _userProfile = MutableLiveData<UserProfile>(UserProfile.getInitial())
    val userProfile: LiveData<UserProfile> = _userProfile
    
    init {
        updateTimeline()
    }
    
    fun addMeals(meals: List<MealItem>) {
        if (meals.isEmpty()) return
        
        val currentMeals = _meals.value ?: emptyList()
        val resultMeals = currentMeals.toMutableList()
        
        // 先合并新添加的meals中相同名称、相同类型、相同时间的
        val newMealsGrouped = meals.groupBy { "${it.name}_${it.type}_${it.time}" }
            .map { (_, mealList) ->
                if (mealList.size == 1) {
                    mealList.first()
                } else {
                    // 合并相同名称、类型、时间的meal
                    val first = mealList.first()
                    mealList.drop(1).fold(first) { acc, meal ->
                        acc.copy(
                            calories = acc.calories + meal.calories,
                            protein = acc.protein + meal.protein,
                            carbs = acc.carbs + meal.carbs,
                            fat = acc.fat + meal.fat
                        )
                    }
                }
            }
        
        // 然后与现有的meals合并
        // 对于同一类型的meal，需要找到同一类型中时间最接近的meal进行合并
        // 如果找不到，则添加到对应类型的meal group中
        newMealsGrouped.forEach { newMeal ->
            // 先尝试找相同名称、类型、时间的meal（精确匹配）
            var existingIndex = resultMeals.indexOfFirst { 
                it.name == newMeal.name && 
                it.type == newMeal.type && 
                it.time == newMeal.time 
            }
            
            if (existingIndex >= 0) {
                // 找到精确匹配的meal，合并数量和营养
                val existing = resultMeals[existingIndex]
                resultMeals[existingIndex] = existing.copy(
                    calories = existing.calories + newMeal.calories,
                    protein = existing.protein + newMeal.protein,
                    carbs = existing.carbs + newMeal.carbs,
                    fat = existing.fat + newMeal.fat
                )
            } else {
                // 没有找到精确匹配，查找同一类型中是否有相同名称的meal（用于合并同一类型的meal）
                // 找到同一类型、相同名称的meal，使用第一个找到的进行合并
                existingIndex = resultMeals.indexOfFirst { 
                    it.name == newMeal.name && 
                    it.type == newMeal.type
                }
                
                if (existingIndex >= 0) {
                    // 找到相同类型和名称的meal，合并到该meal中，并更新时间戳为新的时间
                    val existing = resultMeals[existingIndex]
                    resultMeals[existingIndex] = existing.copy(
                        calories = existing.calories + newMeal.calories,
                        protein = existing.protein + newMeal.protein,
                        carbs = existing.carbs + newMeal.carbs,
                        fat = existing.fat + newMeal.fat,
                        time = newMeal.time // 使用新的时间戳
                    )
                } else {
                    // 没有找到相同类型和名称的meal，直接添加
                    resultMeals.add(newMeal)
                }
            }
        }
        
        _meals.value = resultMeals
        
        // 重新计算所有统计数据
        recalculateStats()
        
        // 更新时间线
        updateTimeline()
        
        // TODO: 更新AI建议
    }
    
    fun addExercises(exercises: List<ActivityItem>) {
        val currentExercises = _exercises.value ?: emptyList()
        val updatedExercises = exercises + currentExercises
        _exercises.value = updatedExercises
        
        // 更新统计数据
        val burned = exercises.sumOf { it.caloriesBurned }
        val currentStats = _dailyStats.value ?: DailyStats.getInitial()
        _dailyStats.value = currentStats.copy(burned = currentStats.burned + burned)
        
        // 更新时间线
        updateTimeline()
    }
    
    private fun updateStatsAfterMeals(newMeals: List<MealItem>) {
        val currentStats = _dailyStats.value ?: DailyStats.getInitial()
        
        val addedCalories = newMeals.sumOf { it.calories }
        val addedProtein = newMeals.sumOf { it.protein }
        val addedCarbs = newMeals.sumOf { it.carbs }
        val addedFat = newMeals.sumOf { it.fat }
        
        _dailyStats.value = DailyStats(
            calories = currentStats.calories.copy(current = currentStats.calories.current + addedCalories),
            protein = currentStats.protein.copy(current = currentStats.protein.current + addedProtein),
            carbs = currentStats.carbs.copy(current = currentStats.carbs.current + addedCarbs),
            fat = currentStats.fat.copy(current = currentStats.fat.current + addedFat),
            burned = currentStats.burned
        )
    }
    
    private fun updateTimeline() {
        val meals = _meals.value ?: emptyList()
        val exercises = _exercises.value ?: emptyList()
        
        // 按餐分组：相同时间和类型的meal归为一组
        val mealGroups = meals.groupBy { "${it.time}_${it.type}" }
            .map { (_, mealList) ->
                val firstMeal = mealList.first()
                MealGroup(
                    id = firstMeal.id, // 使用第一个meal的id作为group id
                    meals = mealList,
                    time = firstMeal.time,
                    type = firstMeal.type
                )
            }
        
        // 按workout分组：每条exercise单独成为一组（强制拆分）
        val workoutGroups = exercises.map { activity ->
            WorkoutGroup(
                id = activity.id, // 使用activity的id作为group id
                activities = listOf(activity), // 每条运动单独打包
                time = activity.time
            )
        }
        
        val timeline: List<TimelineItem> = 
            (mealGroups.map { MealGroupTimelineItem(it) } + 
             workoutGroups.map { WorkoutGroupTimelineItem(it) })
            .sortedByDescending { it.time }
        _timelineItems.value = timeline
    }
    
    fun updateMealGroup(mealGroup: MealGroup) {
        val currentMeals = _meals.value ?: emptyList()
        
        // 删除旧的所有meals（通过time和type匹配，确保同一顿饭的记录都被删除）
        val updatedMeals = currentMeals.filterNot { meal ->
            meal.time == mealGroup.time && meal.type == mealGroup.type
        }
        
        // 添加新的meals
        val finalMeals = mealGroup.meals + updatedMeals
        _meals.value = finalMeals
        
        // 重新计算统计数据
        recalculateStats()
        
        // 更新时间线
        updateTimeline()
    }
    
    fun updateWorkoutGroup(workoutGroup: WorkoutGroup) {
        val currentExercises = _exercises.value ?: emptyList()
        
        // 删除旧的activity（通过id匹配，因为现在每个WorkoutGroup只包含一条运动）
        val updatedExercises = currentExercises.filterNot { exercise ->
            exercise.id == workoutGroup.id
        }
        
        // 添加新的activities（虽然通常只有一条，但保持兼容性）
        val finalExercises = workoutGroup.activities + updatedExercises
        _exercises.value = finalExercises
        
        // 重新计算统计数据
        val currentStats = _dailyStats.value ?: DailyStats.getInitial()
        val totalBurned = finalExercises.sumOf { it.caloriesBurned }
        _dailyStats.value = currentStats.copy(burned = totalBurned)
        
        // 更新时间线
        updateTimeline()
    }
    
    fun deleteMealGroup(mealGroupId: String) {
        val currentMeals = _meals.value ?: emptyList()
        
        // 找到对应的meal group并删除所有meals
        // 通过mealGroupId找到对应的meal，然后通过time和type找到同一组的所有meals
        val targetMeal = currentMeals.find { it.id == mealGroupId }
        if (targetMeal != null) {
            val updatedMeals = currentMeals.filterNot { meal ->
                meal.time == targetMeal.time && meal.type == targetMeal.type
            }
            _meals.value = updatedMeals
            
            // 重新计算统计数据
            recalculateStats()
            
            // 更新时间线
            updateTimeline()
        }
    }
    
    fun deleteWorkoutGroup(workoutGroupId: String) {
        val currentExercises = _exercises.value ?: emptyList()
        
        // 删除对应的activity（通过id匹配，因为现在每个WorkoutGroup只包含一条运动）
        val updatedExercises = currentExercises.filterNot { exercise ->
            exercise.id == workoutGroupId
        }
        _exercises.value = updatedExercises
        
        // 重新计算统计数据
        val currentStats = _dailyStats.value ?: DailyStats.getInitial()
        val totalBurned = updatedExercises.sumOf { it.caloriesBurned }
        _dailyStats.value = currentStats.copy(burned = totalBurned)
        
        // 更新时间线
        updateTimeline()
    }
    
    private fun recalculateStats() {
        val meals = _meals.value ?: emptyList()
        val currentStats = _dailyStats.value ?: DailyStats.getInitial()
        
        val totalCalories = meals.sumOf { it.calories }
        val totalProtein = meals.sumOf { it.protein }
        val totalCarbs = meals.sumOf { it.carbs }
        val totalFat = meals.sumOf { it.fat }
        
        _dailyStats.value = DailyStats(
            calories = currentStats.calories.copy(current = totalCalories),
            protein = currentStats.protein.copy(current = totalProtein),
            carbs = currentStats.carbs.copy(current = totalCarbs),
            fat = currentStats.fat.copy(current = totalFat),
            burned = currentStats.burned
        )
    }
}

