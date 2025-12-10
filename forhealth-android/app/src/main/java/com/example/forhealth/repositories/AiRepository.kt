package com.example.forhealth.repositories

import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.ai.*
import com.example.forhealth.network.safeApiCall

/**
 * AI助手数据仓库
 * 负责管理AI相关数据的获取（只返回DTO，不进行转换）
 */
class AiRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * 健康知识问答
     * POST /api/ai/ask
     * @param question 用户问题
     * @param context 可选上下文信息
     */
    suspend fun askQuestion(
        question: String,
        context: Map<String, Any>? = null
    ): ApiResult<QuestionResponse> {
        return safeApiCall {
            apiService.askQuestion(
                QuestionRequest(
                    question = question,
                    context = context
                )
            )
        }
    }
    
    /**
     * 饮食分析与建议
     * POST /api/ai/diet/analyze
     * @param days 分析最近几天的记录（默认7天）
     */
    suspend fun analyzeDiet(
        days: Int = 7
    ): ApiResult<DietAnalysisResponse> {
        return safeApiCall {
            apiService.analyzeDiet(
                DietAnalysisRequest(days = days)
            )
        }
    }
    
    /**
     * 智能菜式推荐
     * GET /api/ai/meal/recommend
     */
    suspend fun recommendMeal(): ApiResult<MealRecommendationResponse> {
        return safeApiCall {
            apiService.recommendMeal()
        }
    }
}

