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

            // 保存所有用户数据到后端
            saveUserData(gender, birthYear, birthMonth, birthDay, height, weight, activityLevel, goalType, goalWeight, goalWeeks)
        }
    }

    private fun saveUserData(
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
        // 显示加载提示
        val loadingToast = Toast.makeText(this, "正在保存...", Toast.LENGTH_SHORT)
        loadingToast.show()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 使用DataMapper转换数据为后端格式
                val userData = mapOf(
                    "gender" to DataMapper.genderToBackend(gender),
                    "birthdate" to DataMapper.birthDateToBackend(birthYear, birthMonth, birthDay),
                    "height" to height.toDouble(),
                    "weight" to weight.toDouble(),
                    "activity_level" to DataMapper.activityLevelToBackend(activityLevel),
                    "health_goal_type" to DataMapper.goalTypeToBackend(goalType),
                    "target_weight" to goalWeight.toDouble(),
                    "goal_period_weeks" to goalWeeks
                )
                
                // 记录发送的数据，方便调试
                android.util.Log.d("HealthGoalActivity", "准备发送数据: $userData")
                android.util.Log.d("HealthGoalActivity", "性别: ${DataMapper.genderToBackend(gender)}")
                android.util.Log.d("HealthGoalActivity", "出生日期: ${DataMapper.birthDateToBackend(birthYear, birthMonth, birthDay)}")
                android.util.Log.d("HealthGoalActivity", "活动水平: ${DataMapper.activityLevelToBackend(activityLevel)}")
                android.util.Log.d("HealthGoalActivity", "目标类型: ${DataMapper.goalTypeToBackend(goalType)}")

                val result = RetrofitClient.apiService.updateProfile(userData)
                result.fold(
                    onSuccess = { user ->
                        withContext(Dispatchers.Main) {
                            loadingToast.cancel()
                            Toast.makeText(this@HealthGoalActivity, "信息保存成功", Toast.LENGTH_SHORT).show()
                            // 跳转到主页
                            val intent = Intent(this@HealthGoalActivity, HomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    },
                    onFailure = { e ->
                        withContext(Dispatchers.Main) {
                            loadingToast.cancel()
                            // 显示详细的错误信息
                            val errorMsg = e.message ?: "保存失败"
                            Toast.makeText(this@HealthGoalActivity, errorMsg, Toast.LENGTH_LONG).show()
                            // 同时在Log中输出详细错误，方便调试
                            android.util.Log.e("HealthGoalActivity", "保存失败: ${e.message}", e)
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingToast.cancel()
                    val errorMsg = "网络错误: ${e.message ?: e.javaClass.simpleName}"
                    Toast.makeText(this@HealthGoalActivity, errorMsg, Toast.LENGTH_LONG).show()
                    android.util.Log.e("HealthGoalActivity", "异常: ${e.message}", e)
                }
            }
        }
    }
}