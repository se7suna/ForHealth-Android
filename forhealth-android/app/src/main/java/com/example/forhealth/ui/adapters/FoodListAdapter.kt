package com.example.forhealth.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.forhealth.R
import com.example.forhealth.models.FoodItem
import com.example.forhealth.models.SelectedFoodItem
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FoodListAdapter(
    private val foodList: List<FoodItem>,
    private val selectedItems: List<SelectedFoodItem>,
    private val onAddClick: (FoodItem) -> Unit
) : RecyclerView.Adapter<FoodListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivFoodImage: ImageView = itemView.findViewById(R.id.ivFoodImage)
        val tvFoodName: TextView = itemView.findViewById(R.id.tvFoodName)
        val tvFoodInfo: TextView = itemView.findViewById(R.id.tvFoodInfo)
        val fabAdd: FloatingActionButton = itemView.findViewById(R.id.fabAdd)
        val layoutCount: ViewGroup = itemView.findViewById(R.id.layoutCount)
        val tvCount: TextView = itemView.findViewById(R.id.tvCount)
        val fabAddMore: FloatingActionButton = itemView.findViewById(R.id.fabAddMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food = foodList[position]
        val selected = selectedItems.find { it.foodItem.id == food.id }

        // 加载图片（将 127.0.0.1 映射为 Android 模拟器可访问的 10.0.2.2
        val imageUrl = food.image
        // val imageUrl = food.image.replace("127.0.0.1", "10.0.2.2")
        holder.ivFoodImage.load(imageUrl) {
            placeholder(R.color.slate_100)
            error(R.color.slate_100)
        }

        // 设置名称和信息
        holder.tvFoodName.text = food.name
        // 后端计算的值是每百克或每百毫升，所以显示为 "100 kcal / 100g" 或 "100 kcal / 100ml"
        // 但对于自定义食物（单位不是标准"克"或"毫升"），保持原单位显示
        val unitDisplay = if (food.gramsPerUnit == 100.0 && (food.unit == "克" || food.unit == "毫升")) {
            // 标准格式：每100g或100ml
            "100${food.unit}"
        } else {
            // 自定义食物，保持原单位显示（如 "1 serving"）
            food.unit
        }
        holder.tvFoodInfo.text = "${food.calories.toInt()} 千卡 / $unitDisplay"

        // 根据是否已选择显示不同的UI
        if (selected != null) {
            holder.fabAdd.visibility = View.GONE
            holder.layoutCount.visibility = View.VISIBLE
            val countText = if (selected.mode == com.example.forhealth.models.QuantityMode.GRAM) {
                "x${selected.count.toInt()}克"
            } else {
                "x${selected.count}"
            }
            holder.tvCount.text = countText
            holder.fabAddMore.setOnClickListener { onAddClick(food) }
        } else {
            holder.fabAdd.visibility = View.VISIBLE
            holder.layoutCount.visibility = View.GONE
            holder.fabAdd.setOnClickListener { onAddClick(food) }
        }
    }

    override fun getItemCount() = foodList.size
}

