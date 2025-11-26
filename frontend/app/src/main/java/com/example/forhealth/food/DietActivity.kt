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

        // 找到按钮
        val btnBackToFood = findViewById<Button>(R.id.btnBackToFood)
        val btnComplete = findViewById<Button>(R.id.btnComplete)

        // 返回按钮点击事件，直接结束当前页面，回到 FoodSelectionActivity
        btnBackToFood.setOnClickListener {
            finish()
        }

        // 完成按钮点击事件，保持原功能，执行保存操作后再返回
        btnComplete.setOnClickListener {
            saveRecipe()  // 你的保存功能函数
            finish()
        }

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
        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    cancelActiveRequests()
                    finish()
                }
            })
    }

    private fun cancelActiveRequests() {
        searchJob?.cancel()
        requestJob?.cancel()
        println("All network requests canceled due to BACK")
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

        val recipeId = "食谱${System.currentTimeMillis() / 1000}"
        saveRecipeToDatabase(recipeId)

        Toast.makeText(this, "食谱已保存为：$recipeId", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun saveRecipeToDatabase(recipeId: String) {
        val recipeList = selectedFoods.map {
            mapOf(
                "foodId" to it.key,
                "foodName" to it.value.first.name,
                "servingAmount" to it.value.second
            )
        }
        val recipeJson = gson.toJson(recipeList)
        getSharedPreferences("recipes", Context.MODE_PRIVATE)
            .edit().putString(recipeId, recipeJson).apply()
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
