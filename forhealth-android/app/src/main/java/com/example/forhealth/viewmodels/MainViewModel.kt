package com.example.forhealth.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forhealth.models.ActivityItem
import com.example.forhealth.models.DailyStats
import com.example.forhealth.models.MealGroup
import com.example.forhealth.models.MealItem
import com.example.forhealth.models.MealGroupTimelineItem
import com.example.forhealth.models.MealType
import com.example.forhealth.models.TimelineItem
import com.example.forhealth.models.UserProfile
import com.example.forhealth.models.WorkoutGroup
import com.example.forhealth.models.WorkoutGroupTimelineItem
import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.dto.food.FoodRecordCreateRequest
import com.example.forhealth.network.dto.food.FoodRecordResponse
import com.example.forhealth.network.dto.food.FoodRecordUpdateRequest
import com.example.forhealth.repositories.MealRepository
import com.example.forhealth.utils.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    
    /**
     * 直接设置meals数据（从API加载时使用，替换而不是合并）
     */
    fun setMeals(meals: List<MealItem>) {
        _meals.value = meals
        recalculateStats()
        updateTimeline()
    }
    
    /**
     * 直接设置exercises数据（从API加载时使用，替换而不是合并）
     */
    fun setExercises(exercises: List<ActivityItem>) {
        _exercises.value = exercises
        
        // 更新统计数据
        val totalBurned = exercises.sumOf { it.caloriesBurned }
        val currentStats = _dailyStats.value ?: DailyStats.getInitial()
        _dailyStats.value = currentStats.copy(burned = totalBurned)
        
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
        
        // 按餐分组：相同meal_type的meal归为一组（根据后端API的meal_type字段）
        val mealGroups = meals.groupBy { it.type }
            .map { (mealType, mealList) ->
                // 使用该meal_type中最早的时间作为显示时间
                val earliestTime = mealList.minByOrNull { it.time }?.time ?: DateUtils.getCurrentTime()
                MealGroup(
                    id = mealList.firstOrNull()?.id ?: "", // 使用第一个meal的id作为group id
                    meals = mealList,
                    time = earliestTime,
                    type = mealType
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
        
        // 通过 MealGroup 的 id 找到对应的原始 meal
        // MealGroup 的 id 是第一个 meal 的 id，所以通过 id 找到对应的 meal
        val targetMeal = currentMeals.find { it.id == mealGroup.id }
        
        if (targetMeal != null) {
            // 找到原始 meal，删除所有相同 time 和 type 的 meals（确保删除整个 meal group）
            val updatedMeals = currentMeals.filterNot { meal ->
                meal.time == targetMeal.time && meal.type == targetMeal.type
            }
            
            // 添加新的 meals（使用原始 time，保持时间一致性）
            val newMeals = mealGroup.meals.map { meal ->
                meal.copy(time = targetMeal.time) // 保持原始时间
            }
            val finalMeals = newMeals + updatedMeals
            _meals.value = finalMeals
        } else {
            // 如果找不到原始 meal（可能是新添加的），则按 time 和 type 删除
            val updatedMeals = currentMeals.filterNot { meal ->
                meal.time == mealGroup.time && meal.type == mealGroup.type
            }
            
            // 添加新的 meals
            val finalMeals = mealGroup.meals + updatedMeals
            _meals.value = finalMeals
        }
        
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
    
    // ==================== DTO ↔ Model 转换方法 ====================
    
    /**
     * 将后端DTO转换为前端Model
     */
    private fun foodRecordResponseToMealItem(dto: FoodRecordResponse): MealItem {
        return MealItem(
            id = dto.id,
            name = dto.food_name,
            calories = dto.nutrition_data.calories,
            protein = dto.nutrition_data.protein,
            carbs = dto.nutrition_data.carbohydrates,
            fat = dto.nutrition_data.fat,
            time = dto.recorded_at,
            type = mealTypeStringToEnum(dto.meal_type),
            image = null, // FoodRecordResponse没有image字段
            foodId = dto.food_id
        )
    }
    
    /**
     * 将前端Model转换为后端DTO（用于创建）
     */
    private fun mealItemToFoodRecordCreateRequest(meal: MealItem): FoodRecordCreateRequest {
        return FoodRecordCreateRequest(
            food_id = meal.foodId ?: "",
            serving_amount = 1.0, // TODO: 需要从meal中获取份量信息
            recorded_at = meal.time,
            meal_type = mealTypeEnumToString(meal.type),
            notes = null
        )
    }
    
    /**
     * 将前端Model转换为后端DTO（用于更新）
     */
    private fun mealItemToFoodRecordUpdateRequest(meal: MealItem): FoodRecordUpdateRequest {
        return FoodRecordUpdateRequest(
            food_name = meal.name,
            serving_amount = 1.0, // TODO: 需要从meal中获取份量信息
            recorded_at = meal.time,
            meal_type = mealTypeEnumToString(meal.type),
            notes = null,
            nutrition_data = com.example.forhealth.network.dto.food.NutritionData(
                calories = meal.calories,
                protein = meal.protein,
                carbohydrates = meal.carbs,
                fat = meal.fat
            )
        )
    }
    
    /**
     * 后端meal_type字符串转换为前端MealType枚举
     */
    private fun mealTypeStringToEnum(mealType: String?): MealType {
        return when (mealType) {
            "早餐" -> MealType.BREAKFAST
            "午餐" -> MealType.LUNCH
            "晚餐" -> MealType.DINNER
            "加餐" -> MealType.SNACK
            else -> MealType.BREAKFAST // 默认值
        }
    }
    
    /**
     * 前端MealType枚举转换为后端meal_type字符串
     */
    private fun mealTypeEnumToString(mealType: MealType): String {
        return when (mealType) {
            MealType.BREAKFAST -> "早餐"
            MealType.LUNCH -> "午餐"
            MealType.DINNER -> "晚餐"
            MealType.SNACK -> "加餐"
        }
    }
    
    // ==================== 从Repository加载数据的方法 ====================
    
    private val mealRepository = MealRepository()
    
    /**
     * 从后端加载今日餐食数据
     */
    fun loadTodayMeals() {
        viewModelScope.launch {
            when (val result = mealRepository.getTodayMeals()) {
                is ApiResult.Success -> {
                    val mealItems = result.data.records.map { foodRecordResponseToMealItem(it) }
                    setMeals(mealItems)
                }
                is ApiResult.Error -> {
                    // 处理错误，可以显示错误消息
                }
                is ApiResult.Loading -> {}
            }
        }
    }
    
    /**
     * 创建食物记录（添加餐食）
     */
    fun createMealRecord(meal: MealItem, onResult: (ApiResult<MealItem>) -> Unit) {
        viewModelScope.launch {
            val request = mealItemToFoodRecordCreateRequest(meal)
            when (val result = mealRepository.createFoodRecord(request)) {
                is ApiResult.Success -> {
                    val mealItem = foodRecordResponseToMealItem(result.data)
                    addMeals(listOf(mealItem))
                    onResult(ApiResult.Success(mealItem))
                }
                is ApiResult.Error -> onResult(result)
                is ApiResult.Loading -> onResult(result)
            }
        }
    }
    
    /**
     * 更新食物记录
     */
    fun updateMealRecord(recordId: String, meal: MealItem, onResult: (ApiResult<MealItem>) -> Unit) {
        viewModelScope.launch {
            val request = mealItemToFoodRecordUpdateRequest(meal)
            when (val result = mealRepository.updateFoodRecord(recordId, request)) {
                is ApiResult.Success -> {
                    val mealItem = foodRecordResponseToMealItem(result.data)
                    // 更新本地meals列表
                    val currentMeals = _meals.value ?: emptyList()
                    val updatedMeals = currentMeals.map { if (it.id == recordId) mealItem else it }
                    setMeals(updatedMeals)
                    onResult(ApiResult.Success(mealItem))
                }
                is ApiResult.Error -> onResult(result)
                is ApiResult.Loading -> onResult(result)
            }
        }
    }
    
    /**
     * 删除食物记录
     */
    fun deleteMealRecord(recordId: String, onResult: (ApiResult<Boolean>) -> Unit) {
        viewModelScope.launch {
            when (val result = mealRepository.deleteFoodRecord(recordId)) {
                is ApiResult.Success -> {
                    deleteMealGroup(recordId) // 使用现有的删除方法
                    onResult(ApiResult.Success(true))
                }
                is ApiResult.Error -> onResult(ApiResult.Error(result.message))
                is ApiResult.Loading -> onResult(result)
            }
        }
    }
}

