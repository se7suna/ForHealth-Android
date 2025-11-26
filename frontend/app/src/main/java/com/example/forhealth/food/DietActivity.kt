package com.example.forhealth.food

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.example.forhealth.model.SimplifiedFoodSearchItem
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.utils.PrefsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.gson.Gson

class DietActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var rvFoods: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnSaveRecipe: Button

    private lateinit var foodAdapter: FoodSelectionAdapter
    private val selectedFoods = mutableMapOf<String, Pair<SimplifiedFoodSearchItem, Double>>() // foodId -> (food, servingAmount)
    private var searchJob: Job? = null
    private var commonFoods: List<SimplifiedFoodSearchItem> = emptyList()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_selection) // 使用之前的布局文件

        initViews()
        setupRecyclerView()
        setupSearch()
        setupButtons()

        loadCommonFoods()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        etSearch = findViewById(R.id.etSearch)
        rvFoods = findViewById(R.id.rvFoods)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnSaveRecipe = findViewById(R.id.btnComplete)  // 使用 "完成" 按钮保存食谱

        // 设置标题
        tvTitle.text = "创建食谱"
    }

    private fun setupRecyclerView() {
        foodAdapter = FoodSelectionAdapter(
            onAddClick = { food -> showServingDialog(food) },
            selectedFoods = convertSelectedFoodsToAdapterFormat()  // 转换数据结构
        )
        rvFoods.apply {
            layoutManager = LinearLayoutManager(this@DietActivity)
            adapter = foodAdapter
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(500) // 防抖
                    searchFoods(s?.toString())
                }
            }
        })
    }

    private fun setupButtons() {
        btnSaveRecipe.setOnClickListener {
            saveRecipe()
        }
    }

    private fun loadCommonFoods() {
        searchFoods("")
    }

    private fun searchFoods(keyword: String?) {
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
                    commonFoods = response.body()!!.foods
                    if (commonFoods.isEmpty()) {
                        showEmpty(true)
                    } else {
                        showEmpty(false)
                        foodAdapter.submitList(commonFoods)
                    }
                } else {
                    commonFoods = emptyList()
                    showEmpty(true)
                }
            } catch (e: Exception) {
                commonFoods = emptyList()
                showEmpty(true)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showServingDialog(food: SimplifiedFoodSearchItem) {
        val dialog = android.app.AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_serving_amount, null)

        val tvFoodName = view.findViewById<TextView>(R.id.tvDialogFoodName)
        val etServingAmount = view.findViewById<EditText>(R.id.etServingAmount)
        val tvServingInfo = view.findViewById<TextView>(R.id.tvServingInfo)

        tvFoodName.text = food.name
        tvServingInfo.text = "每份 ${food.weight.toInt()}${food.weightUnit} = ${food.nutrition.calories.toInt()}千卡"
        etServingAmount.setText("1.0")

        dialog.setView(view)
            .setTitle("设置份量")
            .setPositiveButton("确定") { _, _ ->
                val amount = etServingAmount.text.toString().toDoubleOrNull() ?: 1.0
                addFoodToSelection(food, amount)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addFoodToSelection(food: SimplifiedFoodSearchItem, servingAmount: Double) {
        val foodId = food.foodId ?: return

        // 将食物和份量添加到 selectedFoods 中
        selectedFoods[foodId] = Pair(food, servingAmount)

        foodAdapter.notifyDataSetChanged()
        updateSaveButton()
    }

    private fun updateSaveButton() {
        btnSaveRecipe.text = if (selectedFoods.isEmpty()) {
            "保存食谱"
        } else {
            "保存食谱 (${selectedFoods.size})"
        }
    }

    private fun saveRecipe() {
        if (selectedFoods.isEmpty()) {
            Toast.makeText(this, "请先选择食物", Toast.LENGTH_SHORT).show()
            return
        }

        // 生成食谱的ID，例如食谱1、2、3等
        val recipeId = "食谱${System.currentTimeMillis() / 1000}"

        // 保存食谱
        saveRecipeToDatabase(recipeId)

        Toast.makeText(this, "食谱已保存为：$recipeId", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun saveRecipeToDatabase(recipeId: String) {
        // 构建食谱，保存选择的食物及其份量
        val recipeList = selectedFoods.map { entry ->
            mapOf(
                "foodId" to entry.key,
                "foodName" to entry.value.first.name,
                "servingAmount" to entry.value.second
            )
        }

        // 将食谱保存为 JSON 格式
        val recipeJson = gson.toJson(recipeList)

        // 保存到 SharedPreferences 或数据库
        val prefs = getSharedPreferences("recipes", Context.MODE_PRIVATE)
        prefs.edit().putString(recipeId, recipeJson).apply()
    }

    private fun convertSelectedFoodsToAdapterFormat(): MutableMap<String, FoodSelectionActivity.SelectedFoodItem> {
        // 将 selectedFoods 转换为 FoodSelectionActivity.SelectedFoodItem 格式
        return selectedFoods.mapValues { entry ->
            FoodSelectionActivity.SelectedFoodItem(
                food = entry.value.first,
                servingAmount = entry.value.second
            )
        }.toMutableMap()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmpty(show: Boolean) {
        tvEmpty.text = "暂无食物"
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
}



