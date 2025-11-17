package com.example.forhealth.user
import com.example.forhealth.R
import android.os.Bundle
import com.example.forhealth.HomeActivity
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.content.Intent
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // 加载布局

        // 获取按钮
        val btnFood = findViewById<Button>(R.id.btnFood)
        val btnSport = findViewById<Button>(R.id.btnSport)
        val btnBackToFood = findViewById<Button>(R.id.btnBackToHome)
        btnBackToFood.setOnClickListener {
            // 跳转回 FoodActivity
            val intent = Intent(this, HomeActivity::class.java)  // 返回到 FoodActivity
            startActivity(intent)
            finish()  // 结束当前页面
        }
        // 设置食物记录按钮点击事件
        btnFood.setOnClickListener {
            val intent = Intent(this, FoodActivity::class.java)  // 创建跳转到 FoodActivity 的 Intent
            startActivity(intent)  // 启动活动
        }

        // 设置运动搜索按钮点击事件
        btnSport.setOnClickListener {
            val intent = Intent(this, SportActivity::class.java)  // 创建跳转到 SportActivity 的 Intent
            startActivity(intent)  // 启动活动
        }
    }
}
