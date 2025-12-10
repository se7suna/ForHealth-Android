package com.example.forhealth.repositories

import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.auth.MessageResponse
import com.example.forhealth.network.dto.user.*
import com.example.forhealth.network.safeApiCall

/**
 * 用户数据仓库
 * 负责管理用户资料和体重记录的所有操作
 */
class UserRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * 获取用户完整资料
     */
    suspend fun getProfile(): ApiResult<UserProfileResponse> {
        return safeApiCall {
            apiService.getProfile()
        }
    }
    
    /**
     * 更新用户资料
     */
    suspend fun updateProfile(updateRequest: UserProfileUpdate): ApiResult<UserProfileResponse> {
        return safeApiCall {
            apiService.updateProfile(updateRequest)
        }
    }
    
    /**
     * 更新用户身体基本数据
     */
    suspend fun updateBodyData(request: BodyDataRequest): ApiResult<MessageResponse> {
        return safeApiCall {
            apiService.updateBodyData(request)
        }
    }
    
    /**
     * 更新活动水平
     */
    suspend fun updateActivityLevel(request: ActivityLevelRequest): ApiResult<MessageResponse> {
        return safeApiCall {
            apiService.updateActivityLevel(request)
        }
    }
    
    /**
     * 设定健康目标
     */
    suspend fun updateHealthGoal(request: HealthGoalRequest): ApiResult<MessageResponse> {
        return safeApiCall {
            apiService.updateHealthGoal(request)
        }
    }
    
    /**
     * 创建体重记录
     */
    suspend fun createWeightRecord(request: WeightRecordCreateRequest): ApiResult<WeightRecordResponse> {
        return safeApiCall {
            apiService.createWeightRecord(request)
        }
    }
    
    /**
     * 获取体重记录列表
     */
    suspend fun getWeightRecords(
        startDate: String? = null,
        endDate: String? = null,
        limit: Int = 100
    ): ApiResult<WeightRecordListResponse> {
        return safeApiCall {
            apiService.getWeightRecords(startDate, endDate, limit)
        }
    }
    
    /**
     * 更新体重记录
     */
    suspend fun updateWeightRecord(
        recordId: String,
        request: WeightRecordUpdateRequest
    ): ApiResult<WeightRecordResponse> {
        return safeApiCall {
            apiService.updateWeightRecord(recordId, request)
        }
    }
    
    /**
     * 删除体重记录
     */
    suspend fun deleteWeightRecord(recordId: String): ApiResult<MessageResponse> {
        return safeApiCall {
            apiService.deleteWeightRecord(recordId)
        }
    }
}

