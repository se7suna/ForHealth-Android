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
import com.example.forhealth.models.SelectedFoodItem
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
        
        // 先合并新添加的meals中相同名称、相同时间戳的（不再考虑type）
        val newMealsGrouped = meals.groupBy { "${it.name}_${it.time}" }
            .map { (_, mealList) ->
                if (mealList.size == 1) {
                    mealList.first()
                } else {
                    // 合并相同名称、时间戳的meal
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
        // 现在只按时间戳和名称合并，不再考虑meal_type
        newMealsGrouped.forEach { newMeal ->
            // 先尝试找相同名称、相同时间戳的meal（精确匹配）
            var existingIndex = resultMeals.indexOfFirst { 
                it.name == newMeal.name && 
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
                // 没有找到精确匹配，直接添加
                resultMeals.add(newMeal)
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
        
        // 按时间戳分组：相同时间戳的meal归为一组（精确到秒）
        val mealGroups = meals.groupBy { it.time }
            .map { (timestamp, mealList) ->
                // 使用该时间戳中第一个meal的type作为显示类型（用于UI显示）
                val mealType = mealList.firstOrNull()?.type ?: MealType.BREAKFAST
                MealGroup(
                    id = mealList.firstOrNull()?.id ?: "", // 使用第一个meal的id作为group id
                    meals = mealList,
                    time = timestamp,
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
        // 这个方法保留用于向后兼容，但实际应该使用updateMealGroupRecords
        val currentMeals = _meals.value ?: emptyList()
        
        // 通过 MealGroup 的 id 找到对应的原始 meal
        // MealGroup 的 id 是第一个 meal 的 id，所以通过 id 找到对应的 meal
        val targetMeal = currentMeals.find { it.id == mealGroup.id }
        
        if (targetMeal != null) {
            // 找到原始 meal，删除所有相同时间戳的 meals（确保删除整个 meal group）
            val updatedMeals = currentMeals.filterNot { meal ->
                meal.time == targetMeal.time
            }
            
            // 添加新的 meals（使用原始 time，保持时间一致性）
            val newMeals = mealGroup.meals.map { meal ->
                meal.copy(time = targetMeal.time) // 保持原始时间戳
            }
            val finalMeals = newMeals + updatedMeals
            _meals.value = finalMeals
        } else {
            // 如果找不到原始 meal（可能是新添加的），则按时间戳删除
            val updatedMeals = currentMeals.filterNot { meal ->
                meal.time == mealGroup.time
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
    
    /**
     * 更新餐食组记录（通过API）
     * 比较原始MealGroup和新的MealGroup，执行更新/删除/创建操作
     */
    fun updateMealGroupRecords(
        originalMealGroup: MealGroup?,
        newMealGroup: MealGroup,
        selectedItems: List<SelectedFoodItem>,
        onResult: (ApiResult<MealGroup>) -> Unit
    ) {
        viewModelScope.launch {
            if (originalMealGroup == null) {
                // 如果没有原始数据，直接创建新记录
                createMealRecords(
                    items = selectedItems,
                    mealType = newMealGroup.type,
                    time = newMealGroup.time
                ) { result ->
                    when (result) {
                        is ApiResult.Success<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val mealItems = result.data as List<MealItem>
                            val mealGroup = MealGroup(
                                id = mealItems.firstOrNull()?.id ?: "",
                                meals = mealItems,
                                time = newMealGroup.time,
                                type = newMealGroup.type
                            )
                            onResult(ApiResult.Success(mealGroup))
                        }
                        is ApiResult.Error -> onResult(ApiResult.Error(result.message))
                        is ApiResult.Loading -> onResult(result)
                    }
                }
                return@launch
            }
            
            // 获取原始meal group中的所有记录ID
            val originalMealIds = originalMealGroup.meals.mapNotNull { it.id }.filter { it.isNotBlank() }
            val newMealCount = selectedItems.size
            
            // 策略：
            // 1. 如果新记录数量 <= 原始记录数量，更新前N条，删除剩余的
            // 2. 如果新记录数量 > 原始记录数量，更新所有原始记录，创建新的记录
            
            val updatedMeals = mutableListOf<MealItem>()
            var updateIndex = 0
            var hasError = false
            var errorMessage = ""
            
            // 先更新现有记录
            for (i in originalMealIds.indices) {
                if (updateIndex >= selectedItems.size) {
                    // 新记录数量少于原始记录，删除剩余的
                    when (val deleteResult = mealRepository.deleteFoodRecord(originalMealIds[i])) {
                        is ApiResult.Success -> { /* 删除成功 */ }
                        is ApiResult.Error -> {
                            hasError = true
                            errorMessage = deleteResult.message
                            break
                        }
                        is ApiResult.Loading -> {}
                    }
                } else {
                    // 更新现有记录
                    val item = selectedItems[updateIndex]
                    val macros = calculateMacrosFromSelectedItem(item)
                    val mealItem = MealItem(
                        id = originalMealIds[i],
                        name = item.foodItem.name,
                        calories = macros.calories,
                        protein = macros.protein,
                        carbs = macros.carbs,
                        fat = macros.fat,
                        time = newMealGroup.time,
                        type = newMealGroup.type,
                        image = item.foodItem.image,
                        foodId = item.foodItem.id,
                        servingAmount = if (item.mode == com.example.forhealth.models.QuantityMode.GRAM) {
                            item.count / item.foodItem.gramsPerUnit
                        } else {
                            item.count
                        }
                    )
                    
                    val request = mealItemToFoodRecordUpdateRequest(mealItem)
                    when (val updateResult = mealRepository.updateFoodRecord(originalMealIds[i], request)) {
                        is ApiResult.Success -> {
                            val updatedMeal = foodRecordResponseToMealItem(updateResult.data)
                            updatedMeals.add(updatedMeal)
                        }
                        is ApiResult.Error -> {
                            hasError = true
                            errorMessage = updateResult.message
                            break
                        }
                        is ApiResult.Loading -> {}
                    }
                    updateIndex++
                }
            }
            
            if (hasError) {
                onResult(ApiResult.Error(errorMessage))
                return@launch
            }
            
            // 创建新记录（如果新记录数量 > 原始记录数量）
            if (updateIndex < selectedItems.size) {
                val remainingItems = selectedItems.subList(updateIndex, selectedItems.size)
                // 先保存当前meals状态，因为createMealRecords会自动更新
                val currentMealsBeforeCreate = _meals.value ?: emptyList()
                val targetMeal = currentMealsBeforeCreate.find { it.id == originalMealGroup.id }
                
                createMealRecords(
                    items = remainingItems,
                    mealType = newMealGroup.type,
                    time = newMealGroup.time
                ) { result ->
                    when (result) {
                        is ApiResult.Success<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val newMeals = result.data as List<MealItem>
                            updatedMeals.addAll(newMeals)
                            val mealGroup = MealGroup(
                                id = updatedMeals.firstOrNull()?.id ?: "",
                                meals = updatedMeals,
                                time = newMealGroup.time,
                                type = newMealGroup.type
                            )
                            // 更新本地状态：移除旧的meal group和createMealRecords自动添加的新meals，然后添加完整的updatedMeals
                            val currentMealsAfterCreate = _meals.value ?: emptyList()
                            val updatedMealsList = if (targetMeal != null) {
                                // 移除旧的meal group（按时间戳）和自动添加的新meals
                                currentMealsAfterCreate.filterNot { meal ->
                                    meal.time == targetMeal.time ||
                                    newMeals.any { it.id == meal.id }
                                } + updatedMeals
                            } else {
                                // 移除自动添加的新meals，然后添加完整的updatedMeals
                                currentMealsAfterCreate.filterNot { meal ->
                                    newMeals.any { it.id == meal.id }
                                } + updatedMeals
                            }
                            setMeals(updatedMealsList)
                            onResult(ApiResult.Success(mealGroup))
                        }
                        is ApiResult.Error -> onResult(ApiResult.Error(result.message))
                        is ApiResult.Loading -> onResult(result)
                    }
                }
            } else {
                // 所有记录都已更新
                val mealGroup = MealGroup(
                    id = updatedMeals.firstOrNull()?.id ?: "",
                    meals = updatedMeals,
                    time = newMealGroup.time,
                    type = newMealGroup.type
                )
                // 更新本地状态
                val currentMeals = _meals.value ?: emptyList()
                val targetMeal = currentMeals.find { it.id == originalMealGroup.id }
                val updatedMealsList = if (targetMeal != null) {
                    currentMeals.filterNot { meal ->
                        meal.time == targetMeal.time
                    } + updatedMeals
                } else {
                    currentMeals + updatedMeals
                }
                setMeals(updatedMealsList)
                onResult(ApiResult.Success(mealGroup))
            }
        }
    }
    
    private fun calculateMacrosFromSelectedItem(item: SelectedFoodItem): MacroResult {
        val ratio = if (item.mode == com.example.forhealth.models.QuantityMode.GRAM) {
            item.count / item.foodItem.gramsPerUnit
        } else {
            item.count
        }
        return MacroResult(
            calories = item.foodItem.calories * ratio,
            protein = item.foodItem.protein * ratio,
            carbs = item.foodItem.carbs * ratio,
            fat = item.foodItem.fat * ratio
        )
    }
    
    private data class MacroResult(
        val calories: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double
    )
    
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
        // 这个方法保留用于向后兼容，但实际应该使用deleteMealRecord
        val currentMeals = _meals.value ?: emptyList()
        
        // 找到对应的meal group并删除所有meals
        // 通过mealGroupId找到对应的meal，然后通过时间戳找到同一组的所有meals
        val targetMeal = currentMeals.find { it.id == mealGroupId }
        if (targetMeal != null) {
            val updatedMeals = currentMeals.filterNot { meal ->
                meal.time == targetMeal.time
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
            foodId = dto.food_id,
            servingAmount = dto.serving_amount
        )
    }
    
    /**
     * 将前端Model转换为后端DTO（用于创建）
     */
    private fun mealItemToFoodRecordCreateRequest(meal: MealItem): FoodRecordCreateRequest {
        return FoodRecordCreateRequest(
            food_id = meal.foodId ?: "",
            serving_amount = meal.servingAmount ?: 1.0,
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
            serving_amount = meal.servingAmount ?: 1.0,
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
     * 批量创建食物记录（逐条上传）
     */
    fun createMealRecords(
        items: List<SelectedFoodItem>,
        mealType: MealType,
        time: String,
        onResult: (ApiResult<List<MealItem>>) -> Unit
    ) {
        viewModelScope.launch {
            if (items.isEmpty()) {
                onResult(ApiResult.Error("没有可保存的餐食"))
                return@launch
            }
            
            val createdMeals = mutableListOf<MealItem>()
            for (item in items) {
                val foodId = item.foodItem.id
                if (foodId.isBlank()) {
                    onResult(ApiResult.Error("食物ID缺失，无法保存：${item.foodItem.name}"))
                    return@launch
                }
                
                val request = selectedFoodItemToCreateRequest(
                    item = item,
                    recordedAt = time,
                    mealTypeString = mealTypeEnumToString(mealType)
                )
                
                when (val result = mealRepository.createFoodRecord(request)) {
                    is ApiResult.Success -> {
                        val mealItem = foodRecordResponseToMealItem(result.data)
                        createdMeals.add(mealItem)
                    }
                    is ApiResult.Error -> {
                        onResult(ApiResult.Error(result.message))
                        return@launch
                    }
                    is ApiResult.Loading -> { /* ignore */ }
                }
            }
            
            // 本地状态更新
            addMeals(createdMeals)
            onResult(ApiResult.Success(createdMeals))
        }
    }

    private fun selectedFoodItemToCreateRequest(
        item: SelectedFoodItem,
        recordedAt: String,
        mealTypeString: String
    ): FoodRecordCreateRequest {
        val servingAmount = if (item.mode == com.example.forhealth.models.QuantityMode.GRAM) {
            // grams / gramsPerUnit
            item.count / item.foodItem.gramsPerUnit
        } else {
            item.count
        }
        return FoodRecordCreateRequest(
            food_id = item.foodItem.id,
            serving_amount = servingAmount,
            recorded_at = recordedAt,
            meal_type = mealTypeString,
            notes = null
        )
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
     * 删除食物记录（单个记录或同一时间戳的所有记录）
     * 现在按时间戳分组，所以删除一条记录会删除同一时间戳的所有记录
     */
    fun deleteMealRecord(recordId: String, onResult: (ApiResult<Boolean>) -> Unit) {
        viewModelScope.launch {
            // 先找到该记录对应的meal group（通过时间戳）
            val currentMeals = _meals.value ?: emptyList()
            val targetMeal = currentMeals.find { it.id == recordId }
            
            if (targetMeal == null) {
                // 如果找不到记录，可能已经被删除了，或者记录ID无效
                // 尝试通过时间戳查找（如果originalMealGroup有时间戳信息）
                // 如果还是找不到，返回成功（可能记录已经不存在了）
                onResult(ApiResult.Success(true))
                return@launch
            }
            
            // 找到同一时间戳的所有记录（相同time）
            val mealGroupRecords = currentMeals.filter { 
                it.time == targetMeal.time
            }
            
            if (mealGroupRecords.isEmpty()) {
                // 如果没有找到记录，直接返回成功
                onResult(ApiResult.Success(true))
                return@launch
            }
            
            // 逐个删除所有记录
            var hasError = false
            var errorMessage = ""
            for (meal in mealGroupRecords) {
                // 确保meal.id不为空
                if (meal.id.isBlank()) {
                    continue
                }
                when (val result = mealRepository.deleteFoodRecord(meal.id)) {
                    is ApiResult.Success -> { /* 删除成功 */ }
                    is ApiResult.Error -> {
                        hasError = true
                        errorMessage = result.message
                        break
                    }
                    is ApiResult.Loading -> {}
                }
            }
            
            if (hasError) {
                onResult(ApiResult.Error(errorMessage))
            } else {
                // 更新本地状态
                deleteMealGroup(recordId) // 使用现有的删除方法更新本地状态
                onResult(ApiResult.Success(true))
            }
        }
    }
}

