package com.example.forhealth.food

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.example.forhealth.model.SimplifiedFoodSearchItem
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.utils.PrefsHelper
import kotlinx.coroutines.*
import com.google.gson.Gson
import androidx.appcompat.app.AlertDialog
import com.example.forhealth.model.SimplifiedNutritionData

class DietActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var rvFoods: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnSaveRecipe: Button

    private lateinit var foodAdapter: FoodSelectionAdapter
    private val selectedFoods = mutableMapOf<String, Pair<SimplifiedFoodSearchItem, Double>>()
    private var searchJob: Job? = null
    private var requestJob: Job? = null

    private var commonFoods: List<SimplifiedFoodSearchItem> = emptyList()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diet)

        initViews()
        setupRecyclerView()
        setupSearch()
        setupButtons()
        setupBackPressLogic(

        )

        // 🚀 关键：加载假数据
        loadCommonFoods()
    }


    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        etSearch = findViewById(R.id.etSearch)
        rvFoods = findViewById(R.id.rvFoods)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnSaveRecipe = findViewById(R.id.btnComplete)
        tvTitle.text = "创建食谱"
    }


    private fun setupRecyclerView() {
        foodAdapter = FoodSelectionAdapter(
            onAddClick = { food -> showServingDialog(food) },
            selectedFoods = convertSelectedFoodsToAdapterFormat()
        )
        rvFoods.layoutManager = LinearLayoutManager(this)
        rvFoods.adapter = foodAdapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(400)
                    searchFoods(s?.toString())
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupButtons() {
        btnSaveRecipe.setOnClickListener { saveRecipe() }
    }

    private fun setupBackPressLogic() {
        val backBtn = findViewById<Button>(R.id.btnBackToFood)
        backBtn.setOnClickListener {
            finish() // 返回上一个页面
        }
    }


    private fun cancelActiveRequests() {
        searchJob?.cancel()
        requestJob?.cancel()
        println("All network requests canceled due to BACK")
    }

    private fun loadCommonFoods() {
        // 临时假数据（确保符合 SimplifiedFoodSearchItem 字段）
        val testFoods = listOf(
            SimplifiedFoodSearchItem(
                source = "local",
                foodId = "F001",
                booheeId = null,
                code = "LOCAL_APPLE",
                name = "苹果",
                weight = 100.0,
                weightUnit = "克",
                brand = "自然农庄",
                imageUrl = null,
                nutrition = SimplifiedNutritionData(
                    calories = 52.0,
                    protein = 0.3,
                    fat = 0.2,
                    carbohydrates = 14.0,
                    sugar = 10.4,
                    sodium = 1.0
                )
            ),
            SimplifiedFoodSearchItem(
                source = "local",
                foodId = "F002",
                booheeId = null,
                code = "LOCAL_BREAD",
                name = "全麦面包",
                weight = 30.0,
                weightUnit = "克",
                brand = "家家麦",
                imageUrl = null,
                nutrition = SimplifiedNutritionData(
                    calories = 79.0,
                    protein = 4.0,
                    fat = 1.0,
                    carbohydrates = 14.0,
                    sugar = 2.0,
                    sodium = 130.0
                )
            )
        )

        commonFoods = testFoods
        foodAdapter.submitList(commonFoods)
        showEmpty(false)
    }



    private fun searchFoods(keyword: String?) {
        val token = PrefsHelper.getToken(this)

        cancelActiveRequests()
        showLoading(true)

        requestJob = lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.searchFoods(
                    token = "Bearer $token",
                    keyword = keyword?.ifBlank { null },
                    page = 1,
                    simplified = true
                )

                if (!isActive) return@launch
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    commonFoods = response.body()!!.foods
                    if (commonFoods.isEmpty()) showEmpty(true)
                    else {
                        showEmpty(false)
                        foodAdapter.submitList(commonFoods)
                    }
                } else {
                    showEmpty(true)
                }
            } catch (_: CancellationException) {
                // 这是正常情况：因为用户点击了返回或重新搜索
                println("Request canceled normally")
            } catch (e: Exception) {
                showEmpty(true)
            } finally {
                if (isActive) showLoading(false)
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
        selectedFoods[food.foodId ?: return] = Pair(food, servingAmount)
        foodAdapter.notifyDataSetChanged()
        updateSaveButton()
    }

    private fun updateSaveButton() {
        btnSaveRecipe.text = if (selectedFoods.isEmpty()) "保存食谱"
        else "保存食谱 (${selectedFoods.size})"
    }

    private fun saveRecipe() {
        if (selectedFoods.isEmpty()) {
            Toast.makeText(this, "请先选择食物", Toast.LENGTH_SHORT).show()
            return
        }

        showInputRecipeNameDialog()
    }

    private fun showInputRecipeNameDialog() {
        val editText = EditText(this)
        editText.hint = "请输入食谱名称"
        editText.setText("我的食谱")  // 默认值，可修改

        AlertDialog.Builder(this)
            .setTitle("保存食谱")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "食谱名称不能为空", Toast.LENGTH_SHORT).show()
                } else {
                    saveRecipeToDatabase(name)
                    Toast.makeText(this, "已保存：$name", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveRecipeToDatabase(name: String) {
        val recipeList = selectedFoods.map {
            mapOf(
                "foodId" to it.key,
                "foodName" to it.value.first.name,
                "servingAmount" to it.value.second,
                "calories" to it.value.first.nutrition.calories * it.value.second
            )
        }

        val json = gson.toJson(recipeList)
        getSharedPreferences("recipes", Context.MODE_PRIVATE)
            .edit()
            .putString(name, json)
            .apply()
    }


    private fun convertSelectedFoodsToAdapterFormat() =
        selectedFoods.mapValues {
            FoodSelectionActivity.SelectedFoodItem(it.value.first, it.value.second)
        }.toMutableMap()

    private fun showLoading(show: Boolean) {
        if (!isFinishing) {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun showEmpty(show: Boolean) {
        if (!isFinishing) {
            tvEmpty.visibility = if (show) View.VISIBLE else View.GONE
            rvFoods.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    private fun redirectToLogin() {
        Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
        val intent = android.content.Intent(
            this,
            com.example.forhealth.auth.LoginActivity::class.java
        )
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        cancelActiveRequests()
        super.onDestroy()
    }
}
