package com.example.forhealth.models

/**
 * 时间线项目（餐食或运动）
 * 使用 sealed class 实现联合类型
 */
sealed class TimelineItem {
    abstract val id: String
    abstract val name: String
    abstract val time: String
    abstract val itemType: ItemType
}

// MealGroup 继承 TimelineItem
data class MealGroupTimelineItem(
    val mealGroup: MealGroup
) : TimelineItem() {
    override val id: String get() = mealGroup.id
    override val name: String get() = mealGroup.type.name
    override val time: String get() = mealGroup.time
    override val itemType: ItemType get() = ItemType.MEAL_GROUP
}

// WorkoutGroup 继承 TimelineItem
data class WorkoutGroupTimelineItem(
    val workoutGroup: WorkoutGroup
) : TimelineItem() {
    override val id: String get() = workoutGroup.id
    override val name: String get() = "Exercise"
    override val time: String get() = workoutGroup.time
    override val itemType: ItemType get() = ItemType.WORKOUT_GROUP
}

