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
import com.example.forhealth.models.SelectedExerciseItem
import com.example.forhealth.models.TimelineItem
import com.example.forhealth.models.UserProfile
import com.example.forhealth.models.ExerciseTimelineItem
import com.example.forhealth.models.ExerciseItem
import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.dto.food.FoodRecordCreateRequest
import com.example.forhealth.network.dto.food.FoodRecordResponse
import com.example.forhealth.network.dto.food.FoodRecordUpdateRequest
import com.example.forhealth.network.dto.sports.LogSportsRequest
import com.example.forhealth.network.dto.sports.SearchSportRecordsResponse
import com.example.forhealth.network.dto.sports.SearchSportsResponse
import com.example.forhealth.network.dto.sports.UpdateSportsRecordRequest
import com.example.forhealth.models.ExerciseType
import com.example.forhealth.network.dto.visualization.*
import com.example.forhealth.repositories.MealRepository
import com.example.forhealth.repositories.ExerciseRepository
import com.example.forhealth.repositories.AiRepository
import com.example.forhealth.utils.DateUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    // 运动库（从后端获取可用运动列表）
    private val _exerciseLibrary = MutableLiveData<List<ExerciseItem>>(emptyList())
    val exerciseLibrary: LiveData<List<ExerciseItem>> = _exerciseLibrary
    
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
        
        // 更新统计数据（从meals计算）
        recalculateStats()
        
        // 更新时间线
        updateTimeline()
        
        // TODO: 更新AI建议
    }
    
    /**
     * 添加运动记录（本地方法，保留用于向后兼容）
     */
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
     * 创建运动记录（通过API）
     * 接受ActivityItem列表，内部转换为DTO并调用Repository
     */
    fun createExerciseRecords(
        activities: List<ActivityItem>,
        onResult: (ApiResult<List<ActivityItem>>) -> Unit
    ) {
        viewModelScope.launch {
            if (activities.isEmpty()) {
                onResult(ApiResult.Error("没有可保存的运动"))
                return@launch
            }
            
            for (activity in activities) {
                val request = LogSportsRequest(
                    sport_name = activity.name,
                    created_at = activity.time,
                    duration_time = activity.duration
                )
                
                when (val result = exerciseRepository.logSports(request)) {
                    is ApiResult.Success -> { /* 继续处理下一条 */ }
                    is ApiResult.Error -> {
                        onResult(ApiResult.Error(result.message))
                        return@launch
                    }
                    is ApiResult.Loading -> { /* ignore */ }
                }
            }
            
            // 重新加载今日记录以获取正确的ID和calories_burned
            loadTodayExercises()
            // 返回更新后的列表
            val updatedActivities = _exercises.value ?: emptyList()
            onResult(ApiResult.Success(updatedActivities))
        }
    }
    
    /**
     * 直接设置meals数据（从API加载时使用，替换而不是合并）
     */
    fun setMeals(meals: List<MealItem>) {
        _meals.value = meals
        
        // 更新统计数据（从meals计算）
        recalculateStats()
        
        // 更新时间线
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
        
        // 每条exercise直接作为timeline item（不再包装成WorkoutGroup）
        val exerciseItems = exercises.map { activity ->
            ExerciseTimelineItem(activity)
        }
        
        val timeline: List<TimelineItem> = 
            (mealGroups.map { MealGroupTimelineItem(it) } + exerciseItems)
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
                            val updatedMeal = foodRecordResponseToMealItemWithImage(updateResult.data)
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
            
            // 更新时间线
            updateTimeline()
        }
    }
    
    private fun recalculateStats() {
        val meals = _meals.value ?: emptyList()
        val exercises = _exercises.value ?: emptyList()
        val currentStats = _dailyStats.value ?: DailyStats.getInitial()
        
        val totalCalories = meals.sumOf { it.calories }
        val totalProtein = meals.sumOf { it.protein }
        val totalCarbs = meals.sumOf { it.carbs }
        val totalFat = meals.sumOf { it.fat }
        val totalBurned = exercises.sumOf { it.caloriesBurned }

        val newStats = DailyStats(
            calories = com.example.forhealth.models.Macro(
                current = totalCalories,
                target = currentStats.calories.target,
                unit = currentStats.calories.unit
            ),
            protein = com.example.forhealth.models.Macro(
                current = totalProtein,
                target = currentStats.protein.target,
                unit = currentStats.protein.unit
            ),
            carbs = com.example.forhealth.models.Macro(
                current = totalCarbs,
                target = currentStats.carbs.target,
                unit = currentStats.carbs.unit
            ),
            fat = com.example.forhealth.models.Macro(
                current = totalFat,
                target = currentStats.fat.target,
                unit = currentStats.fat.unit
            ),
            burned = totalBurned  // 从exercises重新计算burned
        )

        val oldValue = _dailyStats.value
        if (oldValue != null) {
            _dailyStats.postValue(null)
            _dailyStats.postValue(newStats)
        } else {
            _dailyStats.value = newStats
        }
    }
    
    /**
     * 强制重新计算统计数据（公开方法，供外部调用）
     * 用于在EditMealFragment dismiss后手动刷新数据
     */
    fun recalculateStatsForced() {
        recalculateStats()
    }
    
    // ==================== DTO ↔ Model 转换方法 ====================
    
    /**
     * 将后端DTO转换为前端Model（不包含图片）
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
     * 将后端DTO转换为前端Model，并获取食物图片URL
     * 这是一个suspend函数，用于在协程中调用
     */
    private suspend fun foodRecordResponseToMealItemWithImage(dto: FoodRecordResponse): MealItem {
        val mealItem = foodRecordResponseToMealItem(dto)
        
        // 如果有food_id，获取食物详情以获取图片URL
        if (mealItem.foodId != null && mealItem.foodId.isNotBlank()) {
            when (val foodResult = foodRepository.getFood(mealItem.foodId)) {
                is ApiResult.Success -> {
                    return mealItem.copy(image = foodResult.data.image_url)
                }
                else -> {
                    // 如果获取失败，返回不带图片的MealItem
                    return mealItem
                }
            }
        }
        
        return mealItem
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
    
    // ==================== Analytics DTO → Model 转换方法 ====================
    
    
    /**
     * 格式化日期标签
     * 根据视图类型返回合适的标签格式
     */
    private fun formatDateLabel(date: String, viewType: String): String {
        return try {
            when (viewType) {
                "day" -> {
                    // 日视图：显示 "Mon", "Tue" 等
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val dateObj = dateFormat.parse(date)
                    val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH)
                    dayFormat.format(dateObj ?: java.util.Date())
                }
                "week" -> {
                    // 周视图：显示 "W1", "W2" 等或日期
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val dateObj = dateFormat.parse(date)
                    val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH)
                    dayFormat.format(dateObj ?: java.util.Date())
                }
                "month" -> {
                    // 月视图：显示 "W1", "W2" 等
                    // 这里可以根据需要自定义格式
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val dateObj = dateFormat.parse(date)
                    val weekFormat = java.text.SimpleDateFormat("'W'w", java.util.Locale.getDefault())
                    weekFormat.format(dateObj ?: java.util.Date())
                }
                else -> date
            }
        } catch (e: Exception) {
            date // 如果解析失败，返回原始日期
        }
    }
    
    /**
     * 将每日卡路里摘要转换为DailyStats
     * 注意：这个转换只更新卡路里和消耗数据，不更新宏量营养素（protein/carbs/fat）
     * 宏量营养素需要从nutrition-analysis API获取
     */
    private fun dailyCalorieSummaryToDailyStats(
        dto: DailyCalorieSummary,
        currentStats: DailyStats
    ): DailyStats {
        return currentStats.copy(
            calories = currentStats.calories.copy(
                current = dto.total_intake,
                target = dto.daily_goal
            ),
            burned = dto.total_burned
            // 保持protein、carbs、fat不变，因为它们需要从nutrition-analysis获取
        )
    }
    
    // ==================== 从Repository加载数据的方法 ====================
    
    private val mealRepository = MealRepository()
    private val exerciseRepository = ExerciseRepository()
    private val foodRepository = com.example.forhealth.repositories.FoodRepository()
    private val userRepository = com.example.forhealth.repositories.UserRepository()
    private val visualizationRepository = com.example.forhealth.repositories.VisualizationRepository()
    private val aiRepository = AiRepository()
    
    // 存储用户资料DTO（用于获取username）
    private val _userProfileResponse = MutableLiveData<com.example.forhealth.network.dto.user.UserProfileResponse?>()
    val userProfileResponse: LiveData<com.example.forhealth.network.dto.user.UserProfileResponse?> = _userProfileResponse
    
   
    // 存储宏量营养素比例Model（用于饼图显示）
    private val _macroRatio = MutableLiveData<com.example.forhealth.models.MacroRatio>(com.example.forhealth.models.MacroRatio.getInitial())
    val macroRatio: LiveData<com.example.forhealth.models.MacroRatio> = _macroRatio
    
    // 存储活动图表数据Model（用于日柱状图和周/月趋势图）
    private val _activityChartData = MutableLiveData<com.example.forhealth.models.ActivityChartData>(com.example.forhealth.models.ActivityChartData.getInitial())
    val activityChartData: LiveData<com.example.forhealth.models.ActivityChartData> = _activityChartData
    
    /**
     * 从后端加载今日餐食数据
     * 对于每个食物记录，如果food_id存在，则获取食物详情以获取图片URL
     */
    fun loadTodayMeals() {
        viewModelScope.launch {
            when (val result = mealRepository.getTodayMeals()) {
                is ApiResult.Success -> {
                    // 先转换为MealItem（此时image为null）
                    val mealItemsWithoutImages = result.data.records.map { foodRecordResponseToMealItem(it) }
                    
                    // 对于每个有food_id的MealItem，并行获取食物详情以获取图片URL
                    val mealItemsWithImages = mealItemsWithoutImages.map { mealItem ->
                        if (mealItem.foodId != null && mealItem.foodId.isNotBlank()) {
                            // 使用async并行获取食物详情
                            async {
                                when (val foodResult = foodRepository.getFood(mealItem.foodId)) {
                                    is ApiResult.Success -> {
                                        // 更新MealItem的image字段
                                        mealItem.copy(image = foodResult.data.image_url)
                                    }
                                    else -> mealItem // 如果获取失败，保持原样
                                }
                            }
                        } else {
                            async { mealItem }
                        }
                    }.awaitAll()
                    
                    setMeals(mealItemsWithImages)
                }
                is ApiResult.Error -> {
                    // 处理错误，可以显示错误消息
                }
                is ApiResult.Loading -> {}
            }
        }
    }
    
    /**
     * 从后端加载用户资料
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            when (val result = userRepository.getProfile()) {
                is ApiResult.Success -> {
                    // 保存完整的UserProfileResponse（用于获取username等字段）
                    _userProfileResponse.value = result.data
                    
                    // 将UserProfileResponse转换为UserProfile Model
                    // 注意：UserProfile Model只有简单字段，将username映射到name
                    val userProfile = com.example.forhealth.models.UserProfile(
                        name = result.data.username ?: "User",
                        age = result.data.age ?: 25,
                        height = result.data.height?.toInt() ?: 170,
                        weight = result.data.weight?.toInt() ?: 70,
                        gender = when (result.data.gender) {
                            "male" -> "Male"
                            "female" -> "Female"
                            else -> "Male"
                        },
                        activityLevel = result.data.activity_level ?: "Moderate"
                    )
                    _userProfile.value = userProfile
                }
                is ApiResult.Error -> {
                    // 处理错误，可以显示错误消息
                }
                is ApiResult.Loading -> {}
            }
        }
    }
    
    /**
     * 获取今天的日期字符串（YYYY-MM-DD）
     */
    private fun getTodayDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }
    
    /**
     * 从后端加载今日统计数据（卡路里和宏量营养素）
     * 用于初始化主页圆环和宏量营养素显示
     */
    fun loadTodayStats() {
        viewModelScope.launch {
            val today = getTodayDateString()
            
            // 并行加载卡路里摘要和营养素分析
            val calorieSummaryDeferred = async {
                visualizationRepository.getDailyCalorieSummary(today)
            }
            val nutritionAnalysisDeferred = async {
                visualizationRepository.getNutritionAnalysis(today, today)
            }
            
            // 等待两个API都完成
            val calorieSummary = calorieSummaryDeferred.await()
            val nutritionAnalysis = nutritionAnalysisDeferred.await()
            
            // 合并数据更新DailyStats
            val currentStats = _dailyStats.value ?: DailyStats.getInitial()
            var updatedStats = currentStats
            
            // 更新卡路里和消耗数据
            val localBurned = (_exercises.value ?: emptyList()).sumOf { it.caloriesBurned }
            
            when (calorieSummary) {
                is ApiResult.Success -> {
                    val burnedValue = if (localBurned > 0) localBurned else calorieSummary.data.total_burned
                    
                    updatedStats = updatedStats.copy(
                        calories = updatedStats.calories.copy(
                            current = calorieSummary.data.total_intake,
                            target = calorieSummary.data.daily_goal
                        ),
                        burned = burnedValue
                    )
                }
                else -> {
                    // 如果API失败，使用本地计算的数据（从exercises）
                    if (localBurned > 0) {
                        updatedStats = updatedStats.copy(burned = localBurned)
                    }
                }
            }
            
            // 更新宏量营养素数据
            when (nutritionAnalysis) {
                is ApiResult.Success -> {
                    val nutritionData = nutritionAnalysis.data
                    
                    // 在ViewModel层将DTO转换为Model并更新
                    val caloriesData = nutritionData.nutrition_vs_recommended.find {
                        it.nutrient_name.lowercase() == "calories"
                    }
                    val totalCalories = caloriesData?.actual ?: 0.0
                    
                    val macroRatio = com.example.forhealth.models.MacroRatio(
                        proteinPercent = nutritionData.macronutrient_ratio.protein,
                        carbohydratesPercent = nutritionData.macronutrient_ratio.carbohydrates,
                        fatPercent = nutritionData.macronutrient_ratio.fat,
                        totalCalories = totalCalories
                    )
                    _macroRatio.value = macroRatio
                    
                    // 从nutrition_vs_recommended中提取宏量营养素数据
                    val proteinData = nutritionData.nutrition_vs_recommended.find {
                        it.nutrient_name.lowercase() == "protein"
                    }
                    val carbsData = nutritionData.nutrition_vs_recommended.find {
                        it.nutrient_name.lowercase() == "carbohydrates"
                    }
                    val fatData = nutritionData.nutrition_vs_recommended.find {
                        it.nutrient_name.lowercase() == "fat"
                    }
                    
                    updatedStats = updatedStats.copy(
                        protein = updatedStats.protein.copy(
                            current = proteinData?.actual ?: updatedStats.protein.current,
                            target = proteinData?.recommended ?: updatedStats.protein.target
                        ),
                        carbs = updatedStats.carbs.copy(
                            current = carbsData?.actual ?: updatedStats.carbs.current,
                            target = carbsData?.recommended ?: updatedStats.carbs.target
                        ),
                        fat = updatedStats.fat.copy(
                            current = fatData?.actual ?: updatedStats.fat.current,
                            target = fatData?.recommended ?: updatedStats.fat.target
                        )
                    )
                }
                else -> {
                    // 如果API失败，使用本地计算的数据（从meals）
                    // 这里不更新，保持当前状态
                }
            }
            
            _dailyStats.value = updatedStats
        }
    }
    
    /**
     * 从后端加载营养素分析（用于Analytics饼图）
     * 通过Repository层获取DTO，然后在ViewModel层转换为Model
     * @param startDate 开始日期(YYYY-MM-DD)
     * @param endDate 结束日期(YYYY-MM-DD)
     * @param totalIntake 总摄入卡路里（可选，用于计算宏量营养素的实际卡路里值）
     */
    fun loadNutritionAnalysis(
        startDate: String,
        endDate: String,
        totalIntake: Double? = null
    ) {
        viewModelScope.launch {
            when (val result = visualizationRepository.getNutritionAnalysis(startDate, endDate)) {
                is ApiResult.Success -> {
                    // 在ViewModel层将DTO转换为Model
                    val dto = result.data
                    // 从nutrition_vs_recommended中获取总卡路里（虽然后端可能不返回calories项，但保留此逻辑）
                    val caloriesData = dto.nutrition_vs_recommended.find {
                        it.nutrient_name.lowercase() == "calories"
                    }
                    val totalCalories = caloriesData?.actual ?: 0.0
                    
                    // 从macronutrient_ratio获取百分比并创建Model
                    val macroRatio = com.example.forhealth.models.MacroRatio(
                        proteinPercent = dto.macronutrient_ratio.protein,
                        carbohydratesPercent = dto.macronutrient_ratio.carbohydrates,
                        fatPercent = dto.macronutrient_ratio.fat,
                        totalCalories = totalCalories
                    )
                    _macroRatio.value = macroRatio
                }
                is ApiResult.Error -> {
                    // 处理错误，设为初始值（不使用本地数据）
                    _macroRatio.value = com.example.forhealth.models.MacroRatio.getInitial()
                }
                is ApiResult.Loading -> {}
            }
        }
    }
    
    /**
     * 从后端加载时间序列趋势（用于Analytics周/月视图折线图）
     * @param startDate 开始日期(YYYY-MM-DD)
     * @param endDate 结束日期(YYYY-MM-DD)
     * @param viewType 视图类型: "day", "week", "month"（默认"day"）
     */
    fun loadTimeSeriesTrend(
        startDate: String,
        endDate: String,
        viewType: String = "day"
    ) {
        viewModelScope.launch {
            when (val result = visualizationRepository.getTimeSeriesTrend(startDate, endDate, viewType)) {
                is ApiResult.Success -> {
                    // 在ViewModel层将DTO转换为Model
                    val dto = result.data
                    // 创建日期到摄入值的映射
                    val intakeMap = dto.intake_trend.associateBy { it.date }
                    // 创建日期到消耗值的映射
                    val burnedMap = dto.burned_trend.associateBy { it.date }
                    
                    // 获取所有唯一的日期
                    val allDates = (dto.intake_trend.map { it.date } + dto.burned_trend.map { it.date }).distinct().sorted()
                    
                    // 为每个日期创建数据点
                    val dataPoints = allDates.map { date ->
                        val intake = intakeMap[date]?.value ?: 0.0
                        val burned = burnedMap[date]?.value ?: 0.0
                        
                        // 格式化日期标签
                        val label = formatDateLabel(date, dto.view_type)
                        
                        com.example.forhealth.models.ActivityChartDataPoint(
                            label = label,
                            intake = intake,
                            burned = burned
                        )
                    }
                    
                    val chartData = com.example.forhealth.models.ActivityChartData(dataPoints)
                    _activityChartData.value = chartData
                }
                is ApiResult.Error -> {
                    // 处理错误，设为初始值（不使用本地数据）
                    _activityChartData.value = com.example.forhealth.models.ActivityChartData.getInitial()
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
                    val mealItem = foodRecordResponseToMealItemWithImage(result.data)
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
                        val mealItem = foodRecordResponseToMealItemWithImage(result.data)
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
                    val mealItem = foodRecordResponseToMealItemWithImage(result.data)
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
            
            // 获取所有记录的ID
            val mealIds = mealGroupRecords.mapNotNull { it.id }.filter { it.isNotBlank() }
            deleteMealRecords(mealIds, onResult)
        }
    }
    
    /**
     * 批量删除食物记录（通过记录ID列表）
     */
    fun deleteMealRecords(recordIds: List<String>, onResult: (ApiResult<Boolean>) -> Unit) {
        viewModelScope.launch {
            if (recordIds.isEmpty()) {
                onResult(ApiResult.Success(true))
                return@launch
            }
            
            // 逐个删除所有记录
            var hasError = false
            var errorMessage = ""
            for (recordId in recordIds) {
                when (val result = mealRepository.deleteFoodRecord(recordId)) {
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
                // 更新本地状态：删除所有已删除的记录
                val currentMeals = _meals.value ?: emptyList()
                val updatedMeals = currentMeals.filterNot { meal ->
                    recordIds.contains(meal.id)
                }
                setMeals(updatedMeals)
                onResult(ApiResult.Success(true))
            }
        }
    }

    /**
     * 更新运动记录（通过API）
     * 接受ActivityItem，内部转换为DTO并调用Repository
     */
    fun updateExerciseRecord(
        activity: ActivityItem,
        onResult: (ApiResult<ActivityItem>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(ApiResult.Loading)
            
            val request = UpdateSportsRecordRequest(
                record_id = activity.id,
                old_sport_name = activity.name,
                new_sport_name = activity.name,
                created_at = activity.time,
                duration_time = activity.duration
            )
            
            when (val result = exerciseRepository.updateSportRecord(request)) {
                is ApiResult.Success -> {
                    // 立即更新本地状态（同步 LiveData）
                    val currentExercises = _exercises.value ?: emptyList()
                    val updatedExercises = currentExercises.map { exercise ->
                        if (exercise.id == activity.id) {
                            // 更新匹配的记录
                            activity
                        } else {
                            exercise
                        }
                    }
                    setExercises(updatedExercises)
                    
                    // 异步重新加载今日记录以确保数据同步
                    loadTodayExercises()
                    // 重新加载每日卡路里摘要以更新圆环
                    loadDailyCalorieSummary(null)

                    // 更新时间线
                    updateTimeline()
                    
                    onResult(ApiResult.Success(activity))
                }
                is ApiResult.Error -> onResult(ApiResult.Error(result.message))
                is ApiResult.Loading -> onResult(result)
            }
        }
    }
    
    /**
     * 删除运动记录（通过API）
     */
    fun deleteExerciseRecord(recordId: String, onResult: (ApiResult<Boolean>) -> Unit) {
        viewModelScope.launch {
            when (val result = exerciseRepository.deleteSportRecord(recordId)) {
                is ApiResult.Success -> {
                    // 立即同步更新本地状态（更新 LiveData，触发UI更新）
                    val currentExercises = _exercises.value ?: emptyList()
                    val updatedExercises = currentExercises.filterNot { it.id == recordId }
                    
                    // setExercises 会更新 _exercises.value，同时会调用 updateTimeline()
                    // 这会立即触发 UI 更新（通过 timelineItems LiveData 的观察者）
                    setExercises(updatedExercises)
                    
                    // 异步重新加载今日记录以确保与后端数据完全同步
                    loadTodayExercises()
                    // 重新加载每日卡路里摘要以更新圆环和统计数据
                    loadDailyCalorieSummary(null)
                    
                    onResult(ApiResult.Success(true))
                }
                is ApiResult.Error -> onResult(ApiResult.Error(result.message))
                is ApiResult.Loading -> onResult(ApiResult.Loading)
            }
        }
    }

    /**
     * 加载今日运动记录（从后端）
     */
    fun loadTodayExercises() {
        viewModelScope.launch {
            when (val result = exerciseRepository.getTodaySportsRecords()) {
                is ApiResult.Success -> {
                    val activities = result.data.map { searchSportRecordResponseToActivityItemWithImage(it) }
                    setExercises(activities)
                }
                is ApiResult.Error -> {
                    // 处理错误，可以显示错误消息
                }
                is ApiResult.Loading -> {}
            }
        }
    }
    
    /**
     * 重新拉取运动记录（向后兼容方法）
     */
    fun loadExerciseRecords() {
        loadTodayExercises()
    }

    /**
     * 加载可用运动列表（运动库），供选择使用
     */
    fun loadExerciseLibrary(onResult: (ApiResult<List<ExerciseItem>>) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = exerciseRepository.getAvailableSportsTypes()) {
                is ApiResult.Success -> {
                    val items = result.data.map { searchSportsResponseToExerciseItem(it) }
                    _exerciseLibrary.value = items
                    onResult(ApiResult.Success(items))
                }
                is ApiResult.Error -> onResult(ApiResult.Error(result.message))
                is ApiResult.Loading -> onResult(result)
            }
        }
    }
    
    // ==================== DTO ↔ Model 转换方法（运动） ====================
    
    /**
     * 将后端DTO转换为前端Model
     */
    private fun searchSportRecordResponseToActivityItem(dto: SearchSportRecordsResponse): ActivityItem {
        val recordId = dto.record_id ?:""
        val sportName = dto.sport_name ?: ""
        val duration = dto.duration_time ?: 0
        val caloriesBurned = dto.calories_burned ?: 0.0
        val createdAt = dto.created_at ?: DateUtils.getCurrentTime()
        val sportType = dto.sport_type?.let { sportTypeStringToEnum(it) } ?: ExerciseType.CARDIO
        
        return ActivityItem(
            id = recordId,
            name = sportName,
            caloriesBurned = caloriesBurned,
            duration = duration,
            time = createdAt,
            type = sportType,
            image = null // SearchSportRecordsResponse没有image字段
        )
    }
    /**
     * 将后端DTO转换为前端Model，并获取运动图片URL
     * 这是一个suspend函数，用于在协程中调用
     */
    private suspend fun searchSportRecordResponseToActivityItemWithImage(dto: SearchSportRecordsResponse): ActivityItem {
        val activityItem = searchSportRecordResponseToActivityItem(dto)
        
        // 根据运动名称从运动库中查找对应的图片URL
        when (val sportResult = exerciseRepository.getAvailableSportsTypes()) {
            is ApiResult.Success -> {
                val sport = sportResult.data.find { it.sport_name == activityItem.name }
                if (sport != null && !sport.image_url.isNullOrBlank()) {
                    return activityItem.copy(image = sport.image_url)
                }
            }
            else -> {
                // 如果获取失败，返回不带图片的ActivityItem
            }
        }
        
        return activityItem
    }

    /**
     * 将运动类型DTO转换为前端ExerciseItem（用于选择列表）
     */
    private fun searchSportsResponseToExerciseItem(dto: SearchSportsResponse): ExerciseItem {
        val id = dto.sport_name ?: UUID.randomUUID().toString()
        val name = dto.sport_name ?: "Sport"
        val category = dto.sport_type?.let { sportTypeStringToEnum(it) } ?: ExerciseType.CARDIO
        val mets = dto.METs ?: 5.0
        // 将METs近似转换为每分钟消耗（假设70kg，公式：MET * 3.5 * 70 / 200）
        val caloriesPerMin = mets * 3.5 * 70 / 200.0
        val image = dto.image_url ?: ""
        return ExerciseItem(
            id = id,
            name = name,
            caloriesPerUnit = caloriesPerMin,
            unit = "min",
            image = image,
            category = category
        )
    }
    
    /**
     * 后端sport_type字符串转换为前端ExerciseType枚举
     */
    private fun sportTypeStringToEnum(sportType: String): ExerciseType {
        return when (sportType.uppercase()) {
            "CARDIO" -> ExerciseType.CARDIO
            "STRENGTH" -> ExerciseType.STRENGTH
            "FLEXIBILITY" -> ExerciseType.FLEXIBILITY
            "SPORTS" -> ExerciseType.SPORTS
            else -> ExerciseType.CARDIO // 默认值
        }
    }
    
    /**
     * 前端ExerciseType枚举转换为后端sport_type字符串
     */
    private fun exerciseTypeEnumToString(exerciseType: ExerciseType): String {
        return when (exerciseType) {
            ExerciseType.CARDIO -> "CARDIO"
            ExerciseType.STRENGTH -> "STRENGTH"
            ExerciseType.FLEXIBILITY -> "FLEXIBILITY"
            ExerciseType.SPORTS -> "SPORTS"
        }
    }
    
    // ==================== AI 助手相关方法 ====================
    
    /**
     * 健康知识问答
     * 通过Repository层获取DTO，然后在ViewModel层转换为Model（String）
     * @param question 用户问题
     * @param context 可选上下文信息
     * @param onResult 回调函数，返回答案字符串
     */
    fun askQuestion(
        question: String,
        context: Map<String, Any>? = null,
        onResult: (ApiResult<String>) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = aiRepository.askQuestion(question, context)) {
                is ApiResult.Success -> {
                    // 将DTO转换为Model（String）
                    val answer = result.data.answer
                    onResult(ApiResult.Success(answer))
                }
                is ApiResult.Error -> {
                    onResult(result)
                }
                is ApiResult.Loading -> {
                    onResult(result)
                }
            }
        }
    }
    
    /**
     * 刷新AI Insight
     * 根据当前时间概率选择调用 analyzeDiet 或 recommendMeal
     * 通过Repository层获取DTO，然后在ViewModel层转换为Model（String）
     */
    fun refreshAiInsight() {
        viewModelScope.launch {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            
            // 根据时间计算概率
            val shouldRecommendMeal = when {
                // 饭点时间（早6点、12点、晚6点附近 ±1小时）：提高推荐概率到80%
                currentHour in 5..7 || currentHour in 11..13 || currentHour in 17..19 -> {
                    Math.random() < 0.8
                }
                // 睡前时间（晚9-11点）：提高分析概率到80%
                currentHour in 21..23 -> {
                    Math.random() < 0.2
                }
                // 其他时间：50%概率
                else -> {
                    Math.random() < 0.5
                }
            }
            
            if (shouldRecommendMeal) {
                // 调用菜式推荐
                when (val result = aiRepository.recommendMeal()) {
                    is ApiResult.Success -> {
                        // 将DTO转换为Model（String）
                        val message = result.data.message
                        _aiSuggestion.value = message
                    }
                    is ApiResult.Error -> {
                        _aiSuggestion.value = "Unable to get meal recommendation. Please try again later."
                    }
                    is ApiResult.Loading -> {}
                }
            } else {
                // 调用饮食分析
                when (val result = aiRepository.analyzeDiet(days = 7)) {
                    is ApiResult.Success -> {
                        // 将DTO转换为Model（String）
                        val message = result.data.message
                        _aiSuggestion.value = message
                    }
                    is ApiResult.Error -> {
                        _aiSuggestion.value = "Unable to analyze diet. Please try again later."
                    }
                    is ApiResult.Loading -> {}
                }
            }
        }
    }
}

