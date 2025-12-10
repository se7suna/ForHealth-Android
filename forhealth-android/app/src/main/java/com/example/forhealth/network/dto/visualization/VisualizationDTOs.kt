package com.example.forhealth.network.dto.visualization

/**
 * 可视化报告相关 DTO
 */

// 每日卡路里摘要
data class DailyCalorieSummary(
    val date: String, // YYYY-MM-DD
    val total_intake: Double, // 总摄入
    val total_burned: Double, // 总消耗
    val daily_goal: Double, // 每日目标
    val net_calories: Double, // 净卡路里
    val goal_percentage: Double, // 目标完成百分比
    val is_over_budget: Boolean // 是否超预算
)

// 营养素比例
data class NutritionRatio(
    val protein: Double, // 百分比
    val carbohydrates: Double, // 百分比
    val fat: Double // 百分比
)

// 营养素摄入vs推荐量
data class NutritionIntakeVsRecommended(
    val nutrient_name: String,
    val actual: Double,
    val recommended: Double,
    val percentage: Double // 完成百分比
)

// 食物类别分布
data class FoodCategoryDistribution(
    val category: String,
    val count: Int,
    val total_calories: Double,
    val percentage: Double
)

// 营养素分析响应
data class NutritionAnalysisResponse(
    val date_range: Map<String, String>, // start_date, end_date
    val macronutrient_ratio: NutritionRatio,
    val nutrition_vs_recommended: List<NutritionIntakeVsRecommended>,
    val food_category_distribution: List<FoodCategoryDistribution>
)

// 时间序列数据点
data class TimeSeriesDataPoint(
    val date: String, // YYYY-MM-DD
    val value: Double
)

// 时间序列趋势响应
data class TimeSeriesTrendResponse(
    val view_type: String, // "day", "week", "month"
    val date_range: Map<String, String>,
    val intake_trend: List<TimeSeriesDataPoint>, // 摄入趋势
    val burned_trend: List<TimeSeriesDataPoint>, // 消耗趋势
    val weight_trend: List<TimeSeriesDataPoint> // 体重趋势
)

// 健康报告导出响应
data class HealthReportExportResponse(
    val user_info: Map<String, Any>,
    val date_range: Map<String, String>,
    val summary: Map<String, Any>,
    val daily_calorie_summary: DailyCalorieSummary,
    val nutrition_analysis: NutritionAnalysisResponse,
    val time_series_trend: TimeSeriesTrendResponse,
    val generated_at: String // ISO 8601 date-time
)

