package com.example.forhealth.utils

/**
 * 数据映射工具类
 * 用于前端中文显示与后端英文API之间的转换
 */
object DataMapper {
    
    // ==================== 性别转换 ====================
    
    /**
     * 将前端性别（中文）转换为后端格式（英文）
     */
    fun genderToBackend(gender: String): String {
        return when (gender) {
            "男" -> "male"
            "女" -> "female"
            else -> "male"
        }
    }
    
    /**
     * 将后端性别（英文）转换为前端格式（中文）
     */
    fun genderFromBackend(gender: String?): String {
        return when (gender) {
            "male" -> "男"
            "female" -> "女"
            else -> "未设置"
        }
    }
    
    // ==================== 活动水平转换 ====================
    
    /**
     * 将前端活动水平（中文）转换为后端格式（英文）
     */
    fun activityLevelToBackend(level: String): String {
        return when (level) {
            "久坐" -> "sedentary"
            "轻度活跃" -> "lightly_active"
            "中度活跃" -> "moderately_active"
            "非常活跃" -> "very_active"
            "极其活跃" -> "extremely_active"
            else -> "moderately_active"
        }
    }
    
    /**
     * 将后端活动水平（英文）转换为前端格式（中文）
     */
    fun activityLevelFromBackend(level: String?): String {
        return when (level) {
            "sedentary" -> "久坐"
            "lightly_active" -> "轻度活跃"
            "moderately_active" -> "中度活跃"
            "very_active" -> "非常活跃"
            "extremely_active" -> "极其活跃"
            else -> "未设置"
        }
    }
    
    // ==================== 目标类型转换 ====================
    
    /**
     * 将前端目标类型（中文）转换为后端格式（英文）
     */
    fun goalTypeToBackend(type: String): String {
        return when (type) {
            "减重" -> "lose_weight"
            "维持体重" -> "maintain_weight"
            "增重" -> "gain_weight"
            else -> "maintain_weight"
        }
    }
    
    /**
     * 将后端目标类型（英文）转换为前端格式（中文）
     */
    fun goalTypeFromBackend(type: String?): String {
        return when (type) {
            "lose_weight" -> "减重"
            "maintain_weight" -> "维持体重"
            "gain_weight" -> "增重"
            else -> "未设置"
        }
    }
    
    // ==================== 日期转换 ====================
    
    /**
     * 将出生日期转换为后端格式（YYYY-MM-DD）
     */
    fun birthDateToBackend(year: Int, month: Int, day: Int): String {
        return String.format("%04d-%02d-%02d", year, month, day)
    }
    
    /**
     * 解析后端日期格式（YYYY-MM-DD）
     * 返回数组：[year, month, day]
     */
    fun parseBirthDate(birthdate: String?): IntArray? {
        if (birthdate.isNullOrEmpty()) return null
        
        return try {
            val parts = birthdate.split("-")
            if (parts.size == 3) {
                intArrayOf(
                    parts[0].toInt(),  // year
                    parts[1].toInt(),  // month
                    parts[2].toInt()   // day
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}


