package com.example.forhealth.model

import com.google.gson.annotations.SerializedName

/**
 * 运动类型响应
 */
data class SearchSportsResponse(
    @SerializedName("sport_type") val sportType: String?,
    val describe: String?,
    @SerializedName("METs") val mets: Double?,
    @SerializedName("image_url") val imageUrl: String?
)

/**
 * 记录运动请求
 */
data class LogSportsRequest(
    @SerializedName("sport_type") val sportType: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("duration_time") val durationTime: Int
)

/**
 * 搜索运动记录请求
 */
data class SearchSportRecordsRequest(
    @SerializedName("sport_type") val sportType: String? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null
)

/**
 * 运动记录响应
 */
data class SearchSportRecordsResponse(
    @SerializedName("sport_type") val sportType: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("duration_time") val durationTime: Int?,
    @SerializedName("calories_burned") val caloriesBurned: Double?,
    @SerializedName("record_id") val recordId: String?
)

/**
 * 简单响应
 */
data class SimpleSportsResponse(
    val success: Boolean,
    val message: String
)

/**
 * 自定义运动添加请求
 */
data class AddCustomSportRequest(
    @SerializedName("sport_type") val sportType: String,
    @SerializedName("METs") val mets: Double,
    val describe: String?,
    @SerializedName("image_url") val imageUrl: String? = null
)
