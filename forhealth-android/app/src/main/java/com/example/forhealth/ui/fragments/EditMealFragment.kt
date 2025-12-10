package com.example.forhealth.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.forhealth.R
import com.example.forhealth.databinding.FragmentAddMealBinding
import com.example.forhealth.models.*
import com.example.forhealth.network.ApiResult
import com.example.forhealth.repositories.FoodRepository
import com.example.forhealth.ui.adapters.CartFoodAdapter
import com.example.forhealth.ui.adapters.FoodListAdapter
import com.example.forhealth.utils.DateUtils
import com.example.forhealth.viewmodels.MainViewModel
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.launch

class EditMealFragment : DialogFragment() {
    
    private var _binding: FragmentAddMealBinding? = null
    private val binding get() = _binding!!
    
    private var onMealUpdatedListener: ((MealGroup) -> Unit)? = null
    private var onMealDeletedListener: ((String) -> Unit)? = null
    
    private val selectedItems = mutableListOf<SelectedFoodItem>()
    private var selectedType: MealType = MealType.BREAKFAST
    private val customFoods = mutableListOf<FoodItem>() // 保存的自定义食物
    private var remoteFoods: List<FoodItem> = emptyList()
    private var filteredFood: List<FoodItem> = emptyList()
    private var currentQuery: String = ""
    private var isCartExpanded = false
    private var originalMealGroup: MealGroup? = null
    
    private lateinit var foodAdapter: FoodListAdapter
    private lateinit var cartAdapter: CartFoodAdapter
    private val foodRepository = FoodRepository()
    private lateinit var mainViewModel: MainViewModel
    
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
            // 从meal创建FoodItem（使用meal中的信息）
            // 直接使用meal中的信息创建FoodItem，因为我们已经有了完整的营养信息
            val foodItem = createFoodItemFromMeal(meal)
            
            // 计算数量：根据servingAmount或通过营养值反推
            val servingAmount = meal.servingAmount ?: 1.0
            val count: Double
            val mode: QuantityMode
            
            // 如果servingAmount接近1，可能是unit模式；否则计算gram
            if (servingAmount >= 0.9 && servingAmount <= 1.1) {
                count = 1.0
                mode = QuantityMode.UNIT
            } else {
                // 根据营养值反推gram数
                val ratio = if (foodItem.calories > 0) {
                    meal.calories / foodItem.calories
                } else {
                    servingAmount
                }
                count = ratio * foodItem.gramsPerUnit
                mode = QuantityMode.GRAM
            }
            
            selectedItems.add(SelectedFoodItem(
                foodItem = foodItem,
                count = count,
                mode = mode
            ))
        }
    }
    
    private fun createFoodItemFromMeal(meal: MealItem): FoodItem {
        // 使用meal中的信息创建FoodItem
        // 根据servingAmount计算每单位营养值
        val servingAmount = meal.servingAmount ?: 1.0
        
        // 默认使用100g作为单位（如果无法确定）
        // 如果servingAmount是1，假设是1份（100g）；否则假设是gram数
        val defaultGramsPerUnit = if (servingAmount >= 0.9 && servingAmount <= 1.1) {
            100.0 // 1份 = 100g
        } else {
            servingAmount * 100.0 // 假设servingAmount是份数，每份100g
        }
        
        return FoodItem(
            id = meal.foodId ?: meal.id, // 使用foodId或record id
            name = meal.name,
            calories = meal.calories / servingAmount, // 每单位卡路里
            protein = meal.protein / servingAmount,
            carbs = meal.carbs / servingAmount,
            fat = meal.fat / servingAmount,
            unit = "份",
            gramsPerUnit = defaultGramsPerUnit,
            image = meal.image ?: ""
        )
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
        
        // 复用父Fragment的MainViewModel（若失败则退回Activity作用域）
        mainViewModel = try {
            ViewModelProvider(requireParentFragment())[MainViewModel::class.java]
        } catch (e: Exception) {
            ViewModelProvider(requireActivity())[MainViewModel::class.java]
        }
        
        setupCloseButton()
        setupSearchBar()
        setupMealTypeTabs()
        setupCreateCustomFoodButton()
        setupFoodList()
        setupCart()
        
        // 初始化食物列表（调用仓库）
        fetchFoods()
        
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
                currentQuery = s?.toString()?.trim() ?: ""
                applyFoodFilter()
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
                    // 将自定义食物添加到本地列表和购物车
                    customFoods.add(foodItem)
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
    
    private fun applyFoodFilter() {
        val query = currentQuery
        val combined = remoteFoods + customFoods.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true)
        }
        filteredFood = if (query.isBlank()) {
            combined
        } else {
            combined.filter { it.name.contains(query, ignoreCase = true) }
        }
        updateFoodList()
    }
    
    private fun fetchFoods() {
        lifecycleScope.launch {
            when (val result = foodRepository.searchFoods()) {
                is ApiResult.Success -> {
                    remoteFoods = parseFoods(result.data)
                    applyFoodFilter()
                }
                is ApiResult.Error -> {
                    android.widget.Toast.makeText(requireContext(), result.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is ApiResult.Loading -> { /* no-op */ }
            }
        }
    }
    
    private fun parseFoods(data: Any): List<FoodItem> {
        val gson = Gson()
        return try {
            val simplified = gson.fromJson(gson.toJson(data), com.example.forhealth.network.dto.food.SimplifiedFoodListResponse::class.java)
            simplified.foods.map { mapSimplifiedItem(it) }
        } catch (_: Exception) {
            try {
                val full = gson.fromJson(gson.toJson(data), com.example.forhealth.network.dto.food.FoodListResponse::class.java)
                full.foods.map { mapFullItem(it) }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
    
    private fun mapSimplifiedItem(item: com.example.forhealth.network.dto.food.SimplifiedFoodSearchItem): FoodItem {
        val id = item.food_id ?: item.boohee_id?.toString() ?: item.code
        return FoodItem(
            id = id,
            name = item.name,
            calories = item.nutrition.calories,
            protein = item.nutrition.protein,
            carbs = item.nutrition.carbohydrates,
            fat = item.nutrition.fat,
            unit = item.weight_unit,
            gramsPerUnit = item.weight,
            image = item.image_url ?: ""
        )
    }
    
    private fun mapFullItem(item: com.example.forhealth.network.dto.food.FoodSearchItemResponse): FoodItem {
        val id = item.food_id ?: item.boohee_id?.toString() ?: item.code
        return FoodItem(
            id = id,
            name = item.name,
            calories = item.nutrition_per_serving.calories,
            protein = item.nutrition_per_serving.protein,
            carbs = item.nutrition_per_serving.carbohydrates,
            fat = item.nutrition_per_serving.fat,
            unit = item.weight_unit,
            gramsPerUnit = item.weight,
            image = item.image_url ?: ""
        )
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
        updateCartAdapter() // 更新适配器以显示新添加的食物
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
            // 获取originalMealGroup中所有记录的ID
            val mealIds = originalMealGroup?.meals?.mapNotNull { it.id }?.filter { it.isNotBlank() } ?: emptyList()
            
            if (mealIds.isEmpty()) {
                // 如果没有有效的记录ID，直接关闭
                dismiss()
                return@setOnClickListener
            }
            
            // 通过ViewModel删除所有记录（会调用API）
            lifecycleScope.launch {
                mainViewModel.deleteMealRecords(mealIds) { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            originalMealGroup?.id?.let { mealGroupId ->
                                onMealDeletedListener?.invoke(mealGroupId)
                            }
                            dismiss()
                        }
                        is ApiResult.Error -> {
                            android.widget.Toast.makeText(requireContext(), result.message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        is ApiResult.Loading -> {}
                    }
                }
            }
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
            // 展开购物车时，更新适配器以确保显示最新的数据
            updateCartAdapter()
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
            // 获取originalMealGroup中所有记录的ID
            val mealIds = originalMealGroup?.meals?.mapNotNull { it.id }?.filter { it.isNotBlank() } ?: emptyList()
            
            if (mealIds.isEmpty()) {
                // 如果没有有效的记录ID，直接关闭（可能是新创建的记录，还没有保存到后端）
                dismiss()
                return
            }
            
            // 通过ViewModel删除所有记录
            lifecycleScope.launch {
                mainViewModel.deleteMealRecords(mealIds) { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            originalMealGroup?.id?.let { mealGroupId ->
                                onMealDeletedListener?.invoke(mealGroupId)
                            }
                            dismiss()
                        }
                        is ApiResult.Error -> {
                            android.widget.Toast.makeText(requireContext(), result.message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        is ApiResult.Loading -> {}
                    }
                }
            }
            return
        }
        
        // 创建新的MealGroup（包含所有selectedItems）
        val currentTime = originalMealGroup?.time ?: DateUtils.getCurrentDateTimeIso()
        val mealGroup = MealGroup(
            id = originalMealGroup?.id ?: "", // 使用原始ID
            meals = selectedItems.map { item ->
                val macros = calculateItemMacros(item)
                MealItem(
                    id = "", // 将在ViewModel中设置
                    name = item.foodItem.name,
                    calories = macros.calories,
                    protein = macros.protein,
                    carbs = macros.carbs,
                    fat = macros.fat,
                    time = currentTime,
                    type = selectedType,
                    image = item.foodItem.image,
                    foodId = item.foodItem.id,
                    servingAmount = if (item.mode == QuantityMode.GRAM) {
                        item.count / item.foodItem.gramsPerUnit
                    } else {
                        item.count
                    }
                )
            },
            time = currentTime,
            type = selectedType
        )
        
        // 通过ViewModel更新
        setSaveButtonsEnabled(false)
        lifecycleScope.launch {
            mainViewModel.updateMealGroupRecords(
                originalMealGroup = originalMealGroup,
                newMealGroup = mealGroup,
                selectedItems = selectedItems.toList()
            ) { result ->
                when (result) {
                    is ApiResult.Success -> {
                        onMealUpdatedListener?.invoke(result.data)
                        dismiss()
                    }
                    is ApiResult.Error -> {
                        android.widget.Toast.makeText(requireContext(), result.message, android.widget.Toast.LENGTH_SHORT).show()
                        setSaveButtonsEnabled(true)
                    }
                    is ApiResult.Loading -> {}
                }
            }
        }
    }
    
    private fun setSaveButtonsEnabled(enabled: Boolean) {
        binding.btnSave.isEnabled = enabled
        binding.btnAddNowCollapsed.isEnabled = enabled
        binding.btnSave.text = if (enabled) getString(R.string.add_meal) else "Saving..."
        binding.btnAddNowCollapsed.text = if (enabled) getString(R.string.add_meal) else "Saving..."
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

