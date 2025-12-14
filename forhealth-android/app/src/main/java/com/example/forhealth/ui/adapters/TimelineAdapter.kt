package com.example.forhealth.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.example.forhealth.models.ExerciseTimelineItem
import com.example.forhealth.models.MealGroup
import com.example.forhealth.models.MealGroupTimelineItem
import com.example.forhealth.models.TimelineItem
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TimelineAdapter(
    private var items: List<TimelineItem>,
    private val onMealGroupClick: (MealGroup) -> Unit,
    private val onExerciseClick: (com.example.forhealth.models.ActivityItem) -> Unit
) : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {

    fun updateItems(newItems: List<TimelineItem>) {
        items = newItems
        notifyDataSetChanged()
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
            is ExerciseTimelineItem -> holder.bindExercise(item.activity, onExerciseClick)
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        // 餐食：不做时区换算，仅格式化外观
        private fun formatMealTime(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            return try {
                OffsetDateTime.parse(raw)
                    .minusHours(-8) // 提前 8 小时
                    .format(fmt)
            } catch (_: Exception) {
                // 解析失败时仅做外观替换
                raw.replace("T", " ").replace(Regex("\\+.*$"), "")
            }
        }


        // 运动：先清理串，再手动减 8 小时
        private fun formatWorkoutTime(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            val cleaned = raw.replace("T", " ").replace(Regex("\\+.*$"), "")
            return try {
                val dt = LocalDateTime.parse(cleaned, fmt)
                dt.minusHours(-8).format(fmt)
            } catch (_: Exception) {
                cleaned
            }
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
            onClick: (com.example.forhealth.models.ActivityItem) -> Unit
        ) {
            itemView.findViewById<TextView>(R.id.tvWorkoutType)?.text = "Exercise"
            itemView.findViewById<TextView>(R.id.tvWorkoutTime)?.text = formatWorkoutTime(activity.time)
            itemView.findViewById<TextView>(R.id.tvTotalCalories)?.text =
                activity.caloriesBurned.toInt().toString()

            val rvExerciseItems = itemView.findViewById<RecyclerView>(R.id.rvWorkoutItems)
            rvExerciseItems?.let {
                it.adapter = null
                it.layoutManager = LinearLayoutManager(itemView.context)
                it.adapter = ExerciseGroupAdapter(listOf(activity))
            }

            itemView.setOnClickListener { onClick(activity) }
        }
    }
}


