package com.example.forhealth.utils

import java.util.*

/**
 * 数据格式转换工具类
 * 用于将前端显示格式转换为后端API格式
 */
object DataMapper {
    
    /**
     * 性别转换：中文 -> 后端格式
     */
    fun genderToBackend(gender: String): String {
        return when (gender) {
            "男" -> "male"
            "女" -> "female"
            else -> "male" // 默认值
        }
    }
    
    /**
     * 性别转换：后端格式 -> 中文
     */
    fun genderFromBackend(gender: String?): String {
        return when (gender) {
            "male" -> "男"
            "female" -> "女"
            else -> "男" // 默认值
        }
    }
    
    /**
     * 活动水平转换：中文 -> 后端格式
     */
    fun activityLevelToBackend(activityLevel: String): String {
        return when (activityLevel) {
            "久坐" -> "sedentary"
            "轻度活跃" -> "lightly_active"
            "中度活跃" -> "moderately_active"
            "非常活跃" -> "very_active"
            "极其活跃" -> "extremely_active"
            else -> "moderately_active" // 默认值
        }
    }
    
    /**
     * 活动水平转换：后端格式 -> 中文
     */
    fun activityLevelFromBackend(activityLevel: String?): String {
        return when (activityLevel) {
            "sedentary" -> "久坐"
            "lightly_active" -> "轻度活跃"
            "moderately_active" -> "中度活跃"
            "very_active" -> "非常活跃"
            "extremely_active" -> "极其活跃"
            else -> "中度活跃" // 默认值
        }
    }
    
    /**
     * 目标类型转换：中文 -> 后端格式
     */
    fun goalTypeToBackend(goalType: String): String {
        return when (goalType) {
            "减重" -> "lose_weight"
            "维持体重" -> "maintain_weight"
            "增重" -> "gain_weight"
            else -> "maintain_weight" // 默认值
        }
    }
    
    /**
     * 目标类型转换：后端格式 -> 中文
     */
    fun goalTypeFromBackend(goalType: String?): String {
        return when (goalType) {
            "lose_weight" -> "减重"
            "maintain_weight" -> "维持体重"
            "gain_weight" -> "增重"
            else -> "维持体重" // 默认值
        }
    }
    
    /**
     * 出生日期转换：年月日 -> YYYY-MM-DD格式
     */
    fun birthDateToBackend(year: Int, month: Int, day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }
    
    /**
     * 出生日期解析：YYYY-MM-DD格式 -> 年月日
     */
    fun birthDateFromBackend(birthdate: String?): Triple<Int, Int, Int>? {
        if (birthdate == null) return null
        try {
            val parts = birthdate.split("-")
            if (parts.size == 3) {
                return Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            }
        } catch (e: Exception) {
            // 解析失败
        }
        return null
    }
}

