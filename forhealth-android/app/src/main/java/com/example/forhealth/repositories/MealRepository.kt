package com.example.forhealth.repositories

import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.food.*
import com.example.forhealth.network.safeApiCall
import java.text.SimpleDateFormat
import java.util.*

/**
 * 餐食数据仓库
 * 负责餐食记录的增删改查（只返回DTO，不进行转换）
 * 注意：餐食记录实际上就是食物记录（FoodRecord），所以使用FoodRepository的方法
 */
class MealRepository {
    
    private val foodRepository = FoodRepository()
    
    /**
     * 获取今日餐食列表（通过食物记录API）
     */
    suspend fun getTodayMeals(): ApiResult<FoodRecordListResponse> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        return foodRepository.getFoodRecords(
            startDate = today,
            endDate = today,
            limit = 500
        )
    }
    
    /**
     * 获取指定日期范围的餐食列表
     */
    suspend fun getMeals(
        startDate: String? = null,
        endDate: String? = null,
        mealType: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): ApiResult<FoodRecordListResponse> {
        return foodRepository.getFoodRecords(startDate, endDate, mealType, limit, offset)
    }
    
    /**
     * 创建食物记录（添加餐食）
     */
    suspend fun createFoodRecord(
        request: FoodRecordCreateRequest
    ): ApiResult<FoodRecordResponse> {
        return foodRepository.createFoodRecord(request)
    }
    
    /**
     * 批量创建食物记录（批量添加餐食）
     */
    suspend fun createFoodRecords(
        requests: List<FoodRecordCreateRequest>
    ): ApiResult<List<FoodRecordResponse>> {
        // 后端不支持批量创建，需要逐个创建
        val results = mutableListOf<FoodRecordResponse>()
        for (request in requests) {
            when (val result = foodRepository.createFoodRecord(request)) {
                is ApiResult.Success -> results.add(result.data)
                is ApiResult.Error -> return result
                is ApiResult.Loading -> {}
            }
        }
        return ApiResult.Success(results)
    }
    
    /**
     * 更新食物记录
     */
    suspend fun updateFoodRecord(
        recordId: String,
        request: FoodRecordUpdateRequest
    ): ApiResult<FoodRecordResponse> {
        return foodRepository.updateFoodRecord(recordId, request)
    }
    
    /**
     * 删除食物记录
     */
    suspend fun deleteFoodRecord(
        recordId: String
    ): ApiResult<com.example.forhealth.network.dto.auth.MessageResponse> {
        return foodRepository.deleteFoodRecord(recordId)
    }
    
    /**
     * 获取某日的营养摘要
     */
    suspend fun getDailyNutrition(
        targetDate: String
    ): ApiResult<DailyNutritionSummary> {
        return foodRepository.getDailyNutrition(targetDate)
    }
}

