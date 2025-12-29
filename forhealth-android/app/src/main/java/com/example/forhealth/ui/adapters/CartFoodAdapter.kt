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
import com.example.forhealth.models.QuantityMode
import com.example.forhealth.models.SelectedFoodItem

class CartFoodAdapter(
    private val items: List<SelectedFoodItem>,
    private val onModeToggle: (String) -> Unit,
    private val onQuantityChange: (String, Double) -> Unit,
    private val onQuantityInput: (String, String) -> Unit,
    private val onQuantityBlur: (String) -> Unit,
    private val onRemove: (String) -> Unit,
    private val onPickImage: (String) -> Unit,
    private val calculateMacros: (SelectedFoodItem) -> Double
) : RecyclerView.Adapter<CartFoodAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCartFoodImage: ImageView = itemView.findViewById(R.id.ivCartFoodImage)
        val tvCartFoodName: TextView = itemView.findViewById(R.id.tvCartFoodName)
        val layoutUnitSwitcher: ViewGroup = itemView.findViewById(R.id.layoutUnitSwitcher)
        val tvUnitButton: TextView = itemView.findViewById(R.id.tvUnitButton)
        val tvGramButton: TextView = itemView.findViewById(R.id.tvGramButton)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)
        val tvCartCalories: TextView = itemView.findViewById(R.id.tvCartCalories)
        val btnDecrease: ImageButton = itemView.findViewById(R.id.btnDecrease)
        val etQuantity: EditText = itemView.findViewById(R.id.etQuantity)
        val tvUnitSuffix: TextView = itemView.findViewById(R.id.tvUnitSuffix)
        val btnIncrease: ImageButton = itemView.findViewById(R.id.btnIncrease)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_food, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        // 加载图片
        holder.ivCartFoodImage.load(item.foodItem.image) {
            placeholder(R.color.slate_100)
            error(R.color.slate_100)
        }
        holder.ivCartFoodImage.setOnClickListener {
            onPickImage(item.foodItem.id)
        }
        
        // 设置名称
        holder.tvCartFoodName.text = item.foodItem.name
        
        // 设置单位切换器
        setupUnitSwitcher(holder, item)
        
        // 设置数量输入
        setupQuantityInput(holder, item)
        
        // 设置卡路里
        val calories = calculateMacros(item)
        holder.tvCartCalories.text = "${calories.toInt()} 千卡"
        
        // 删除按钮
        holder.btnRemove.setOnClickListener {
            onRemove(item.foodItem.id)
        }
    }
    
    private fun setupUnitSwitcher(holder: ViewHolder, item: SelectedFoodItem) {
        // 每次重新创建按钮以确保使用最新的 item 状态
        holder.layoutUnitSwitcher.removeAllViews()
        
        val foodId = item.foodItem.id
        
        // Unit 按钮 - 将"份"和"克"统一显示为"份"
        val unitText = when (item.foodItem.unit) {
            "份", "克" -> "份"
            else -> item.foodItem.unit
        }
        val unitButton = TextView(holder.itemView.context).apply {
            text = unitText
            setPadding(
                holder.itemView.context.resources.getDimensionPixelSize(R.dimen.spacing_8),
                holder.itemView.context.resources.getDimensionPixelSize(R.dimen.spacing_4),
                holder.itemView.context.resources.getDimensionPixelSize(R.dimen.spacing_8),
                holder.itemView.context.resources.getDimensionPixelSize(R.dimen.spacing_4)
            )
            textSize = 10f
            setOnClickListener {
                // 如果当前是 GRAM 模式，切换到 UNIT
                if (item.mode == QuantityMode.GRAM) {
                    onModeToggle(foodId)
                }
            }
        }
        
        // Gram 按钮 - 改为"克"
        val gramButton = TextView(holder.itemView.context).apply {
            text = "克"
            setPadding(
                holder.itemView.context.resources.getDimensionPixelSize(R.dimen.spacing_8),
                holder.itemView.context.resources.getDimensionPixelSize(R.dimen.spacing_4),
                holder.itemView.context.resources.getDimensionPixelSize(R.dimen.spacing_8),
                holder.itemView.context.resources.getDimensionPixelSize(R.dimen.spacing_4)
            )
            textSize = 10f
            setOnClickListener {
                // 如果当前是 UNIT 模式，切换到 GRAM
                if (item.mode == QuantityMode.UNIT) {
                    onModeToggle(foodId)
                }
            }
        }
        
        holder.layoutUnitSwitcher.addView(unitButton)
        holder.layoutUnitSwitcher.addView(gramButton)
        
        // 更新按钮状态
        if (item.mode == QuantityMode.UNIT) {
            unitButton.setBackgroundResource(R.drawable.bg_emerald_50)
            unitButton.setTextColor(holder.itemView.context.getColor(R.color.text_primary))
            gramButton.background = null
            gramButton.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
        } else {
            gramButton.setBackgroundResource(R.drawable.bg_emerald_50)
            gramButton.setTextColor(holder.itemView.context.getColor(R.color.text_primary))
            unitButton.background = null
            unitButton.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
        }
    }
    
    private var textWatcher: TextWatcher? = null
    
    private fun setupQuantityInput(holder: ViewHolder, item: SelectedFoodItem) {
        // 清除之前的监听器
        textWatcher?.let { holder.etQuantity.removeTextChangedListener(it) }
        
        // 设置当前值
        holder.etQuantity.setText(item.count.toString())
        
        // 设置单位后缀
        if (item.mode == QuantityMode.GRAM) {
            holder.tvUnitSuffix.text = "g"
            holder.tvUnitSuffix.visibility = View.VISIBLE
        } else {
            holder.tvUnitSuffix.visibility = View.GONE
        }
        
        // 增加按钮
        holder.btnIncrease.setOnClickListener {
            val delta = if (item.mode == QuantityMode.GRAM) 10.0 else 0.5
            onQuantityChange(item.foodItem.id, delta)
        }
        
        // 减少按钮
        holder.btnDecrease.setOnClickListener {
            val delta = if (item.mode == QuantityMode.GRAM) -10.0 else -0.5
            onQuantityChange(item.foodItem.id, delta)
        }
        
        // 输入监听
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString() ?: ""
                if (value.matches(Regex("^\\d*\\.?\\d*$"))) {
                    onQuantityInput(item.foodItem.id, value)
                }
            }
        }
        holder.etQuantity.addTextChangedListener(textWatcher)
        
        // 失去焦点时验证
        holder.etQuantity.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                onQuantityBlur(item.foodItem.id)
            }
        }
    }

    override fun getItemCount() = items.size
}

