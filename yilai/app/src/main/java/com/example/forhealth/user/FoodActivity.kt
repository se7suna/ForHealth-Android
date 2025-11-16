package com.example.forhealth.user

import android.content.Intent
import android.os.Bundle
import com.example.forhealth.R
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class FoodActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food)  // 加载 FoodActivity 的布局

        // 获取按钮
        val btnFoodSearch = findViewById<Button>(R.id.btnFoodSearch)  // 获取食物搜索按钮
        val btnFoodRecord = findViewById<Button>(R.id.btnFoodRecord)  // 获取手动记录食物按钮
        val btnBarcode = findViewById<Button>(R.id.btnFoodBarcode)  // 获取条形码按钮
        val btnCreateCustomFood = findViewById<Button>(R.id.btnCreateCustomFood)  // 获取创建自定义食物按钮
        val btnCreateRecipe = findViewById<Button>(R.id.btnCreateCustomRecipe)  // 获取创建自定义食谱按钮
        val btnEditDelete = findViewById<Button>(R.id.btnEditRecord)  // 获取编辑/删除已记录条目按钮
        val btnRecordCustom = findViewById<Button>(R.id.btnRecordCustom)  // 获取记录自定义食物/谱按钮
        val btnBackToFood = findViewById<Button>(R.id.btnBack)
        btnBackToFood.setOnClickListener {
            // 跳转回 FoodActivity
            val intent = Intent(this, MainActivity::class.java)  // 返回到 FoodActivity
            startActivity(intent)
            finish()  // 结束当前页面
        }
        // 设置跳转到食物搜索页面
        btnFoodSearch.setOnClickListener {
            val intent = Intent(this, FoodSearchActivity::class.java)
            startActivity(intent)
        }

        // 设置跳转到手动记录食物页面
        btnFoodRecord.setOnClickListener {
            val intent = Intent(this,FoodRecordActivity::class.java)
            startActivity(intent)
        }

        // 设置跳转到扫描条形码页面

        // 设置跳转到创建自定义食物页面
        btnCreateCustomFood.setOnClickListener {
            val intent = Intent(this, CreateCustomFoodActivity::class.java)
            startActivity(intent)
        }

        // 设置跳转到创建自定义食谱页面
        btnCreateRecipe.setOnClickListener {
            val intent = Intent(this, RecipeActivity::class.java)
            startActivity(intent)
        }

        // 设置跳转到编辑/删除已记录条目页面

        // 设置跳转到记录自定义食物/谱页面
        btnRecordCustom.setOnClickListener {
            val intent = Intent(this, SelectFoodRecipeActivity::class.java)
            startActivity(intent)
        }
    }
}

