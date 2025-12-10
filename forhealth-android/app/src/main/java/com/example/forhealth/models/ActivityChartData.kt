package com.example.forhealth.models

/**
 * 活动图表数据点（用于日柱状图和周/月趋势图）
 */
data class ActivityChartDataPoint(
    val label: String,      // 日期标签（如 "Mon", "Tue" 或 "W1", "W2"）
    val intake: Double,      // 摄入卡路里
    val burned: Double       // 消耗卡路里
)

/**
 * 活动图表数据（包含多个数据点）
 */
data class ActivityChartData(
    val dataPoints: List<ActivityChartDataPoint>
) {
    companion object {
        fun getInitial(): ActivityChartData {
            return ActivityChartData(emptyList())
        }
    }
}

