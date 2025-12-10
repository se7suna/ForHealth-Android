package com.example.forhealth.network.dto.sports

/**
 * 运动记录相关 DTO
 */

// 创建自定义运动类型请求
data class CreateSportsRequest(
    val sport_type: String? = null,
    val describe: String? = null,
    val METs: Double? = null
)

// 更新自定义运动类型请求
data class UpdateSportsRequest(
    val sport_type: String? = null,
    val describe: String? = null,
    val METs: Double? = null
)

// 简单运动响应
data class SimpleSportsResponse(
    val success: Boolean,
    val message: String
)

// 搜索运动类型响应
data class SearchSportsResponse(
    val sport_type: String? = null,
    val sport_name: String? = null,
    val describe: String? = null,
    val METs: Double? = null,
    val image_url: String? = null
)

// 记录运动请求
data class LogSportsRequest(
    val sport_name: String? = null,
    val created_at: String? = null, // ISO 8601 date-time，默认当前时间
    val duration_time: Int? = null // minutes，必须大于0
)

// 更新运动记录请求
data class UpdateSportsRecordRequest(
    val record_id: String? = null,
    val old_sport_name: String? = null,
    val new_sport_name: String? = null,
    val created_at: String? = null,
    val duration_time: Int? = null
)

// 查询运动历史请求
data class SearchSportRecordsRequest(
    val start_date: String? = null, // YYYY-MM-DD
    val end_date: String? = null, // YYYY-MM-DD
    val sport_name: String? = null
)

// 运动记录响应
data class SearchSportRecordsResponse(
    val record_id: String? = null,
    val sport_type: String? = null,
    val sport_name: String? = null,
    val created_at: String? = null, // ISO 8601 date-time
    val duration_time: Int? = null, // minutes
    val calories_burned: Double? = null
)

