package com.example.forhealth.user
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.forhealth.database.CustomFoodDatabaseHelper
import com.example.forhealth.model.FoodItem
import kotlinx.coroutines.launch
import com.example.forhealth.R
import android.util.Log
class SelectFoodRecipeActivity : AppCompatActivity() {

    private lateinit var spinnerFoodRecipe: Spinner
    private lateinit var etPortionSize: EditText
    private lateinit var tvCalories: TextView
    private lateinit var tvNutrients: TextView
    private lateinit var btnRecord: Button
    private lateinit var foodRecipeList: List<String> // 食物名称列表
    private lateinit var selectedFoodRecipe: String
    private lateinit var dbHelper: CustomFoodDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_food_recipe)

        // 初始化视图
        spinnerFoodRecipe = findViewById(R.id.spinnerFoodRecipe)
        etPortionSize = findViewById(R.id.etPortionSize)
        tvCalories = findViewById(R.id.tvCalories)
        tvNutrients = findViewById(R.id.tvNutrients)
        btnRecord = findViewById(R.id.btnRecord)

        // 初始化数据库
        dbHelper = CustomFoodDatabaseHelper(this)

        // 加载食物名称列表（这里我们从数据库加载所有食物名称）
        lifecycleScope.launch {
            foodRecipeList = getFoodNamesFromDatabase()
            val adapter = ArrayAdapter(this@SelectFoodRecipeActivity, android.R.layout.simple_spinner_item, foodRecipeList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerFoodRecipe.adapter = adapter
        }

        // 选择食物或食谱
        spinnerFoodRecipe.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedFoodRecipe = foodRecipeList[position]
                updateNutritionalInfo(selectedFoodRecipe)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }

        // 点击记录按钮
        btnRecord.setOnClickListener {
            val portionSize = etPortionSize.text.toString()
            if (portionSize.isNotEmpty()) {
                val totalCalories = calculateCalories(selectedFoodRecipe, portionSize)
                val nutrients = getNutritionalInfo(selectedFoodRecipe, portionSize)

                // 显示计算结果
                tvCalories.text = "热量: $totalCalories kcal"
                tvNutrients.text = "营养成分: $nutrients"

                // 将记录添加到饮食日志（此处需实现数据库或日志保存逻辑）
                addToDietLog(selectedFoodRecipe, portionSize, totalCalories, nutrients)

                // 提示记录成功
                Toast.makeText(this, "记录成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "请输入摄入份数", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 从数据库中获取所有食物名称
    private suspend fun getFoodNamesFromDatabase(): List<String> {
        val foodNames = mutableListOf<String>()
        val db = dbHelper.readableDatabase

        // 确保使用 CustomFoodDatabaseHelper 中的常量
        val query = "SELECT ${CustomFoodDatabaseHelper.COLUMN_FOOD_NAME} FROM ${CustomFoodDatabaseHelper.CUSTOM_FOOD_TABLE_NAME}"
        val cursor = db.rawQuery(query, null)

        // 确保查询成功并且获得有效的数据
        if (cursor.moveToFirst()) {
            do {
                // 使用 CustomFoodDatabaseHelper 中的常量来获取列索引
                val foodNameIndex = cursor.getColumnIndex(CustomFoodDatabaseHelper.COLUMN_FOOD_NAME)
                if (foodNameIndex >= 0) {
                    val foodName = cursor.getString(foodNameIndex)
                    foodNames.add(foodName)
                } else {
                    Log.e("DatabaseError", "Column ${CustomFoodDatabaseHelper.COLUMN_FOOD_NAME} not found.")
                }
            } while (cursor.moveToNext())
        }

        cursor.close()
        return foodNames
    }


    // 更新营养信息
    private fun updateNutritionalInfo(foodRecipe: String) {
        lifecycleScope.launch {
            val foodItem = dbHelper.getFoodRecipeByName(foodRecipe)
            if (foodItem != null) {
                tvCalories.text = "热量: ${foodItem.calories} kcal"
                tvNutrients.text = "蛋白质: ${foodItem.protein} g, 脂肪: ${foodItem.fat} g, 碳水化合物: ${foodItem.carbs} g"
            } else {
                tvCalories.text = "未找到食物数据"
                tvNutrients.text = "未找到营养成分"
            }
        }
    }

    // 根据摄入份数计算总热量
    private fun calculateCalories(foodRecipe: String, portionSize: String): Double {
        val foodItem = dbHelper.getFoodRecipeByName(foodRecipe)
        val caloriesPerUnit = foodItem?.calories ?: 0.0
        return caloriesPerUnit * portionSize.toDouble() // 根据份数计算总热量
    }

    // 获取食物的营养成分
    private fun getNutritionalInfo(foodRecipe: String, portionSize: String): String {
        val foodItem = dbHelper.getFoodRecipeByName(foodRecipe)
        if (foodItem != null) {
            val protein = foodItem.protein * portionSize.toDouble()
            val fat = foodItem.fat * portionSize.toDouble()
            val carbs = foodItem.carbs * portionSize.toDouble()

            return "蛋白质: ${String.format("%.2f", protein)} g, 脂肪: ${String.format("%.2f", fat)} g, 碳水化合物: ${String.format("%.2f", carbs)} g"
        } else {
            return "未找到该食物的营养成分"
        }
    }

    // 将记录添加到饮食日志
    private fun addToDietLog(foodRecipe: String, portionSize: String, calories: Double, nutrients: String) {
        // 示例：打印日志，实际应用中应将数据保存到数据库
        Log.d("DietLog", "食物: $foodRecipe, 份数: $portionSize, 热量: $calories, 营养成分: $nutrients")
    }
}
