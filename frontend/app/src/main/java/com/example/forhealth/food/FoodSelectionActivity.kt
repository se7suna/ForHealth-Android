package com.example.forhealth.food

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.example.forhealth.model.CreateFoodRecordRequest
import com.example.forhealth.model.FoodCreateRequest
import com.example.forhealth.model.FoodResponse
import com.example.forhealth.model.NutritionData
import com.example.forhealth.model.SimplifiedFoodSearchItem
import com.example.forhealth.model.SimplifiedNutritionData
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.utils.PrefsHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent


class FoodSelectionActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var rvFoods: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnComplete: Button
    private lateinit var tabCommonFoodsLabel: TextView
    private lateinit var tabCustomContainer: View
    private lateinit var tabCustomLabel: TextView
    private lateinit var btnAddCustomFood: ImageView

    private lateinit var foodAdapter: FoodSelectionAdapter
    private var mealType: String = "早餐"
    private val selectedFoods = mutableMapOf<String, SelectedFoodItem>()
    private var searchJob: Job? = null
    private var currentTab: FoodTab = FoodTab.COMMON
    private var commonFoods: List<SimplifiedFoodSearchItem> = emptyList()
    private val customFoods = mutableListOf<SimplifiedFoodSearchItem>()
    private val gson = Gson()
    private val customFoodsPrefs by lazy { getSharedPreferences("food_selection_custom", Context.MODE_PRIVATE) }
    private val customFoodsPrefsKey = "custom_foods"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            android.util.Log.d("FoodSelection", "onCreate: 开始")
            setContentView(R.layout.activity_food_selection)
            android.util.Log.d("FoodSelection", "onCreate: 布局加载完成")

            mealType = intent.getStringExtra("meal_type") ?: "早餐"
            android.util.Log.d("FoodSelection", "onCreate: 餐次类型 = $mealType")

            initViews()
            android.util.Log.d("FoodSelection", "onCreate: 视图初始化完成")

            setupRecyclerView()
            android.util.Log.d("FoodSelection", "onCreate: RecyclerView设置完成")

            setupSearch()
            android.util.Log.d("FoodSelection", "onCreate: 搜索设置完成")

            setupButtons()
            android.util.Log.d("FoodSelection", "onCreate: 按钮设置完成")

            setupTabs()
            android.util.Log.d("FoodSelection", "onCreate: 分类标签设置完成")

            loadCustomFoodsFromCache()
            android.util.Log.d("FoodSelection", "onCreate: 自定义食物加载完成")

            loadCommonFoods()
            android.util.Log.d("FoodSelection", "onCreate: 开始加载食物")
        } catch (e: Exception) {
            android.util.Log.e("FoodSelection", "onCreate: 发生错误", e)
            e.printStackTrace()
            Toast.makeText(this, "页面加载失败: ${e.message}\n${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initViews() {
        try {
            tvTitle = findViewById(R.id.tvTitle)
            etSearch = findViewById(R.id.etSearch)
            rvFoods = findViewById(R.id.rvFoods)
            progressBar = findViewById(R.id.progressBar)
            tvEmpty = findViewById(R.id.tvEmpty)
            btnComplete = findViewById(R.id.btnComplete)
            tabCommonFoodsLabel = findViewById(R.id.tabCommonFoods)
            tabCustomContainer = findViewById(R.id.tabCustomContainer)
            tabCustomLabel = findViewById(R.id.tabCustomFoods)
            btnAddCustomFood = findViewById(R.id.btnAddCustomFood)

            // 设置标题
            val sdf = SimpleDateFormat("M月d日", Locale.CHINA)
            tvTitle.text = "${sdf.format(Date())}$mealType"

            // 设置返回按钮
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = mealType
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("初始化视图失败: ${e.message}", e)
        }
    }

    private fun setupRecyclerView() {
        foodAdapter = FoodSelectionAdapter(
            onAddClick = { food -> showServingDialog(food) },
            selectedFoods = selectedFoods
        )
        rvFoods.apply {
            layoutManager = LinearLayoutManager(this@FoodSelectionActivity)
            adapter = foodAdapter
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (currentTab != FoodTab.COMMON) {
                    return
                }
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(500) // 防抖
                    searchFoods(s?.toString())
                }
            }
        })
    }

    private fun setupButtons() {
        btnComplete.setOnClickListener {
            createFoodRecords()
        }

        findViewById<Button>(R.id.btnPhotos).setOnClickListener {
            Toast.makeText(this, "拍照记录功能待实现", Toast.LENGTH_SHORT).show()
        }


        findViewById<Button>(R.id.btnScanBarcode).setOnClickListener {
            Toast.makeText(this, "扫条形码功能待实现", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        tabCommonFoodsLabel.setOnClickListener { selectTab(FoodTab.COMMON) }
        tabCustomContainer.setOnClickListener { selectTab(FoodTab.CUSTOM) }
        btnAddCustomFood.setOnClickListener {
            selectTab(FoodTab.CUSTOM)
            showCustomFoodDialog()
        }

        findViewById<TextView>(R.id.tabRecipeFoods)?.setOnClickListener {
            val intent = Intent(this, DietActivity::class.java)
            startActivity(intent)//食谱功能
        }


        updateTabStyles()
    }

    private fun loadCommonFoods() {
        // 加载常见食物（空关键词搜索）
        selectTab(FoodTab.COMMON, forceRefresh = true)
    }

    private fun selectTab(tab: FoodTab, forceRefresh: Boolean = false) {
        val tabChanged = currentTab != tab
        currentTab = tab
        updateTabStyles()

        when (tab) {
            FoodTab.COMMON -> {
                etSearch.isEnabled = true
                etSearch.alpha = 1f
                if (forceRefresh || tabChanged || commonFoods.isEmpty()) {
                    searchFoods(etSearch.text?.toString())
                } else {
                    foodAdapter.submitList(commonFoods)
                    showEmpty(commonFoods.isEmpty())
                }
            }
            FoodTab.CUSTOM -> {
                etSearch.isEnabled = false
                etSearch.alpha = 0.5f
                etSearch.clearFocus()
                searchJob?.cancel()
                showCustomFoods()
            }
        }
    }

    private fun showCustomFoods() {
        if (customFoods.isEmpty()) {
            foodAdapter.submitList(emptyList())
            showEmpty(true)
            return
        }
        foodAdapter.submitList(customFoods.toList())
        showEmpty(false)
    }

    private fun addCustomFood(item: SimplifiedFoodSearchItem) {
        val foodId = item.foodId ?: return
        customFoods.removeAll { it.foodId == foodId }
        customFoods.add(0, item)
        saveCustomFoodsToCache()
        if (currentTab == FoodTab.CUSTOM) {
            foodAdapter.submitList(customFoods.toList())
            showEmpty(false)
        }
    }

    private fun loadCustomFoodsFromCache() {
        try {
            val json = customFoodsPrefs.getString(customFoodsPrefsKey, null) ?: return
            val type = object : TypeToken<List<CustomFoodCache>>() {}.type
            val cached: List<CustomFoodCache> = gson.fromJson(json, type) ?: return
            customFoods.clear()
            customFoods.addAll(cached.map { it.toItem() })
        } catch (_: Exception) {
            customFoods.clear()
        }
    }

    private fun saveCustomFoodsToCache() {
        val cacheList = customFoods.mapNotNull { CustomFoodCache.from(it) }
        customFoodsPrefs.edit()
            .putString(customFoodsPrefsKey, gson.toJson(cacheList))
            .apply()
    }

    private fun showCustomFoodDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_custom_food, null)
        val nameInput = view.findViewById<EditText>(R.id.etCustomFoodName)
        val categoryInput = view.findViewById<EditText>(R.id.etCustomFoodCategory)
        val brandInput = view.findViewById<EditText>(R.id.etCustomFoodBrand)
        val servingSizeInput = view.findViewById<EditText>(R.id.etCustomFoodServingSize)
        val servingUnitInput = view.findViewById<EditText>(R.id.etCustomFoodServingUnit)
        val caloriesInput = view.findViewById<EditText>(R.id.etCustomFoodCalories)
        val proteinInput = view.findViewById<EditText>(R.id.etCustomFoodProtein)
        val carbsInput = view.findViewById<EditText>(R.id.etCustomFoodCarbs)
        val fatInput = view.findViewById<EditText>(R.id.etCustomFoodFat)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("添加自定义食物")
            .setView(view)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    nameInput.error = "请输入食物名称"
                    return@setOnClickListener
                }

                val servingSize = servingSizeInput.text.toString().toDoubleOrNull()
                if (servingSize == null || servingSize <= 0) {
                    servingSizeInput.error = "请输入正确的份量"
                    return@setOnClickListener
                }

                val calories = caloriesInput.text.toString().toDoubleOrNull()
                if (calories == null || calories <= 0) {
                    caloriesInput.error = "请输入热量"
                    return@setOnClickListener
                }

                val protein = proteinInput.text.toString().toDoubleOrNull() ?: 0.0
                val carbs = carbsInput.text.toString().toDoubleOrNull() ?: 0.0
                val fat = fatInput.text.toString().toDoubleOrNull() ?: 0.0
                val unit = servingUnitInput.text.toString().ifBlank { "克" }
                val category = categoryInput.text.toString().ifBlank { null }
                val brand = brandInput.text.toString().ifBlank { null }

                val form = CustomFoodForm(
                    name = name,
                    category = category,
                    brand = brand,
                    servingSize = servingSize,
                    servingUnit = unit,
                    calories = calories,
                    protein = protein,
                    carbohydrates = carbs,
                    fat = fat
                )

                dialog.dismiss()
                createCustomFood(form)
            }
        }

        dialog.show()
    }

    private fun createCustomFood(form: CustomFoodForm) {
        val token = PrefsHelper.getToken(this)
        if (token.isBlank()) {
            redirectToLogin()
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.createFood(
                    token = "Bearer $token",
                    request = form.toRequest()
                )

                if (response.isSuccessful && response.body() != null) {
                    val newItem = foodResponseToSimplified(response.body()!!)
                    addCustomFood(newItem)
                    Toast.makeText(
                        this@FoodSelectionActivity,
                        "自定义食物已添加",
                        Toast.LENGTH_SHORT
                    ).show()
                    selectTab(FoodTab.CUSTOM)
                } else {
                    Toast.makeText(
                        this@FoodSelectionActivity,
                        "创建失败：${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@FoodSelectionActivity,
                    "创建失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun foodResponseToSimplified(response: FoodResponse): SimplifiedFoodSearchItem {
        val nutrition = response.nutritionPerServing
        return SimplifiedFoodSearchItem(
            source = response.source ?: "local",
            foodId = response.id,
            booheeId = response.booheeId,
            code = response.id,
            name = response.name,
            weight = response.servingSize,
            weightUnit = response.servingUnit,
            brand = response.brand,
            imageUrl = response.imageUrl,
            nutrition = SimplifiedNutritionData(
                calories = nutrition.calories,
                protein = nutrition.protein,
                fat = nutrition.fat,
                carbohydrates = nutrition.carbohydrates,
                sugar = nutrition.sugar,
                sodium = nutrition.sodium
            )
        )
    }

    private fun updateTabStyles() {
        val selectedColor = ContextCompat.getColor(this, R.color.primary_green)
        val defaultColor = Color.parseColor("#666666")

        val commonBackground = if (currentTab == FoodTab.COMMON) {
            ContextCompat.getDrawable(this, R.drawable.bg_category_tab_selected)
        } else {
            ContextCompat.getDrawable(this, R.drawable.bg_category_tab_unselected)
        }
        tabCommonFoodsLabel.background = commonBackground
        tabCommonFoodsLabel.setTextColor(if (currentTab == FoodTab.COMMON) selectedColor else defaultColor)

        val customBackground = if (currentTab == FoodTab.CUSTOM) {
            ContextCompat.getDrawable(this, R.drawable.bg_category_tab_selected)
        } else {
            ContextCompat.getDrawable(this, R.drawable.bg_category_tab_unselected)
        }
        tabCustomContainer.background = customBackground
        tabCustomLabel.setTextColor(if (currentTab == FoodTab.CUSTOM) selectedColor else defaultColor)
        btnAddCustomFood.setColorFilter(if (currentTab == FoodTab.CUSTOM) selectedColor else defaultColor)
    }

    private fun searchFoods(keyword: String?) {
        if (currentTab != FoodTab.COMMON) {
            return
        }

        val token = PrefsHelper.getToken(this)
        if (token.isBlank()) {
            redirectToLogin()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.searchFoods(
                    token = "Bearer $token",
                    keyword = keyword?.ifBlank { null },
                    page = 1,
                    simplified = true
                )

                if (response.isSuccessful && response.body() != null) {
                    val foodList = response.body()!!.foods
                    commonFoods = foodList
                    if (foodList.isEmpty()) {
                        showEmpty(true)
                    } else if (currentTab == FoodTab.COMMON) {
                        showEmpty(false)
                        foodAdapter.submitList(commonFoods)
                    }
                } else {
                    commonFoods = emptyList()
                    showEmpty(true)
                    Toast.makeText(
                        this@FoodSelectionActivity,
                        "搜索失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                commonFoods = emptyList()
                showEmpty(true)
                Toast.makeText(
                    this@FoodSelectionActivity,
                    "搜索失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private val unitNames = arrayOf("克", "份", "碗", "盘")
    private val unitGrams = arrayOf(1.0, 100.0, 200.0, 400.0)

    private fun showServingDialog(food: SimplifiedFoodSearchItem) {
        val dialog = android.app.AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_serving_amount, null)

        val tvFoodName = view.findViewById<TextView>(R.id.tvDialogFoodName)
        val etServingAmount = view.findViewById<EditText>(R.id.etServingAmount)
        val tvServingInfo = view.findViewById<TextView>(R.id.tvServingInfo)
        val spServingUnit = view.findViewById<Spinner>(R.id.spServingUnit)

        tvFoodName.text = food.name
        etServingAmount.setText("1.0")

        // 单位选择
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            unitNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spServingUnit.adapter = adapter
        spServingUnit.setSelection(1) // 默认“份”

        // 显示参考份：接口提供的标准单位
        tvServingInfo.text = "参考: ${food.weight.toInt()}${food.weightUnit} = ${food.nutrition.calories.toInt()} 千卡"

        // 每克卡路里
        val calPerGram = food.nutrition.calories / food.weight

        dialog.setView(view)
            .setTitle("设置份量")
            .setPositiveButton("确定") { _, _ ->
                val amount = etServingAmount.text.toString().toDoubleOrNull() ?: 1.0
                val unitIndex = spServingUnit.selectedItemPosition

                val grams = unitGrams[unitIndex] * amount // 总克数
                val calories = calPerGram * grams        // 计算总热量

                // 创建更新后的 food 数据
                val updatedFood = food.copy(
                    weight = grams,
                    weightUnit = unitNames[unitIndex],
                    nutrition = food.nutrition.copy(calories = calories)
                )

                addFoodToSelection(updatedFood, amount)
            }
            .setNegativeButton("取消", null)
            .show()
    }


    private fun addFoodToSelection(food: SimplifiedFoodSearchItem, servingAmount: Double) {
        val foodId = food.foodId ?: return

        selectedFoods[foodId] = SelectedFoodItem(
            food = food,
            servingAmount = servingAmount
        )

        foodAdapter.notifyDataSetChanged()
        updateCompleteButton()

        Toast.makeText(
            this,
            "已添加 ${food.name} ${servingAmount}份",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateCompleteButton() {
        btnComplete.text = if (selectedFoods.isEmpty()) {
            "完成"
        } else {
            "完成 (${selectedFoods.size})"
        }
    }

    private fun createFoodRecords() {
        if (selectedFoods.isEmpty()) {
            finish()
            return
        }

        val token = PrefsHelper.getToken(this)
        if (token.isBlank()) {
            redirectToLogin()
            return
        }

        showLoading(true)
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val recordedAt = sdf.format(Date())

        lifecycleScope.launch {
            var successCount = 0
            var failCount = 0

            for ((foodId, selectedItem) in selectedFoods) {
                try {
                    val request = CreateFoodRecordRequest(
                        foodId = foodId,
                        source = "auto",
                        servingAmount = selectedItem.servingAmount,
                        recordedAt = recordedAt,
                        mealType = mealType,
                        notes = null
                    )

                    val response = RetrofitClient.api.createFoodRecord("Bearer $token", request)
                    if (response.isSuccessful) {
                        successCount++
                    } else {
                        failCount++
                    }
                } catch (e: Exception) {
                    failCount++
                }
            }

            showLoading(false)

            if (successCount > 0) {
                Toast.makeText(
                    this@FoodSelectionActivity,
                    "成功添加${successCount}条记录${if (failCount > 0) "，失败${failCount}条" else ""}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                Toast.makeText(
                    this@FoodSelectionActivity,
                    "添加记录失败",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmpty(show: Boolean) {
        tvEmpty.text = if (currentTab == FoodTab.CUSTOM) "暂无自定义食物" else "暂无食物"
        tvEmpty.visibility = if (show) View.VISIBLE else View.GONE
        rvFoods.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun redirectToLogin() {
        Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
        val intent = android.content.Intent(this, com.example.forhealth.auth.LoginActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    data class SelectedFoodItem(
        val food: SimplifiedFoodSearchItem,
        val servingAmount: Double
    )

    private data class CustomFoodForm(
        val name: String,
        val category: String?,
        val brand: String?,
        val servingSize: Double,
        val servingUnit: String,
        val calories: Double,
        val protein: Double,
        val carbohydrates: Double,
        val fat: Double
    ) {
        fun toRequest(): FoodCreateRequest {
            return FoodCreateRequest(
                name = name,
                category = category,
                servingSize = servingSize,
                servingUnit = servingUnit,
                nutritionPerServing = NutritionData(
                    calories = calories,
                    protein = protein,
                    carbohydrates = carbohydrates,
                    fat = fat
                ),
                brand = brand
            )
        }
    }

    private data class CustomFoodCache(
        val foodId: String,
        val name: String,
        val weight: Double,
        val weightUnit: String,
        val brand: String?,
        val imageUrl: String?,
        val calories: Double,
        val protein: Double,
        val fat: Double,
        val carbohydrates: Double,
        val sugar: Double? = null,
        val sodium: Double? = null
    ) {
        fun toItem(): SimplifiedFoodSearchItem {
            return SimplifiedFoodSearchItem(
                source = "local",
                foodId = foodId,
                booheeId = null,
                code = foodId,
                name = name,
                weight = weight,
                weightUnit = weightUnit,
                brand = brand,
                imageUrl = imageUrl,
                nutrition = SimplifiedNutritionData(
                    calories = calories,
                    protein = protein,
                    fat = fat,
                    carbohydrates = carbohydrates,
                    sugar = sugar,
                    sodium = sodium
                )
            )
        }

        companion object {
            fun from(item: SimplifiedFoodSearchItem): CustomFoodCache? {
                val id = item.foodId ?: return null
                return CustomFoodCache(
                    foodId = id,
                    name = item.name,
                    weight = item.weight,
                    weightUnit = item.weightUnit,
                    brand = item.brand,
                    imageUrl = item.imageUrl,
                    calories = item.nutrition.calories,
                    protein = item.nutrition.protein,
                    fat = item.nutrition.fat,
                    carbohydrates = item.nutrition.carbohydrates,
                    sugar = item.nutrition.sugar,
                    sodium = item.nutrition.sodium
                )
            }
        }
    }

    private enum class FoodTab {
        COMMON, CUSTOM
    }
}

