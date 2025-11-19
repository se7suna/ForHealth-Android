package com.example.forhealth.model

/**
 * 统一的每日记录项（食物或运动）
 */
sealed class DailyRecordItem {
    data class FoodItem(val record: FoodRecord) : DailyRecordItem()
    data class SportsItem(val record: SearchSportRecordsResponse) : DailyRecordItem()
}

