package com.example.forhealth.repositories

import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.visualization.*
import com.example.forhealth.network.safeApiCall

/**
 * 可视化数据仓库
 * 负责管理可视化报告数据的获取（只返回DTO，不进行转换）
 */
class VisualizationRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * 获取每日卡路里摘要
     * @param targetDate 目标日期(YYYY-MM-DD)，默认为null（今天）
     */
    suspend fun getDailyCalorieSummary(
        targetDate: String? = null
    ): ApiResult<DailyCalorieSummary> {
        return safeApiCall {
            apiService.getDailyCalorieSummary(targetDate)
        }
    }
    
    /**
     * 获取时间序列趋势分析
     * @param startDate 开始日期(YYYY-MM-DD)
     * @param endDate 结束日期(YYYY-MM-DD)
     * @param viewType 视图类型: "day", "week", "month"（默认"day"）
     */
    suspend fun getTimeSeriesTrend(
        startDate: String,
        endDate: String,
        viewType: String = "day"
    ): ApiResult<TimeSeriesTrendResponse> {
        return safeApiCall {
            apiService.getTimeSeriesTrend(startDate, endDate, viewType)
        }
    }
    
    /**
     * 获取营养素与食物来源分析
     * @param startDate 开始日期(YYYY-MM-DD)
     * @param endDate 结束日期(YYYY-MM-DD)
     */
    suspend fun getNutritionAnalysis(
        startDate: String,
        endDate: String
    ): ApiResult<NutritionAnalysisResponse> {
        return safeApiCall {
            apiService.getNutritionAnalysis(startDate, endDate)
        }
    }
    
    /**
     * 导出健康数据报告
     * @param startDate 开始日期(YYYY-MM-DD)
     * @param endDate 结束日期(YYYY-MM-DD)
     */
    suspend fun exportHealthReport(
        startDate: String,
        endDate: String
    ): ApiResult<HealthReportExportResponse> {
        return safeApiCall {
            apiService.exportHealthReport(startDate, endDate)
        }
    }
}

