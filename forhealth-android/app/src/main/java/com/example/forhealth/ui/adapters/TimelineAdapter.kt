package com.example.forhealth.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.example.forhealth.models.MealGroupTimelineItem
import com.example.forhealth.models.TimelineItem
import com.example.forhealth.models.ExerciseTimelineItem
import com.example.forhealth.ui.adapters.ExerciseGroupAdapter

class TimelineAdapter(
    private var items: List<TimelineItem>,
    private val onMealGroupClick: (com.example.forhealth.models.MealGroup) -> Unit,
    private val onExerciseClick: (com.example.forhealth.models.ActivityItem) -> Unit
) : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {
    
    fun updateItems(newItems: List<TimelineItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (items[position].itemType) {
            com.example.forhealth.models.ItemType.MEAL_GROUP -> 0
            com.example.forhealth.models.ItemType.EXERCISE -> 1
            else -> 0
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (viewType) {
            0 -> R.layout.item_meal_group_card
            1 -> R.layout.item_workout_group_card // 使用exercise布局显示运动记录
            else -> R.layout.item_meal_group_card
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        when (item) {
            is MealGroupTimelineItem -> {
                holder.bindMealGroup(item.mealGroup, onMealGroupClick)
            }
            is ExerciseTimelineItem -> {
                holder.bindExercise(item.activity, onExerciseClick)
            }
        }
    }
    
    override fun getItemCount() = items.size
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindMealGroup(mealGroup: com.example.forhealth.models.MealGroup, onClick: (com.example.forhealth.models.MealGroup) -> Unit) {
            itemView.findViewById<TextView>(R.id.tvMealType)?.text = when (mealGroup.type) {
                com.example.forhealth.models.MealType.BREAKFAST -> "Breakfast"
                com.example.forhealth.models.MealType.LUNCH -> "Lunch"
                com.example.forhealth.models.MealType.DINNER -> "Dinner"
                com.example.forhealth.models.MealType.SNACK -> "Snack"
            }
            itemView.findViewById<TextView>(R.id.tvMealTime)?.text = mealGroup.time
            itemView.findViewById<TextView>(R.id.tvTotalCalories)?.text = "${mealGroup.totalCalories.toInt()}"
            
            // 确保使用 MealGroupFoodAdapter 和 item_meal_group_food.xml
            val rvMealItems = itemView.findViewById<RecyclerView>(R.id.rvMealItems)
            rvMealItems?.let {
                // 清除之前的 adapter（如果有）
                it.adapter = null
                // 使用正确的 adapter：MealGroupFoodAdapter（使用 item_meal_group_food.xml）
                val adapter = MealGroupFoodAdapter(mealGroup.meals)
                it.layoutManager = LinearLayoutManager(itemView.context)
                it.adapter = adapter
            }
            
            itemView.setOnClickListener {
                onClick(mealGroup)
            }
        }
        
        fun bindExercise(activity: com.example.forhealth.models.ActivityItem, onClick: (com.example.forhealth.models.ActivityItem) -> Unit) {
            itemView.findViewById<TextView>(R.id.tvWorkoutType)?.text = "Exercise"
            itemView.findViewById<TextView>(R.id.tvWorkoutTime)?.text = activity.time
            itemView.findViewById<TextView>(R.id.tvTotalCalories)?.text = "${activity.caloriesBurned.toInt()}"
            
            // 使用 ExerciseGroupAdapter 显示运动记录列表
            val rvExerciseItems = itemView.findViewById<RecyclerView>(R.id.rvWorkoutItems)
            rvExerciseItems?.let {
                // 清除之前的 adapter（如果有）
                it.adapter = null
                // 使用 ExerciseGroupAdapter 显示运动记录列表
                val adapter = ExerciseGroupAdapter(listOf(activity))
                it.layoutManager = LinearLayoutManager(itemView.context)
                it.adapter = adapter
            }
            
            itemView.setOnClickListener {
                onClick(activity)
            }
        }
    }
}

