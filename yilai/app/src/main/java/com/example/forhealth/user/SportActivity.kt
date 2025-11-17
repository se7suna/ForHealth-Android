package com.example.forhealth.user


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.forhealth.R  // 导入正确的包名
import com.example.forhealth.HomeActivity
class SportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sport)  // 加载 activity_sport.xml 布局

        // 获取并设置返回按钮点击事件
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            // 跳转到 HomeActivity 或你需要的页面
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()  // 结束当前页面
        }

        // 获取并设置其他按钮的点击事件
        val btnSearchExercise = findViewById<Button>(R.id.btnSearchExercise)
        btnSearchExercise.setOnClickListener {
            // 跳转到 SearchExerciseActivity
            val intent = Intent(this, SearchExerciseActivity::class.java)
            startActivity(intent)
        }

        val btnRecordExercise = findViewById<Button>(R.id.btnRecordExercise)
        btnRecordExercise.setOnClickListener {
            // 跳转到 RecordExerciseActivity
            val intent = Intent(this, RecordExerciseActivity::class.java)
            startActivity(intent)
        }

        val btnGeneratePlan = findViewById<Button>(R.id.btnGeneratePlan)
        btnGeneratePlan.setOnClickListener {
            // 跳转到 GenerateExercisePlanActivity
            val intent = Intent(this, GenerateExercisePlanActivity::class.java)
            startActivity(intent)
        }

        val btnViewHistory = findViewById<Button>(R.id.btnViewHistory)
        btnViewHistory.setOnClickListener {
            // 跳转到 ViewExerciseHistoryActivity
            val intent = Intent(this, ViewExerciseHistoryActivity::class.java)
            startActivity(intent)
        }

        val btnCreateCustomExercise = findViewById<Button>(R.id.btnCreateCustomExercise)
        btnCreateCustomExercise.setOnClickListener {
            // 跳转到 CreateCustomExerciseActivity
            val intent = Intent(this, CreateCustomExerciseActivity::class.java)
            startActivity(intent)
        }
    }
}
