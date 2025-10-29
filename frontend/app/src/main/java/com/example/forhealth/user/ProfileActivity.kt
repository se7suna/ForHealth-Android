package com.example.forhealth.user

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.forhealth.R
import com.example.forhealth.model.User
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.utils.DataMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvEmail: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvBirthDate: TextView
    private lateinit var tvHeight: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvActivityLevel: TextView
    private lateinit var tvGoalType: TextView
    private lateinit var tvGoalWeight: TextView
    private lateinit var tvGoalWeeks: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // 设置ActionBar
        supportActionBar?.title = "个人信息"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 初始化视图
        tvEmail = findViewById(R.id.tvEmail)
        tvGender = findViewById(R.id.tvGender)
        tvBirthDate = findViewById(R.id.tvBirthDate)
        tvHeight = findViewById(R.id.tvHeight)
        tvWeight = findViewById(R.id.tvWeight)
        tvActivityLevel = findViewById(R.id.tvActivityLevel)
        tvGoalType = findViewById(R.id.tvGoalType)
        tvGoalWeight = findViewById(R.id.tvGoalWeight)
        tvGoalWeeks = findViewById(R.id.tvGoalWeeks)

        // 加载用户信息
        loadUserProfile()
    }

    private fun loadUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = RetrofitClient.apiService.getProfile()
                result.fold(
                    onSuccess = { user ->
                        withContext(Dispatchers.Main) {
                            displayUserInfo(user)
                        }
                    },
                    onFailure = { e ->
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ProfileActivity, "加载用户信息失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayUserInfo(user: User) {
        tvEmail.text = "邮箱：${user.email ?: "未设置"}"
        tvGender.text = "性别：${DataMapper.genderFromBackend(user.gender)}"
        
        // 优先使用后端的birthdate字段
        if (user.birthdate != null) {
            tvBirthDate.text = "出生日期：${user.birthdate}"
        } else if (user.birth_year != null && user.birth_month != null && user.birth_day != null) {
            tvBirthDate.text = "出生日期：${user.birth_year}-${user.birth_month}-${user.birth_day}"
        } else {
            tvBirthDate.text = "出生日期：未设置"
        }
        
        tvHeight.text = "身高：${user.height?.let { String.format("%.1f", it) } ?: "未设置"} cm"
        tvWeight.text = "体重：${user.weight?.let { String.format("%.1f", it) } ?: "未设置"} kg"
        tvActivityLevel.text = "活动水平：${DataMapper.activityLevelFromBackend(user.activity_level)}"
        
        // 优先使用health_goal_type，如果没有则使用goal_type
        val goalType = user.health_goal_type ?: user.goal_type
        tvGoalType.text = "健康目标：${DataMapper.goalTypeFromBackend(goalType)}"
        
        // 优先使用target_weight，如果没有则使用goal_weight
        val targetWeight = user.target_weight ?: user.goal_weight
        tvGoalWeight.text = "目标体重：${targetWeight?.let { String.format("%.1f", it) } ?: "未设置"} kg"
        
        // 优先使用goal_period_weeks，如果没有则使用goal_weeks
        val goalWeeks = user.goal_period_weeks ?: user.goal_weeks
        tvGoalWeeks.text = "目标周期：${goalWeeks ?: "未设置"} 周"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

