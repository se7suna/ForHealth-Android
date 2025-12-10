package com.example.forhealth.network.dto.user

/**
 * 用户管理相关 DTO
 */

// 更新身体数据请求
data class BodyDataRequest(
    val height: Double,
    val weight: Double,
    val birthdate: String, // YYYY-MM-DD
    val gender: String // "male" or "female"
)

// 更新活动水平请求
data class ActivityLevelRequest(
    val activity_level: String // "sedentary", "lightly_active", "moderately_active", "very_active", "extremely_active"
)

// 设定健康目标请求
data class HealthGoalRequest(
    val health_goal_type: String, // "lose_weight", "gain_weight", "maintain_weight"
    val target_weight: Double? = null,
    val goal_period_weeks: Int? = null
)

// 用户资料响应（根据 OpenAPI 规范，包含所有字段）
data class UserProfileResponse(
    val email: String,
    val username: String,
    val height: Double? = null,
    val weight: Double? = null,
    val age: Int? = null,
    val gender: String? = null, // "male" or "female"
    val birthdate: String? = null, // YYYY-MM-DD
    val activity_level: String? = null,
    val health_goal_type: String? = null,
    val target_weight: Double? = null,
    val goal_period_weeks: Int? = null,
    val bmr: Double? = null, // 基础代谢率
    val tdee: Double? = null, // 每日总能量消耗
    val daily_calorie_goal: Double? = null, // 每日卡路里目标
    // 食物偏好
    val liked_foods: List<String>? = null,
    val disliked_foods: List<String>? = null,
    val allergies: List<String>? = null,
    val dietary_restrictions: List<String>? = null,
    val preferred_tastes: List<String>? = null,
    val cooking_skills: String? = null,
    // 预算信息
    val budget_per_day: Double? = null,
    val include_budget: Boolean = false
)

// 更新用户资料请求
data class UserProfileUpdate(
    val username: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    val birthdate: String? = null,
    val gender: String? = null,
    val activity_level: String? = null,
    val health_goal_type: String? = null,
    val target_weight: Double? = null,
    val goal_period_weeks: Int? = null,
    val liked_foods: List<String>? = null,
    val disliked_foods: List<String>? = null,
    val allergies: List<String>? = null,
    val dietary_restrictions: List<String>? = null,
    val preferred_tastes: List<String>? = null,
    val cooking_skills: String? = null,
    val budget_per_day: Double? = null,
    val include_budget: Boolean? = null
)

// 创建体重记录请求
data class WeightRecordCreateRequest(
    val weight: Double,
    val recorded_at: String, // ISO 8601 date-time
    val notes: String? = null
)

// 体重记录响应
data class WeightRecordResponse(
    val id: String,
    val weight: Double,
    val recorded_at: String, // ISO 8601 date-time
    val notes: String? = null,
    val created_at: String // ISO 8601 date-time
)

// 体重记录列表响应
data class WeightRecordListResponse(
    val total: Int,
    val records: List<WeightRecordResponse>
)

// 更新体重记录请求
data class WeightRecordUpdateRequest(
    val weight: Double? = null,
    val recorded_at: String? = null,
    val notes: String? = null
)

