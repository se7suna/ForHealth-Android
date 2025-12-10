package com.example.forhealth.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.forhealth.network.dto.user.UserProfileResponse

/**
 * 用户资料本地存储管理工具类
 * 用于在无后端的情况下本地测试
 */
object ProfileManager {
    
    private const val PREFS_NAME = "forhealth_prefs"
    private const val KEY_USERNAME = "profile_username"
    private const val KEY_EMAIL = "profile_email"
    private const val KEY_HEIGHT = "profile_height"
    private const val KEY_WEIGHT = "profile_weight"
    private const val KEY_AGE = "profile_age"
    private const val KEY_GENDER = "profile_gender"
    private const val KEY_BIRTHDATE = "profile_birthdate"
    private const val KEY_ACTIVITY_LEVEL = "profile_activity_level"
    private const val KEY_HEALTH_GOAL_TYPE = "profile_health_goal_type"
    private const val KEY_TARGET_WEIGHT = "profile_target_weight"
    private const val KEY_GOAL_PERIOD_WEEKS = "profile_goal_period_weeks"
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 保存用户资料到本地
     */
    fun saveProfile(context: Context, profile: UserProfileResponse) {
        getSharedPreferences(context).edit().apply {
            putString(KEY_USERNAME, profile.username)
            putString(KEY_EMAIL, profile.email)
            profile.height?.let { putFloat(KEY_HEIGHT, it.toFloat()) }
            profile.weight?.let { putFloat(KEY_WEIGHT, it.toFloat()) }
            profile.age?.let { putInt(KEY_AGE, it) }
            profile.gender?.let { putString(KEY_GENDER, it) }
            profile.birthdate?.let { putString(KEY_BIRTHDATE, it) }
            profile.activity_level?.let { putString(KEY_ACTIVITY_LEVEL, it) }
            profile.health_goal_type?.let { putString(KEY_HEALTH_GOAL_TYPE, it) }
            profile.target_weight?.let { putFloat(KEY_TARGET_WEIGHT, it.toFloat()) }
            profile.goal_period_weeks?.let { putInt(KEY_GOAL_PERIOD_WEEKS, it) }
            apply()
        }
    }
    
    /**
     * 更新部分用户资料
     */
    fun updateProfile(context: Context, updates: Map<String, Any?>) {
        getSharedPreferences(context).edit().apply {
            updates.forEach { (key, value) ->
                when (key) {
                    "username" -> putString(KEY_USERNAME, value as? String)
                    "email" -> putString(KEY_EMAIL, value as? String)
                    "height" -> (value as? Double)?.let { putFloat(KEY_HEIGHT, it.toFloat()) }
                    "weight" -> (value as? Double)?.let { putFloat(KEY_WEIGHT, it.toFloat()) }
                    "age" -> (value as? Int)?.let { putInt(KEY_AGE, it) }
                    "gender" -> putString(KEY_GENDER, value as? String)
                    "birthdate" -> putString(KEY_BIRTHDATE, value as? String)
                    "activity_level" -> putString(KEY_ACTIVITY_LEVEL, value as? String)
                    "health_goal_type" -> putString(KEY_HEALTH_GOAL_TYPE, value as? String)
                    "target_weight" -> (value as? Double)?.let { putFloat(KEY_TARGET_WEIGHT, it.toFloat()) }
                    "goal_period_weeks" -> (value as? Int)?.let { putInt(KEY_GOAL_PERIOD_WEEKS, it) }
                }
            }
            apply()
        }
    }
    
    /**
     * 从本地获取用户资料
     */
    fun getProfile(context: Context): UserProfileResponse? {
        val prefs = getSharedPreferences(context)
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, "test@example.com") ?: "test@example.com" // 默认邮箱
        
        return UserProfileResponse(
            email = email,
            username = username,
            height = if (prefs.contains(KEY_HEIGHT)) prefs.getFloat(KEY_HEIGHT, 0f).toDouble() else null,
            weight = if (prefs.contains(KEY_WEIGHT)) prefs.getFloat(KEY_WEIGHT, 0f).toDouble() else null,
            age = if (prefs.contains(KEY_AGE)) prefs.getInt(KEY_AGE, 0) else null,
            gender = prefs.getString(KEY_GENDER, null),
            birthdate = prefs.getString(KEY_BIRTHDATE, null),
            activity_level = prefs.getString(KEY_ACTIVITY_LEVEL, null),
            health_goal_type = prefs.getString(KEY_HEALTH_GOAL_TYPE, null),
            target_weight = if (prefs.contains(KEY_TARGET_WEIGHT)) prefs.getFloat(KEY_TARGET_WEIGHT, 0f).toDouble() else null,
            goal_period_weeks = if (prefs.contains(KEY_GOAL_PERIOD_WEEKS)) prefs.getInt(KEY_GOAL_PERIOD_WEEKS, 0) else null,
            // 新字段使用默认值（本地缓存不存储这些字段）
            bmr = null,
            tdee = null,
            daily_calorie_goal = null,
            liked_foods = null,
            disliked_foods = null,
            allergies = null,
            dietary_restrictions = null,
            preferred_tastes = null,
            cooking_skills = null,
            budget_per_day = null,
            include_budget = false
        )
    }
    
    /**
     * 清除用户资料
     */
    fun clearProfile(context: Context) {
        getSharedPreferences(context).edit().apply {
            remove(KEY_USERNAME)
            remove(KEY_EMAIL)
            remove(KEY_HEIGHT)
            remove(KEY_WEIGHT)
            remove(KEY_AGE)
            remove(KEY_GENDER)
            remove(KEY_BIRTHDATE)
            remove(KEY_ACTIVITY_LEVEL)
            remove(KEY_HEALTH_GOAL_TYPE)
            remove(KEY_TARGET_WEIGHT)
            remove(KEY_GOAL_PERIOD_WEEKS)
            apply()
        }
    }
}

