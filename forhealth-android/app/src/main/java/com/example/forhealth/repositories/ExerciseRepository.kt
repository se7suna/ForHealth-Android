package com.example.forhealth.repositories

import com.example.forhealth.models.ExerciseItem
import com.example.forhealth.network.ApiResult
import com.example.forhealth.utils.Constants

/**
 * 运动数据仓库
 */
class ExerciseRepository {
    
    /**
     * 获取运动列表
     */
    suspend fun getExercises(): ApiResult<List<ExerciseItem>> {
        // TODO: 从API获取
        // 临时返回本地常量数据
        return ApiResult.Success(Constants.EXERCISE_DB)
    }
    
    /**
     * 搜索运动
     */
    suspend fun searchExercises(query: String): ApiResult<List<ExerciseItem>> {
        // TODO: 调用后端搜索API
        val filtered = Constants.EXERCISE_DB.filter {
            it.name.contains(query, ignoreCase = true)
        }
        return ApiResult.Success(filtered)
    }
    
    /**
     * 创建自定义运动
     */
    suspend fun createCustomExercise(exercise: ExerciseItem): ApiResult<ExerciseItem> {
        // TODO: 调用后端API
        return ApiResult.Success(exercise)
    }
}

