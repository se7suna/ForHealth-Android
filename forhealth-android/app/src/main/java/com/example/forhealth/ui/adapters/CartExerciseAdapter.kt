package com.example.forhealth.ui.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.forhealth.R
import com.example.forhealth.models.SelectedExerciseItem

class CartExerciseAdapter(
    private val items: List<SelectedExerciseItem>,
    private val onDurationChange: (String, Double) -> Unit,
    private val onDurationInput: (String, String) -> Unit,
    private val onDurationBlur: (String) -> Unit,
    private val onRemove: (String) -> Unit,
    private val calculateCalories: (SelectedExerciseItem) -> Double
) : RecyclerView.Adapter<CartExerciseAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCartExerciseImage: ImageView = itemView.findViewById(R.id.ivCartExerciseImage)
        val tvCartExerciseName: TextView = itemView.findViewById(R.id.tvCartExerciseName)
        val tvCartExerciseInfo: TextView = itemView.findViewById(R.id.tvCartExerciseInfo)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)
        val tvCartCalories: TextView = itemView.findViewById(R.id.tvCartCalories)
        val btnDecrease: ImageButton = itemView.findViewById(R.id.btnDecrease)
        val etQuantity: EditText = itemView.findViewById(R.id.etQuantity)
        val tvUnitSuffix: TextView = itemView.findViewById(R.id.tvUnitSuffix)
        val btnIncrease: ImageButton = itemView.findViewById(R.id.btnIncrease)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_exercise, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        // 加载图片
        holder.ivCartExerciseImage.load(item.exerciseItem.image) {
            placeholder(R.color.slate_100)
            error(R.color.slate_100)
        }
        
        // 设置名称和信息
        holder.tvCartExerciseName.text = item.exerciseItem.name
        holder.tvCartExerciseInfo.text = "${item.exerciseItem.caloriesPerUnit.toInt()} 千卡/分钟"
        
        // 设置卡路里
        val calories = calculateCalories(item)
        holder.tvCartCalories.text = "总计: ${calories.toInt()} 千卡"
        
        // 删除按钮
        holder.btnRemove.setOnClickListener {
            onRemove(item.exerciseItem.id)
        }
        
        // 设置数量输入
        setupDurationInput(holder, item)
    }
    
    private var textWatcher: TextWatcher? = null
    
    private fun setupDurationInput(holder: ViewHolder, item: SelectedExerciseItem) {
        // 清除之前的监听器
        textWatcher?.let { holder.etQuantity.removeTextChangedListener(it) }
        
        // 设置当前值
        holder.etQuantity.setText(item.count.toInt().toString())
        
        // 增加按钮（每次增加5分钟）
        holder.btnIncrease.setOnClickListener {
            onDurationChange(item.exerciseItem.id, 5.0)
        }
        
        // 减少按钮（每次减少5分钟）
        holder.btnDecrease.setOnClickListener {
            onDurationChange(item.exerciseItem.id, -5.0)
        }
        
        // 输入监听
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString() ?: ""
                if (value.matches(Regex("^\\d*$"))) {
                    onDurationInput(item.exerciseItem.id, value)
                }
            }
        }
        holder.etQuantity.addTextChangedListener(textWatcher)
        
        // 失去焦点时验证
        holder.etQuantity.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                onDurationBlur(item.exerciseItem.id)
            }
        }
    }

    override fun getItemCount() = items.size
}

