package com.example.forhealth.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault())
    private val isoDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
    
    fun getCurrentTime(): String {
        return timeFormat.format(Date())
    }
    
    fun getCurrentDate(): String {
        return dateFormat.format(Date())
    }
    
    /**
     * 返回 ISO 8601 带时区的时间戳，用于后端 recorded_at
     * 例：2025-03-01T11:21:35+08:00
     */
    fun getCurrentDateTimeIso(): String {
        return isoDateTimeFormat.format(Date())
    }
    
    fun getMealTypeByHour(): com.example.forhealth.models.MealType {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 11 -> com.example.forhealth.models.MealType.BREAKFAST
            hour < 15 -> com.example.forhealth.models.MealType.LUNCH
            hour < 19 -> com.example.forhealth.models.MealType.DINNER
            else -> com.example.forhealth.models.MealType.SNACK
        }
    }
}

