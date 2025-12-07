package com.example.forhealth.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.forhealth.R
import com.example.forhealth.databinding.FragmentAddMealBinding
import com.example.forhealth.models.*
import com.example.forhealth.ui.activities.CameraActivity
import com.example.forhealth.ui.adapters.CartFoodAdapter
import com.example.forhealth.ui.adapters.FoodListAdapter
import com.example.forhealth.utils.Constants
import com.example.forhealth.utils.DateUtils
import com.google.android.material.button.MaterialButton
import java.util.*

class AddMealFragment : DialogFragment() {
    
    private var _binding: FragmentAddMealBinding? = null
    private val binding get() = _binding!!
    
    private var onMealAddedListener: ((List<MealItem>) -> Unit)? = null
    
    private val selectedItems = mutableListOf<SelectedFoodItem>()
    private var selectedType: MealType = MealType.BREAKFAST
    private val customFoods = mutableListOf<FoodItem>() // 保存的自定义食物
    private var filteredFood: List<FoodItem> = Constants.FOOD_DB
    private var isCartExpanded = false
    
    private lateinit var foodAdapter: FoodListAdapter
    private lateinit var cartAdapter: CartFoodAdapter
    
    fun setOnMealAddedListener(listener: (List<MealItem>) -> Unit) {
        onMealAddedListener = listener
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_NoTitleBar_Fullscreen)
        
        // 根据当前时间设置默认餐食类型
        selectedType = DateUtils.getMealTypeByHour()
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
        
        // 清空购物车，确保每次打开都是全新的状态
        selectedItems.clear()
        isCartExpanded = false
        
        setupCloseButton()
        setupSearchBar()
        setupMealTypeTabs()
        setupCreateCustomFoodButton()
        setupCameraButton()
        setupFoodList()
        setupCart()
        
        // 初始化食物列表（包含自定义食物）
        updateAllFoodList()
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
                    // 将自定义食物添加到列表（这样以后搜索时也能找到）
                    if (!customFoods.any { it.id == foodItem.id }) {
                        customFoods.add(foodItem)
                    }
                    // 更新食物列表
                    updateAllFoodList()
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
    
    private val cameraResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val mode = data?.getStringExtra("mode")
            
            when (mode) {
                "scan" -> {
                    val barcode = data?.getStringExtra("barcode")
                    if (barcode != null) {
                        // 处理扫码结果，可以搜索对应的食物
                        binding.etSearch.setText(barcode)
                        filterFood(barcode)
                    }
                }
                "photo" -> {
                    val photoUri = data?.getStringExtra("photo_uri")
                    if (photoUri != null) {
                        // 处理拍照结果，可以用于AI识别食物
                        // TODO: 实现AI识别食物的功能
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Photo captured: $photoUri",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun setupCameraButton() {
        binding.btnCamera.setOnClickListener {
            val intent = Intent(requireContext(), CameraActivity::class.java)
            cameraResultLauncher.launch(intent)
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
    
    private fun updateAllFoodList() {
        // 合并常量食物列表和自定义食物列表
        val allFoods = Constants.FOOD_DB + customFoods
        val query = binding.etSearch.text.toString()
        filterFood(query, allFoods)
    }
    
    private fun filterFood(query: String, allFoods: List<FoodItem> = Constants.FOOD_DB + customFoods) {
        filteredFood = if (query.isBlank()) {
            allFoods
        } else {
            allFoods.filter {
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
            // 如果已存在，增加数量
            val increment = if (existing.mode == QuantityMode.GRAM) 10.0 else 1.0
            existing.count += increment
        } else {
            // 如果不存在，添加到购物车
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
        // 购物车头部点击展开/收起
        binding.layoutCartHeader.setOnClickListener {
            toggleCartExpansion()
        }
        
        // 收起状态下的"Add Meal"按钮
        binding.btnAddNowCollapsed.setOnClickListener {
            saveMeals()
        }
        
        // 展开状态下的"Confirm & Add"按钮
        binding.btnSave.setOnClickListener {
            saveMeals()
        }
        
        // 初始化购物车 RecyclerView
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
        
        // 转换数量
        val newCount = if (newMode == QuantityMode.GRAM) {
            // Unit -> Gram: 乘以 gramsPerUnit
            (currentCount * item.foodItem.gramsPerUnit).toInt().toDouble()
        } else {
            // Gram -> Unit: 除以 gramsPerUnit，保留一位小数
            ((currentCount / item.foodItem.gramsPerUnit) * 10).toInt() / 10.0
        }
        
        // 安全检查
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
        if (value.isBlank()) {
            return // 允许空字符串，在 blur 时处理
        }
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
            binding.layoutCart.visibility = View.GONE
        } else {
            binding.layoutCart.visibility = View.VISIBLE
            
            val totalCalories = selectedItems.sumOf { calculateItemMacros(it).calories }
            binding.tvCartTotalCalories.text = "${totalCalories.toInt()} kcal"
            
            // 更新徽章
            if (selectedItems.size > 0) {
                binding.tvCartBadge.text = selectedItems.size.toString()
                binding.tvCartBadge.visibility = View.VISIBLE
            } else {
                binding.tvCartBadge.visibility = View.GONE
            }
            
            // 更新确认按钮文本
            binding.btnSave.text = getString(R.string.add_meal)
            
            // 确保"Add Meal"按钮在收起状态下可见
            if (!isCartExpanded) {
                binding.btnAddNowCollapsed.visibility = View.VISIBLE
            }
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
        if (selectedItems.isEmpty()) return
        
        // 所有meal使用相同的时间戳，确保同一批次添加的meal可以正确分组
        val currentTime = DateUtils.getCurrentTime()
        
        val meals = selectedItems.map { item ->
            val macros = calculateItemMacros(item)
            MealItem(
                id = "${System.currentTimeMillis()}-${UUID.randomUUID()}",
                name = item.foodItem.name,
                calories = macros.calories,
                protein = macros.protein,
                carbs = macros.carbs,
                fat = macros.fat,
                time = currentTime,
                type = selectedType,
                image = item.foodItem.image
            )
        }
        
        onMealAddedListener?.invoke(meals)
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
