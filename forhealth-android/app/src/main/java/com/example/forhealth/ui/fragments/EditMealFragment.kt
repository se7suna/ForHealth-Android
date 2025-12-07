package com.example.forhealth.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.forhealth.R
import com.example.forhealth.databinding.FragmentAddMealBinding
import com.example.forhealth.models.*
import com.example.forhealth.ui.adapters.CartFoodAdapter
import com.example.forhealth.ui.adapters.FoodListAdapter
import com.example.forhealth.utils.Constants
import com.example.forhealth.utils.DateUtils
import com.google.android.material.button.MaterialButton
import java.util.*

class EditMealFragment : DialogFragment() {
    
    private var _binding: FragmentAddMealBinding? = null
    private val binding get() = _binding!!
    
    private var onMealUpdatedListener: ((MealGroup) -> Unit)? = null
    private var onMealDeletedListener: ((String) -> Unit)? = null
    
    private val selectedItems = mutableListOf<SelectedFoodItem>()
    private var selectedType: MealType = MealType.BREAKFAST
    private var filteredFood: List<FoodItem> = Constants.FOOD_DB
    private var isCartExpanded = false
    private var originalMealGroup: MealGroup? = null
    
    private lateinit var foodAdapter: FoodListAdapter
    private lateinit var cartAdapter: CartFoodAdapter
    
    fun setOnMealUpdatedListener(listener: (MealGroup) -> Unit) {
        onMealUpdatedListener = listener
    }
    
    fun setOnMealDeletedListener(listener: (String) -> Unit) {
        onMealDeletedListener = listener
    }
    
    fun setMealGroup(mealGroup: MealGroup) {
        originalMealGroup = mealGroup
        selectedType = mealGroup.type
        
        // 将MealItem转换为SelectedFoodItem
        selectedItems.clear()
        mealGroup.meals.forEach { meal ->
            // 从meal反向查找对应的FoodItem
            val foodItem = Constants.FOOD_DB.find { it.name == meal.name }
            if (foodItem != null) {
                // 计算数量（假设是unit模式，如果calories匹配则使用unit，否则计算gram）
                val ratio = meal.calories / foodItem.calories
                val count = if (ratio >= 0.9 && ratio <= 1.1) {
                    // 接近1，可能是unit模式
                    1.0
                } else {
                    // 计算gram
                    meal.calories / foodItem.calories * foodItem.gramsPerUnit
                }
                val mode = if (ratio >= 0.9 && ratio <= 1.1) QuantityMode.UNIT else QuantityMode.GRAM
                
                selectedItems.add(SelectedFoodItem(
                    foodItem = foodItem,
                    count = count,
                    mode = mode
                ))
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_NoTitleBar_Fullscreen)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMealBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 更新标题
        val titleView = binding.root.findViewById<android.widget.TextView>(R.id.tvTitle)
        titleView?.text = "Edit Meal"
        
        setupCloseButton()
        setupSearchBar()
        setupMealTypeTabs()
        setupCreateCustomFoodButton()
        setupFoodList()
        setupCart()
        
        // 初始显示购物车
        if (selectedItems.isNotEmpty()) {
            updateCart()
            isCartExpanded = true
            toggleCartExpansion()
        }
    }
    
    private fun setupCloseButton() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterFood(s?.toString() ?: "")
            }
        })
    }
    
    private fun setupMealTypeTabs() {
        val mealTypes = listOf(
            MealType.BREAKFAST to "Breakfast",
            MealType.LUNCH to "Lunch",
            MealType.DINNER to "Dinner",
            MealType.SNACK to "Snack"
        )
        
        binding.layoutMealTypes.removeAllViews()
        
        mealTypes.forEach { (type, label) ->
            val button = MaterialButton(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = type == selectedType
                setOnClickListener {
                    selectedType = type
                    updateMealTypeButtons()
                }
            }
            binding.layoutMealTypes.addView(button)
        }
        
        updateMealTypeButtons()
    }
    
    private fun updateMealTypeButtons() {
        for (i in 0 until binding.layoutMealTypes.childCount) {
            val button = binding.layoutMealTypes.getChildAt(i) as MaterialButton
            val type = when (i) {
                0 -> MealType.BREAKFAST
                1 -> MealType.LUNCH
                2 -> MealType.DINNER
                else -> MealType.SNACK
            }
            button.isChecked = type == selectedType
            
            if (type == selectedType) {
                button.setBackgroundColor(resources.getColor(R.color.emerald_600, null))
                button.setTextColor(resources.getColor(R.color.white, null))
            } else {
                button.setBackgroundColor(resources.getColor(R.color.slate_100, null))
                button.setTextColor(resources.getColor(R.color.slate_600, null))
            }
        }
    }
    
    private fun setupCreateCustomFoodButton() {
        binding.btnCreateCustomFood.setOnClickListener {
            val customFoodFragment = CustomFoodFragment().apply {
                setOnCustomFoodCreatedListener { foodItem ->
                    // 将自定义食物添加到购物车
                    addToCart(foodItem)
                    // 展开购物车
                    if (!isCartExpanded) {
                        toggleCartExpansion()
                    }
                }
            }
            customFoodFragment.show(parentFragmentManager, "CustomFoodFragment")
        }
    }
    
    private fun setupFoodList() {
        foodAdapter = FoodListAdapter(
            foodList = filteredFood,
            selectedItems = selectedItems,
            onAddClick = { food -> addToCart(food) }
        )
        
        binding.rvFoodList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFoodList.adapter = foodAdapter
    }
    
    private fun filterFood(query: String) {
        filteredFood = if (query.isBlank()) {
            Constants.FOOD_DB
        } else {
            Constants.FOOD_DB.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        updateFoodList()
    }
    
    private fun updateFoodList() {
        foodAdapter = FoodListAdapter(
            foodList = filteredFood,
            selectedItems = selectedItems,
            onAddClick = { food -> addToCart(food) }
        )
        binding.rvFoodList.adapter = foodAdapter
    }
    
    private fun addToCart(food: FoodItem) {
        val existing = selectedItems.find { it.foodItem.id == food.id }
        
        if (existing != null) {
            val increment = if (existing.mode == QuantityMode.GRAM) 10.0 else 1.0
            existing.count += increment
        } else {
            selectedItems.add(SelectedFoodItem(
                foodItem = food,
                count = 1.0,
                mode = QuantityMode.UNIT
            ))
        }
        
        updateFoodList()
        updateCart()
    }
    
    private fun setupCart() {
        binding.layoutCartHeader.setOnClickListener {
            toggleCartExpansion()
        }
        
        binding.btnAddNowCollapsed.setOnClickListener {
            saveMeals()
        }
        
        binding.btnSave.setOnClickListener {
            saveMeals()
        }
        
        binding.btnDeleteRecord.setOnClickListener {
            originalMealGroup?.id?.let { mealGroupId ->
                onMealDeletedListener?.invoke(mealGroupId)
            }
            dismiss()
        }
        
        cartAdapter = CartFoodAdapter(
            items = selectedItems,
            onModeToggle = { foodId -> toggleMode(foodId) },
            onQuantityChange = { foodId, delta -> updateQuantity(foodId, delta) },
            onQuantityInput = { foodId, value -> handleQuantityInput(foodId, value) },
            onQuantityBlur = { foodId -> handleQuantityBlur(foodId) },
            onRemove = { foodId -> removeItem(foodId) },
            calculateMacros = { item -> calculateItemMacros(item).calories }
        )
        
        binding.rvCartItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCartItems.adapter = cartAdapter
        
        updateCart()
    }
    
    private fun toggleCartExpansion() {
        isCartExpanded = !isCartExpanded
        
        if (isCartExpanded) {
            binding.scrollCartContent.visibility = View.VISIBLE
            binding.layoutCartFooter.visibility = View.VISIBLE
            binding.btnAddNowCollapsed.visibility = View.GONE
            binding.ivCartChevron.setImageResource(R.drawable.ic_chevron_down)
        } else {
            binding.scrollCartContent.visibility = View.GONE
            binding.layoutCartFooter.visibility = View.GONE
            binding.btnAddNowCollapsed.visibility = View.VISIBLE
            binding.ivCartChevron.setImageResource(R.drawable.ic_chevron_up)
        }
    }
    
    private fun toggleMode(foodId: String) {
        val item = selectedItems.find { it.foodItem.id == foodId } ?: return
        val currentCount = item.count
        val newMode = if (item.mode == QuantityMode.UNIT) QuantityMode.GRAM else QuantityMode.UNIT
        
        val newCount = if (newMode == QuantityMode.GRAM) {
            (currentCount * item.foodItem.gramsPerUnit).toInt().toDouble()
        } else {
            ((currentCount / item.foodItem.gramsPerUnit) * 10).toInt() / 10.0
        }
        
        val finalCount = if (newCount <= 0) {
            if (newMode == QuantityMode.GRAM) 10.0 else 0.5
        } else {
            newCount
        }
        
        item.mode = newMode
        item.count = finalCount
        
        updateCartAdapter()
        updateCart()
    }
    
    private fun updateQuantity(foodId: String, delta: Double) {
        val item = selectedItems.find { it.foodItem.id == foodId } ?: return
        val minVal = if (item.mode == QuantityMode.GRAM) 1.0 else 0.5
        val newCount = maxOf(minVal, item.count + delta)
        item.count = newCount
        
        updateCartAdapter()
        updateCart()
    }
    
    private fun handleQuantityInput(foodId: String, value: String) {
        val item = selectedItems.find { it.foodItem.id == foodId } ?: return
        if (value.isBlank()) return
        val parsed = value.toDoubleOrNull()
        if (parsed != null && parsed > 0) {
            item.count = parsed
            updateCart()
        }
    }
    
    private fun handleQuantityBlur(foodId: String) {
        val item = selectedItems.find { it.foodItem.id == foodId } ?: return
        val minVal = if (item.mode == QuantityMode.GRAM) 10.0 else 1.0
        
        if (item.count <= 0) {
            item.count = minVal
        }
        
        updateCartAdapter()
        updateCart()
    }
    
    private fun removeItem(foodId: String) {
        selectedItems.removeAll { it.foodItem.id == foodId }
        if (selectedItems.isEmpty()) {
            isCartExpanded = false
        }
        updateCartAdapter()
        updateFoodList()
        updateCart()
    }
    
    private fun updateCartAdapter() {
        cartAdapter = CartFoodAdapter(
            items = selectedItems,
            onModeToggle = { foodId -> toggleMode(foodId) },
            onQuantityChange = { foodId, delta -> updateQuantity(foodId, delta) },
            onQuantityInput = { foodId, value -> handleQuantityInput(foodId, value) },
            onQuantityBlur = { foodId -> handleQuantityBlur(foodId) },
            onRemove = { foodId -> removeItem(foodId) },
            calculateMacros = { item -> calculateItemMacros(item).calories }
        )
        binding.rvCartItems.adapter = cartAdapter
    }
    
    private fun updateCart() {
        if (selectedItems.isEmpty()) {
            // 隐藏购物车
            binding.layoutCart.visibility = View.GONE
            // 如果是编辑模式且购物车为空，显示独立的删除按钮
            if (originalMealGroup != null) {
                binding.layoutDeleteButton.visibility = View.VISIBLE
            } else {
                binding.layoutDeleteButton.visibility = View.GONE
            }
        } else {
            // 显示购物车，隐藏删除按钮
            binding.layoutCart.visibility = View.VISIBLE
            binding.layoutDeleteButton.visibility = View.GONE
            
            val totalCalories = selectedItems.sumOf { calculateItemMacros(it).calories }
            binding.tvCartTotalCalories.text = "${totalCalories.toInt()} kcal"
            
            if (selectedItems.size > 0) {
                binding.tvCartBadge.text = selectedItems.size.toString()
                binding.tvCartBadge.visibility = View.VISIBLE
            } else {
                binding.tvCartBadge.visibility = View.GONE
            }
            
            // 确保购物车内容区域在展开时可见
            if (isCartExpanded) {
                binding.scrollCartContent.visibility = View.VISIBLE
                binding.layoutCartFooter.visibility = View.VISIBLE
                binding.btnAddNowCollapsed.visibility = View.GONE
            }
            
            // 编辑模式下显示"ADD MEAL"，添加模式下显示"Confirm & Add"
            if (originalMealGroup != null) {
                binding.btnSave.text = getString(R.string.add_meal)
            } else {
                binding.btnSave.text = "Confirm & Add ${selectedItems.size} items"
            }
            binding.btnSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.emerald_600, null)
            ))
        }
    }
    
    private fun calculateItemMacros(item: SelectedFoodItem): MacroResult {
        val ratio = if (item.mode == QuantityMode.GRAM) {
            item.count / item.foodItem.gramsPerUnit
        } else {
            item.count
        }
        
        return MacroResult(
            calories = item.foodItem.calories * ratio,
            protein = item.foodItem.protein * ratio,
            carbs = item.foodItem.carbs * ratio,
            fat = item.foodItem.fat * ratio
        )
    }
    
    private fun saveMeals() {
        // 如果购物车为空，删除整个meal group
        if (selectedItems.isEmpty()) {
            originalMealGroup?.id?.let { mealGroupId ->
                onMealDeletedListener?.invoke(mealGroupId)
            }
            dismiss()
            return
        }
        
        val meals = selectedItems.map { item ->
            val macros = calculateItemMacros(item)
            MealItem(
                id = UUID.randomUUID().toString(),
                name = item.foodItem.name,
                calories = macros.calories,
                protein = macros.protein,
                carbs = macros.carbs,
                fat = macros.fat,
                time = originalMealGroup?.time ?: DateUtils.getCurrentTime(),
                type = selectedType,
                image = item.foodItem.image
            )
        }
        
        val mealGroup = MealGroup(
            id = originalMealGroup?.id ?: UUID.randomUUID().toString(),
            meals = meals,
            time = originalMealGroup?.time ?: DateUtils.getCurrentTime(),
            type = selectedType
        )
        
        onMealUpdatedListener?.invoke(mealGroup)
        dismiss()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private data class MacroResult(
        val calories: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double
    )
}

