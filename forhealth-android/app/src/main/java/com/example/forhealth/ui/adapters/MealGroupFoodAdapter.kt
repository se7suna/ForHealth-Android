package com.example.forhealth.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.forhealth.R
import com.example.forhealth.models.MealItem

class MealGroupFoodAdapter(
    private val meals: List<MealItem>
) : RecyclerView.Adapter<MealGroupFoodAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivFoodImage: ImageView = itemView.findViewById(R.id.ivFoodImage)
        val tvFoodName: TextView = itemView.findViewById(R.id.tvFoodName)
        val tvFoodDetails: TextView = itemView.findViewById(R.id.tvFoodDetails)
        val tvFoodCalories: TextView = itemView.findViewById(R.id.tvFoodCalories)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_group_food, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meal = meals[position]
        
        holder.tvFoodName.text = meal.name
        holder.tvFoodCalories.text = "${meal.calories.toInt()}"
        holder.tvFoodDetails.text = "${meal.protein.toInt()}P • ${meal.carbs.toInt()}C • ${meal.fat.toInt()}F"
        
        meal.image?.let {
            holder.ivFoodImage.load(it) {
                placeholder(R.color.slate_100)
                error(R.color.slate_100)
            }
        } ?: run {
            holder.ivFoodImage.setImageResource(R.drawable.ic_utensils)
            holder.ivFoodImage.setColorFilter(holder.itemView.context.getColor(R.color.emerald_600))
        }
    }

    override fun getItemCount() = meals.size
}

