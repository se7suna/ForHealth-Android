package com.example.forhealth.user

import android.os.Bundle
import android.text.Editable
import android.content.Intent
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.forhealth.R
import com.example.forhealth.network.FoodDatabaseAPI
import com.example.forhealth.model.FoodItem

class

FoodSearchActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var listView: ListView
    private val foodAdapter = FoodListAdapter(this, mutableListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_search)
        val btnBackToFood = findViewById<Button>(R.id.btnBack)
        btnBackToFood.setOnClickListener {
            // 跳转回 FoodActivity
            val intent = Intent(this, FoodActivity::class.java)  // 返回到 FoodActivity
            startActivity(intent)
            finish()  // 结束当前页面
        }
        searchEditText = findViewById(R.id.searchEditText)
        listView = findViewById(R.id.foodListView)

        listView.adapter = foodAdapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    fetchFoodData(query)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedFood = foodAdapter.getItem(position)
            selectedFood?.let {
                showFoodDetails(it)
            }
        }
    }

    private fun fetchFoodData(query: String) {
        // 调用FoodDatabaseAPI获取数据
        FoodDatabaseAPI.getFoodItems(query, object : FoodDatabaseAPI.FoodDataCallback {
            override fun onSuccess(foodItems: List<FoodItem>) {
                foodAdapter.updateData(foodItems)
            }

            override fun onError(error: String) {
                Toast.makeText(this@FoodSearchActivity, "错误: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showFoodDetails(food: FoodItem) {
        // 显示食物详细信息，可以使用弹窗或展开形式
        val detailFragment = FoodDetailFragment.newInstance(food)
        detailFragment.show(supportFragmentManager, "FoodDetail")
    }
}
