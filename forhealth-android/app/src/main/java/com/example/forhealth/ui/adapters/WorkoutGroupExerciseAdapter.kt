package com.example.forhealth.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.forhealth.R
import com.example.forhealth.models.ActivityItem

class WorkoutGroupExerciseAdapter(
    private val activities: List<ActivityItem>
) : RecyclerView.Adapter<WorkoutGroupExerciseAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivExerciseImage: ImageView = itemView.findViewById(R.id.ivExerciseImage) 
            ?: throw IllegalStateException(
                "View 'ivExerciseImage' (R.id.ivExerciseImage) not found in item_workout_group_exercise.xml. " +
                "Please clean and rebuild the project to ensure R resources are properly generated."
            )
        val tvExerciseName: TextView = itemView.findViewById(R.id.tvExerciseName)
            ?: throw IllegalStateException(
                "View 'tvExerciseName' (R.id.tvExerciseName) not found in item_workout_group_exercise.xml. " +
                "Please clean and rebuild the project to ensure R resources are properly generated."
            )
        val tvExerciseDetails: TextView = itemView.findViewById(R.id.tvExerciseDetails)
            ?: throw IllegalStateException(
                "View 'tvExerciseDetails' (R.id.tvExerciseDetails) not found in item_workout_group_exercise.xml. " +
                "Please clean and rebuild the project to ensure R resources are properly generated."
            )
        val tvExerciseCalories: TextView = itemView.findViewById(R.id.tvExerciseCalories)
            ?: throw IllegalStateException(
                "View 'tvExerciseCalories' (R.id.tvExerciseCalories) not found in item_workout_group_exercise.xml. " +
                "Please clean and rebuild the project to ensure R resources are properly generated."
            )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_group_exercise, parent, false)
        
        // 验证布局是否正确加载
        if (view.findViewById<ImageView>(R.id.ivExerciseImage) == null) {
            android.util.Log.e("WorkoutGroupExerciseAdapter", "Layout item_workout_group_exercise.xml may not be correctly loaded or compiled")
        }
        
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        
        holder.tvExerciseName.text = activity.name
        holder.tvExerciseCalories.text = "${activity.caloriesBurned.toInt()}"
        holder.tvExerciseDetails.text = "${activity.duration} min"
        
        activity.image?.let {
            holder.ivExerciseImage.load(it) {
                placeholder(R.color.orange_100)
                error(R.color.orange_100)
            }
        } ?: run {
            holder.ivExerciseImage.setImageResource(R.drawable.ic_dumbbell)
            holder.ivExerciseImage.setColorFilter(holder.itemView.context.getColor(R.color.orange_600))
        }
    }

    override fun getItemCount() = activities.size
}

