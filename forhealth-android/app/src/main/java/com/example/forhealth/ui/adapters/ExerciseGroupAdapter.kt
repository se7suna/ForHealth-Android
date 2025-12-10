package com.example.forhealth.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.decode.SvgDecoder
import com.example.forhealth.R
import com.example.forhealth.models.ActivityItem

class ExerciseGroupAdapter(
    private val activities: List<ActivityItem>
) : RecyclerView.Adapter<ExerciseGroupAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivExerciseImage: ImageView = itemView.findViewById(R.id.ivExerciseImage)
        val tvExerciseName: TextView = itemView.findViewById(R.id.tvExerciseName)
        val tvExerciseDetails: TextView = itemView.findViewById(R.id.tvExerciseDetails)
        val tvExerciseCalories: TextView = itemView.findViewById(R.id.tvExerciseCalories)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_group_exercise, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        
        holder.tvExerciseName.text = activity.name
        holder.tvExerciseCalories.text = "${activity.caloriesBurned.toInt()}"
        holder.tvExerciseDetails.text = "${activity.duration} min"
        
        activity.image?.let {
            val imageUrl = it
            // val imageUrl = it.replace("127.0.0.1", "10.0.2.2")
            holder.ivExerciseImage.load(imageUrl) {
                decoderFactory { result, options, _ -> SvgDecoder(result.source, options) }
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

