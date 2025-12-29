package com.example.forhealth.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.example.forhealth.models.ExerciseItem
import com.example.forhealth.models.ExerciseTimelineItem
import com.example.forhealth.models.MealGroup
import com.example.forhealth.models.MealGroupTimelineItem
import com.example.forhealth.models.TimelineItem
import com.example.forhealth.utils.CalculationUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimelineAdapter(
    private var items: List<TimelineItem>,
    private val onMealGroupClick: (MealGroup) -> Unit,
    private val onExerciseClick: (com.example.forhealth.models.ActivityItem) -> Unit,
    private var exerciseLibrary: List<ExerciseItem> = emptyList(),
    private var userWeight: Double? = null
) : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {

    fun updateItems(newItems: List<TimelineItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateExerciseLibrary(list: List<ExerciseItem>) {
        exerciseLibrary = list
    }

    fun updateUserWeight(weight: Double?) {
        userWeight = weight
    }

    override fun getItemViewType(position: Int): Int = when (items[position].itemType) {
        com.example.forhealth.models.ItemType.MEAL_GROUP -> 0
        com.example.forhealth.models.ItemType.EXERCISE -> 1
        else -> 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (viewType) {
            0 -> R.layout.item_meal_group_card
            1 -> R.layout.item_workout_group_card
            else -> R.layout.item_meal_group_card
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MealGroupTimelineItem -> holder.bindMealGroup(item.mealGroup, onMealGroupClick)
            is ExerciseTimelineItem -> holder.bindExercise(item.activity, onExerciseClick, exerciseLibrary, userWeight)
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        private val isoDateFormatWithTimezone = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())

        // 将UTC时间转换为本地时间（UTC+8），然后格式化显示
        private fun formatTimeWithTimezone(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            return try {
                // 先尝试解析带时区的时间格式（如 2025-01-01T12:00:00+08:00）
                val date = try {
                    isoDateFormatWithTimezone.parse(raw)
                } catch (e: Exception) {
                    // 如果失败，尝试处理Z结尾的UTC时间（如 2025-01-01T12:00:00Z）
                    try {
                        if (raw.endsWith("Z")) {
                            val utcString = raw.replace("Z", "+00:00")
                            val utcDate = isoDateFormatWithTimezone.parse(utcString)
                            // UTC时间加8小时转换为中国时间
                            utcDate?.let { Date(it.time + 8 * 60 * 60 * 1000) }
                        } else {
                            // 如果不包含时区信息，假设是UTC时间，加8小时转换为中国时间
                            val cleaned = raw.replace(Regex("\\+.*$"), "")
                            val utcDate = isoDateFormat.parse(cleaned)
                            // UTC时间加8小时转换为中国时间
                            utcDate?.let { Date(it.time + 8 * 60 * 60 * 1000) }
                        }
                    } catch (e2: Exception) {
                        null
                    }
                }
                
                if (date != null) {
                    // 格式化为显示格式
                    fmt.format(date)
                } else {
                    // 解析失败时，简单替换格式
                    raw.replace("T", " ").replace(Regex("\\+.*$"), "").replace("Z", "").take(19)
                }
            } catch (_: Exception) {
                // 解析失败时仅做外观替换
                raw.replace("T", " ").replace(Regex("\\+.*$"), "").replace("Z", "").take(19)
            }
        }

        // 餐食：格式化时间（带时区转换）
        private fun formatMealTime(raw: String?): String {
            return formatTimeWithTimezone(raw)
        }

        // 运动：格式化时间（带时区转换）
        private fun formatWorkoutTime(raw: String?): String {
            return formatTimeWithTimezone(raw)
        }

        fun bindMealGroup(mealGroup: MealGroup, onClick: (MealGroup) -> Unit) {
            itemView.findViewById<TextView>(R.id.tvMealType)?.text = when (mealGroup.type) {
                com.example.forhealth.models.MealType.BREAKFAST -> "Breakfast"
                com.example.forhealth.models.MealType.LUNCH -> "Lunch"
                com.example.forhealth.models.MealType.DINNER -> "Dinner"
                com.example.forhealth.models.MealType.SNACK -> "Snack"
            }
            itemView.findViewById<TextView>(R.id.tvMealTime)?.text = formatMealTime(mealGroup.time)
            itemView.findViewById<TextView>(R.id.tvTotalCalories)?.text =
                mealGroup.totalCalories.toInt().toString()

            val rvMealItems = itemView.findViewById<RecyclerView>(R.id.rvMealItems)
            rvMealItems?.let {
                it.adapter = null
                it.layoutManager = LinearLayoutManager(itemView.context)
                it.adapter = MealGroupFoodAdapter(mealGroup.meals)
            }

            itemView.setOnClickListener { onClick(mealGroup) }
        }

        fun bindExercise(
            activity: com.example.forhealth.models.ActivityItem,
            onClick: (com.example.forhealth.models.ActivityItem) -> Unit,
            exerciseLibrary: List<ExerciseItem>,
            userWeight: Double?
        ) {
            itemView.findViewById<TextView>(R.id.tvWorkoutType)?.text = "Exercise"
            itemView.findViewById<TextView>(R.id.tvWorkoutTime)?.text = formatWorkoutTime(activity.time)
            val durationHours = activity.duration / 60.0
            val matched = exerciseLibrary.find { it.name == activity.name }
            val recalculated = matched?.let {
                CalculationUtils.calculateExerciseCalories(it, activity.duration.toDouble(), userWeight)
            } ?: activity.caloriesBurned
            itemView.findViewById<TextView>(R.id.tvTotalCalories)?.text =
                recalculated.toInt().toString()

            val rvExerciseItems = itemView.findViewById<RecyclerView>(R.id.rvWorkoutItems)
            rvExerciseItems?.let {
                it.adapter = null
                it.layoutManager = LinearLayoutManager(itemView.context)
                it.adapter = ExerciseGroupAdapter(listOf(activity.copy(caloriesBurned = recalculated)))
            }

            itemView.setOnClickListener { onClick(activity) }
        }
    }
}


