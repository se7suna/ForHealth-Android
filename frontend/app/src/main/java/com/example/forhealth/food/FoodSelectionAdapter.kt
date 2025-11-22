package com.example.forhealth.food

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.example.forhealth.model.SimplifiedFoodSearchItem

class FoodSelectionAdapter(
    private val onAddClick: (SimplifiedFoodSearchItem) -> Unit,
    private val selectedFoods: MutableMap<String, FoodSelectionActivity.SelectedFoodItem>
) : ListAdapter<SimplifiedFoodSearchItem, FoodSelectionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivFoodImage: ImageView = itemView.findViewById(R.id.ivFoodImage)
        private val tvFoodName: TextView = itemView.findViewById(R.id.tvFoodName)
        private val tvFoodCalories: TextView = itemView.findViewById(R.id.tvFoodCalories)
        private val btnAdd: Button = itemView.findViewById(R.id.btnAdd)
        private val indicatorSelected: View = itemView.findViewById(R.id.indicatorSelected)

        fun bind(food: SimplifiedFoodSearchItem) {
            tvFoodName.text = food.name
            
            val caloriesText = "${food.nutrition.calories.toInt()} 千卡/${food.weight.toInt()}${food.weightUnit}"
            tvFoodCalories.text = caloriesText

            // 检查是否已选中
            val foodId = food.foodId ?: ""
            val isSelected = selectedFoods.containsKey(foodId)
            indicatorSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

            btnAdd.setOnClickListener {
                onAddClick(food)
            }
            Glide.with(itemView.context)
                .load(food.imageUrl) // 加载图片 URL
                .into(ivFoodImage) // 将图片加载到 ImageView 中
            // 可以在这里加载图片（使用Glide或其他库）
            // Glide.with(itemView.context).load(food.imageUrl).into(ivFoodImage)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SimplifiedFoodSearchItem>() {
        override fun areItemsTheSame(
            oldItem: SimplifiedFoodSearchItem,
            newItem: SimplifiedFoodSearchItem
        ): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(
            oldItem: SimplifiedFoodSearchItem,
            newItem: SimplifiedFoodSearchItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}

