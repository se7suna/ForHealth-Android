package com.example.forhealth.network.dto.ai

import com.example.forhealth.network.dto.food.NutritionData

/**
 * AI助手相关 DTO
 */

// 食物识别确认响应
data class FoodRecognitionConfirmResponse(
    val success: Boolean,
    val message: String,
    val processed_foods: List<ProcessedFoodItem>,
    val total_foods: Int
)

// 处理后的食物信息
data class ProcessedFoodItem(
    val food_id: String,
    val food_name: String,
    val serving_amount: Double,
    val serving_size: Double,
    val serving_unit: String,
    val nutrition_per_serving: NutritionData,
    val source: String // "ai" or "database"
)

// 生成饮食计划请求
data class MealPlanRequest(
    val plan_duration: String, // "day" or "week"
    val plan_days: Int? = null, // 当plan_duration为day时必填，最多30天
    val meals_per_day: Int = 3, // 2-6餐
    val target_calories: Double? = null, // 可选，不填则使用用户资料中的目标
    val include_budget: Boolean = false,
    val budget_per_day: Double? = null,
    val food_preference: FoodPreferenceRequest? = null
)

// 食物偏好请求
data class FoodPreferenceRequest(
    val liked_foods: List<String>? = null,
    val disliked_foods: List<String>? = null,
    val allergies: List<String>? = null,
    val dietary_restrictions: List<String>? = null,
    val preferred_tastes: List<String>? = null,
    val cooking_skills: String? = null
)

// 饮食计划响应
data class MealPlanResponse(
    val success: Boolean,
    val message: String,
    val plan_duration: String,
    val plan_days: Int,
    val target_calories: Double,
    val daily_plans: List<DailyMealPlanResponse>,
    val total_cost: Double? = null,
    val average_daily_cost: Double? = null,
    val nutrition_summary: Map<String, Any>,
    val suggestions: List<String>? = null
)

// 每日饮食计划响应
data class DailyMealPlanResponse(
    val date: String, // YYYY-MM-DD
    val meals: Map<String, List<MealItemResponse>>, // key为餐次名称（如：早餐、午餐、晚餐）
    val daily_nutrition: NutritionData,
    val daily_calories: Double,
    val daily_cost: Double? = null,
    val macro_ratio: Map<String, Double> // 宏量营养素比例（蛋白质、碳水化合物、脂肪的百分比）
)

// 单餐食物项
data class MealItemResponse(
    val food_name: String,
    val serving_size: Double,
    val serving_unit: String = "克",
    val cooking_method: String? = null,
    val nutrition: NutritionData,
    val estimated_cost: Double? = null
)

// 知识问答请求
data class QuestionRequest(
    val question: String,
    val context: Map<String, Any>? = null // 可选上下文信息
)

// 知识问答响应
data class QuestionResponse(
    val success: Boolean,
    val question: String,
    val answer: String,
    val related_topics: List<String>? = null,
    val sources: List<String>? = null,
    val confidence: Double? = null // 0-1
)

// 提醒设置
data class ReminderSettings(
    val meal_reminders: Boolean = true,
    val meal_reminder_times: List<String>? = null, // 格式：HH:MM
    val record_reminders: Boolean = true,
    val record_reminder_hours: Int? = null, // 未记录提醒间隔（小时）
    val goal_reminders: Boolean = true,
    val motivational_messages: Boolean = true
)

// 更新提醒设置请求
data class ReminderSettingsRequest(
    val settings: ReminderSettings
)

// 提醒设置响应
data class ReminderSettingsResponse(
    val success: Boolean,
    val message: String,
    val settings: ReminderSettings
)

// 通知消息响应
data class NotificationMessageResponse(
    val id: String,
    val type: String, // "meal_reminder", "record_reminder", "goal_achievement", "motivational", "feedback"
    val title: String,
    val content: String,
    val created_at: String, // ISO 8601 date-time
    val read: Boolean = false,
    val action_url: String? = null,
    val priority: String = "normal" // "low", "normal", "high"
)

// 通知列表响应
data class NotificationListResponse(
    val total: Int,
    val unread_count: Int,
    val notifications: List<NotificationMessageResponse>
)

// 标记通知为已读请求
data class NotificationReadRequest(
    val notification_ids: List<String>
)

// 每日反馈响应
data class DailyFeedbackResponse(
    val success: Boolean,
    val feedback: FeedbackDataResponse,
    val notification: NotificationMessageResponse? = null
)

// 反馈数据响应
data class FeedbackDataResponse(
    val date: String, // YYYY-MM-DD
    val daily_calories: Double,
    val target_calories: Double,
    val calories_progress: Double, // 0-1
    val nutrition_summary: Map<String, Any>, // 营养摘要（结构与NutritionData相同）
    val meal_count: Int,
    val goal_status: String, // "on_track", "exceeded", "below"
    val suggestions: List<String>
)

// 饮食分析请求
data class DietAnalysisRequest(
    val days: Int = 7 // 分析最近几天的记录（默认7天，1-30天）
)

// 饮食分析响应
data class DietAnalysisResponse(
    val success: Boolean,
    val message: String, // 一句话分析建议（亲和语气）
    val analysis: Map<String, Any>? = null // 详细分析数据（可选）
)

// 菜式推荐响应
data class MealRecommendationResponse(
    val success: Boolean,
    val message: String, // 推荐语（包含时间提醒和菜式推荐）
    val meal_type: String, // 推荐的餐次类型：早餐、午餐、晚餐、加餐
    val recommended_dish: String, // 推荐的菜式名称
    val nutrition_highlight: String? = null, // 营养亮点（如：高蛋白、适量碳水）
    val reason: String? = null // 推荐理由
)

