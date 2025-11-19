package com.example.forhealth.food

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.example.forhealth.model.FoodRecord

class FoodRecordsAdapter(
    private val onDeleteClick: (FoodRecord) -> Unit
) : ListAdapter<FoodRecord, FoodRecordsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFoodName: TextView = itemView.findViewById(R.id.tvFoodName)
        private val tvMealType: TextView = itemView.findViewById(R.id.tvMealType)
        private val tvServingInfo: TextView = itemView.findViewById(R.id.tvServingInfo)
        private val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)
        private val tvNutrition: TextView = itemView.findViewById(R.id.tvNutrition)

        fun bind(record: FoodRecord) {
            tvFoodName.text = record.foodName
            tvMealType.text = record.mealType ?: "其他"
            tvServingInfo.text = "${record.servingAmount}份 · ${record.servingSize.toInt()}${record.servingUnit}"
            tvCalories.text = "${record.nutritionData.calories.toInt()}千卡"
            
            val nutritionText = "蛋白${record.nutritionData.protein.toInt()}g " +
                    "脂肪${record.nutritionData.fat.toInt()}g " +
                    "碳水${record.nutritionData.carbohydrates.toInt()}g"
            tvNutrition.text = nutritionText

            // 长按删除
            itemView.setOnLongClickListener {
                onDeleteClick(record)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FoodRecord>() {
        override fun areItemsTheSame(oldItem: FoodRecord, newItem: FoodRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FoodRecord, newItem: FoodRecord): Boolean {
            return oldItem == newItem
        }
    }
}

