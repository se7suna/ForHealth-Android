package com.example.forhealth.models

/**
 * 宏量营养素比例（用于饼图显示）
 */
data class MacroRatio(
    val proteinPercent: Double,      // 蛋白质百分比（0-100）
    val carbohydratesPercent: Double, // 碳水化合物百分比（0-100）
    val fatPercent: Double,          // 脂肪百分比（0-100）
    val totalCalories: Double = 0.0  // 总卡路里（用于中心显示）
) {
    companion object {
        fun getInitial(): MacroRatio {
            return MacroRatio(
                proteinPercent = 0.0,
                carbohydratesPercent = 0.0,
                fatPercent = 0.0,
                totalCalories = 0.0
            )
        }
    }
}

