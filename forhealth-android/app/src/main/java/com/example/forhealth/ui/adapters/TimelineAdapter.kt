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
import com.example.forhealth.models.WorkoutGroupTimelineItem

class TimelineAdapter(
    private var items: List<TimelineItem>,
    private val onMealGroupClick: (com.example.forhealth.models.MealGroup) -> Unit,
    private val onWorkoutGroupClick: (com.example.forhealth.models.WorkoutGroup) -> Unit
) : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {
    
    fun updateItems(newItems: List<TimelineItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (items[position].itemType) {
            com.example.forhealth.models.ItemType.MEAL_GROUP -> 0
            com.example.forhealth.models.ItemType.WORKOUT_GROUP -> 1
            else -> 0
        }
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
        val item = items[position]
        when (item) {
            is MealGroupTimelineItem -> {
                holder.bindMealGroup(item.mealGroup, onMealGroupClick)
            }
            is WorkoutGroupTimelineItem -> {
                holder.bindWorkoutGroup(item.workoutGroup, onWorkoutGroupClick)
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
            
            val rvMealItems = itemView.findViewById<RecyclerView>(R.id.rvMealItems)
            rvMealItems?.let {
                val adapter = MealGroupFoodAdapter(mealGroup.meals)
                it.layoutManager = LinearLayoutManager(itemView.context)
                it.adapter = adapter
            }
            
            itemView.setOnClickListener {
                onClick(mealGroup)
            }
        }
        
        fun bindWorkoutGroup(workoutGroup: com.example.forhealth.models.WorkoutGroup, onClick: (com.example.forhealth.models.WorkoutGroup) -> Unit) {
            itemView.findViewById<TextView>(R.id.tvWorkoutType)?.text = "Exercise"
            itemView.findViewById<TextView>(R.id.tvWorkoutTime)?.text = workoutGroup.time
            itemView.findViewById<TextView>(R.id.tvTotalCalories)?.text = "${workoutGroup.totalCaloriesBurned.toInt()}"
            
            val rvWorkoutItems = itemView.findViewById<RecyclerView>(R.id.rvWorkoutItems)
            rvWorkoutItems?.let {
                val adapter = WorkoutGroupExerciseAdapter(workoutGroup.activities)
                it.layoutManager = LinearLayoutManager(itemView.context)
                it.adapter = adapter
            }
            
            itemView.setOnClickListener {
                onClick(workoutGroup)
            }
        }
    }
}

