package com.example.forhealth.food

import android.view.LayoutInflater
import android.view.View
import com.bumptech.glide.Glide
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.example.forhealth.model.DailyRecordItem
import com.example.forhealth.model.FoodRecord
import android.widget.ImageView
class DailyRecordsAdapter(
    private val onFoodLongClick: (FoodRecord) -> Unit,
    private val onSportsLongClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_FOOD = 0
        private const val TYPE_SPORTS = 1
    }

    private var items = listOf<DailyRecordItem>()

    fun submitList(list: List<DailyRecordItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DailyRecordItem.FoodItem -> TYPE_FOOD
            is DailyRecordItem.SportsItem -> TYPE_SPORTS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FOOD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_food_record, parent, false)
                FoodViewHolder(view)
            }
            TYPE_SPORTS -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_sports_record, parent, false)
                SportsViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DailyRecordItem.FoodItem -> (holder as FoodViewHolder).bind(item.record)
            is DailyRecordItem.SportsItem -> (holder as SportsViewHolder).bind(item.record)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivFoodImage: ImageView = itemView.findViewById(R.id.ivFoodImage)
        private val tvFoodName: TextView = itemView.findViewById(R.id.tvFoodName)
        private val tvMealType: TextView = itemView.findViewById(R.id.tvMealType)
        private val tvServingInfo: TextView = itemView.findViewById(R.id.tvServingInfo)
        private val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)
        private val tvNutrition: TextView = itemView.findViewById(R.id.tvNutrition)

        fun bind(record: FoodRecord) {
            // 设置食物名称
            tvFoodName.text = record.foodName
            tvMealType.text = record.mealType ?: "其他"

            // 显示份量信息
            val servingAmount = if (record.servingAmount % 1.0 == 0.0) {
                record.servingAmount.toInt().toString()
            } else {
                String.format("%.1f", record.servingAmount)
            }
            val servingSize = if (record.servingSize % 1.0 == 0.0) {
                record.servingSize.toInt().toString()
            } else {
                String.format("%.1f", record.servingSize)
            }
            val servingUnit = record.servingUnit ?: "克"
            tvServingInfo.text = "${servingAmount}份 · ${servingSize}${servingUnit}"

            // 显示卡路里
            tvCalories.text = "+${record.nutritionData.calories.toInt()}"

            // 显示营养素信息
            val protein = record.nutritionData.protein.toInt()
            val fat = record.nutritionData.fat.toInt()
            val carbs = record.nutritionData.carbohydrates.toInt()
            tvNutrition.text = "蛋白${protein}g 脂肪${fat}g 碳水${carbs}g"

            // 使用 Glide 加载食物图片
            Glide.with(itemView.context)
                .load(record.imageUrl)
                 .into(ivFoodImage) // 将图片加载到 ImageView 中

            itemView.setOnLongClickListener {
                onFoodLongClick(record)
                true
            }
        }
    }


    inner class SportsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivSportImage: ImageView = itemView.findViewById(R.id.ivSportImage) // 添加 ImageView 用于显示图片
        private val tvSportName: TextView = itemView.findViewById(R.id.tvSportName)
        private val tvSportDuration: TextView = itemView.findViewById(R.id.tvSportDuration)
        private val tvCaloriesBurned: TextView = itemView.findViewById(R.id.tvCaloriesBurned)

        fun bind(record: com.example.forhealth.model.SearchSportRecordsResponse) {
            // 设置运动名称
            tvSportName.text = record.sportType ?: "运动"

            // 设置运动时长
            tvSportDuration.text = "${record.durationTime ?: 0}分钟"

            // 设置消耗的卡路里
            tvCaloriesBurned.text = "-${record.caloriesBurned?.toInt() ?: 0}"

            // 使用 Glide 加载图片
            //Glide.with(itemView.context)
            //    .load(record.imageUrl) // 假设后端返回了 imageUrl 字段
            //    .into(ivSportImage) // 将图片加载到 ImageView 中

            itemView.setOnLongClickListener {
                record.recordId?.let { onSportsLongClick(it) }
                true
            }
        }
    }

}

