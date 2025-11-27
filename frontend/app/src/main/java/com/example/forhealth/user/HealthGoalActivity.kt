package com.example.forhealth.user

import com.example.forhealth.R
import com.example.forhealth.HomeActivity
import android.content.Intent
import android.widget.Button
import android.os.Bundle
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.utils.DataMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HealthGoalActivity : AppCompatActivity() {

    private lateinit var tvPrompt: TextView
    private lateinit var npGoalType: NumberPicker
    private lateinit var npGoalWeight: NumberPicker
    private lateinit var npGoalWeeks: NumberPicker
    private lateinit var btnNext: Button

    private val goalTypes = arrayOf("减重","维持体重","增重")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_goal)

        tvPrompt = findViewById(R.id.tvPrompt)
        npGoalType = findViewById(R.id.npGoalType)
        npGoalWeight = findViewById(R.id.npGoalWeight)
        npGoalWeeks = findViewById(R.id.npGoalWeeks)
        btnNext = findViewById(R.id.btnNext)

        // 目标类型
        npGoalType.minValue = 0
        npGoalType.maxValue = goalTypes.size - 1
        npGoalType.displayedValues = goalTypes
        npGoalType.value = 1

        // 目标体重
        npGoalWeight.minValue = 30
        npGoalWeight.maxValue = 150
        npGoalWeight.value = 55

        // 目标周期（周）
        npGoalWeeks.minValue = 1
        npGoalWeeks.maxValue = 52
        npGoalWeeks.value = 10

        btnNext.setOnClickListener {
            val goalType = goalTypes[npGoalType.value]
            val goalWeight = npGoalWeight.value
            val goalWeeks = npGoalWeeks.value

            // 获取之前传递的数据
            val gender = intent.getStringExtra("gender") ?: "男"
            val birthYear = intent.getIntExtra("birthYear", 1990)
            val birthMonth = intent.getIntExtra("birthMonth", 1)
            val birthDay = intent.getIntExtra("birthDay", 1)
            val height = intent.getIntExtra("height", 170)
            val weight = intent.getIntExtra("weight", 60)
            val activityLevel = intent.getStringExtra("activityLevel") ?: "中度活跃"

            // 模拟保存数据，不调用后端
            saveUserDataSimulated(gender, birthYear, birthMonth, birthDay, height, weight, activityLevel, goalType, goalWeight, goalWeeks)
        }
    }

    private fun saveUserDataSimulated(
        gender: String,
        birthYear: Int,
        birthMonth: Int,
        birthDay: Int,
        height: Int,
        weight: Int,
        activityLevel: String,
        goalType: String,
        goalWeight: Int,
        goalWeeks: Int
    ) {
        // 直接弹出保存成功，模拟网络请求
        Toast.makeText(this, "信息保存成功（模拟）", Toast.LENGTH_SHORT).show()

        // 打印日志方便调试
        android.util.Log.d("HealthGoalActivity", "模拟保存用户数据:")
        android.util.Log.d("HealthGoalActivity", "gender: $gender, birthdate: $birthYear-$birthMonth-$birthDay")
        android.util.Log.d("HealthGoalActivity", "height: $height, weight: $weight")
        android.util.Log.d("HealthGoalActivity", "activityLevel: $activityLevel")
        android.util.Log.d("HealthGoalActivity", "goalType: $goalType, goalWeight: $goalWeight, goalWeeks: $goalWeeks")

        // 模拟跳转主页
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
