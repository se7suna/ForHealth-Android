package com.example.forhealth.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.forhealth.R
import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.user.*
import com.example.forhealth.network.safeApiCall
import com.example.forhealth.utils.DataMapper
import com.example.forhealth.utils.ProfileManager
import kotlinx.coroutines.launch
import java.util.*

class EditDataActivity : AppCompatActivity() {

    private lateinit var tvPrompt: TextView
    private lateinit var tvActivityHint: TextView
    private lateinit var npPicker1: NumberPicker
    private lateinit var npPicker2: NumberPicker
    private lateinit var npPicker3: NumberPicker
    private lateinit var btnNext: Button

    private var step = 0
    private var isRecordChanges = false

    private var gender = "男"
    private var birthYear = Calendar.getInstance().get(Calendar.YEAR)
    private var birthMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var birthDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private var height = 170
    private var weight = 55
    private var activityLevel = "中度活跃"
    private var goalType = "维持体重"
    private var goalWeight = 55
    private var goalWeeks = 10

    private val activityLevels = arrayOf(
        "久坐",       // PAL = 1.2
        "轻度活跃",   // PAL = 1.375
        "中度活跃",   // PAL = 1.55
        "非常活跃",   // PAL = 1.725
        "极其活跃"    // PAL = 1.9
    )

    private val activityHints = arrayOf(
        "基本不运动",
        "进行少量运动或锻炼（每周 1-3 次）",
        "进行中等强度的运动或锻炼（每周 3-5 次）",
        "进行高强度运动或锻炼（每周 6-7 次）",
        "从事体力劳动工作，或经常进行极高强度的训练"
    )

    private val goalTypes = arrayOf("减重", "维持体重", "增重")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_data)

        tvPrompt = findViewById(R.id.tvPrompt)
        tvActivityHint = findViewById(R.id.tvActivityHint)
        npPicker1 = findViewById(R.id.npPicker1)
        npPicker2 = findViewById(R.id.npPicker2)
        npPicker3 = findViewById(R.id.npPicker3)
        btnNext = findViewById(R.id.btnNext)

        // 检查是否是Record Changes模式
        isRecordChanges = intent.getBooleanExtra("isRecordChanges", false)
        
        // 如果是Record Changes模式，从步骤2（身高）开始
        if (isRecordChanges) {
            step = 2
        }

        // 尝试从Intent获取现有数据
        loadExistingData()

        setupStep()
        
        // 更新按钮文本
        updateButtonText()

        btnNext.setOnClickListener {
            saveStep()
            step++
            val maxStep = if (isRecordChanges) 5 else 5 // Record Changes模式从步骤2开始，所以最大步骤还是5
            if (step > maxStep) {
                // 所有步骤完成，保存到后端
                saveAllData()
            } else {
                setupStep()
                updateButtonText()
            }
        }
    }
    
    private fun updateButtonText() {
        val maxStep = if (isRecordChanges) 5 else 5
        btnNext.text = if (step == maxStep) "完成" else "下一步"
    }

    private fun loadExistingData() {
        // 从Intent获取现有数据（如果有）
        intent.getIntExtra("height", -1).let { if (it > 0) height = it }
        intent.getIntExtra("weight", -1).let { if (it > 0) weight = it }
        intent.getStringExtra("gender")?.let { gender = it }
        intent.getIntExtra("birthYear", -1).let { if (it > 0) birthYear = it }
        intent.getIntExtra("birthMonth", -1).let { if (it > 0) birthMonth = it }
        intent.getIntExtra("birthDay", -1).let { if (it > 0) birthDay = it }
        intent.getStringExtra("activityLevel")?.let { activityLevel = it }
        intent.getStringExtra("goalType")?.let { goalType = it }
        intent.getIntExtra("goalWeight", -1).let { if (it > 0) goalWeight = it }
        intent.getIntExtra("goalWeeks", -1).let { if (it > 0) goalWeeks = it }
    }

    private fun setupStep() {
        // 重置NumberPicker显示
        npPicker1.displayedValues = null
        npPicker2.displayedValues = null
        npPicker3.displayedValues = null

        npPicker1.wrapSelectorWheel = true
        npPicker2.wrapSelectorWheel = true
        npPicker3.wrapSelectorWheel = true

        when (step) {
            0 -> { // 性别（Record Changes模式跳过）
                if (isRecordChanges) {
                    step = 2
                    setupStep()
                    return
                }
                tvPrompt.text = "请选择性别"
                tvActivityHint.visibility = TextView.GONE
                npPicker1.minValue = 0
                npPicker1.maxValue = 1
                npPicker1.displayedValues = arrayOf("男", "女")
                npPicker1.value = if (gender == "女") 1 else 0

                npPicker2.visibility = NumberPicker.GONE
                npPicker3.visibility = NumberPicker.GONE
            }

            1 -> { // 出生日期（Record Changes模式跳过）
                if (isRecordChanges) {
                    step = 2
                    setupStep()
                    return
                }
                tvPrompt.text = "请选择出生日期"
                tvActivityHint.visibility = TextView.GONE
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                // 年
                npPicker1.minValue = currentYear - 100
                npPicker1.maxValue = currentYear
                npPicker1.value = birthYear
                npPicker1.visibility = NumberPicker.VISIBLE

                // 月
                npPicker2.minValue = 1
                npPicker2.maxValue = 12
                npPicker2.value = birthMonth
                npPicker2.visibility = NumberPicker.VISIBLE

                // 日
                npPicker3.minValue = 1
                npPicker3.maxValue = 31
                npPicker3.value = birthDay
                npPicker3.visibility = NumberPicker.VISIBLE
            }

            2 -> { // 身高
                tvPrompt.text = "请选择身高 (cm)"
                tvActivityHint.visibility = TextView.GONE
                npPicker1.minValue = 100
                npPicker1.maxValue = 220
                npPicker1.value = height
                npPicker1.visibility = NumberPicker.VISIBLE

                npPicker2.visibility = NumberPicker.GONE
                npPicker3.visibility = NumberPicker.GONE
            }

            3 -> { // 体重
                tvPrompt.text = "请选择体重 (kg)"
                tvActivityHint.visibility = TextView.GONE
                npPicker1.minValue = 30
                npPicker1.maxValue = 150
                npPicker1.value = weight
                npPicker1.visibility = NumberPicker.VISIBLE

                npPicker2.visibility = NumberPicker.GONE
                npPicker3.visibility = NumberPicker.GONE
            }

            4 -> { // 活动水平
                tvPrompt.text = "选择日常活动程度"
                tvActivityHint.visibility = TextView.VISIBLE
                npPicker1.minValue = 0
                npPicker1.maxValue = activityLevels.size - 1
                npPicker1.displayedValues = activityLevels
                val currentIndex = activityLevels.indexOf(activityLevel).let { if (it >= 0) it else 2 }
                npPicker1.value = currentIndex
                npPicker1.visibility = NumberPicker.VISIBLE

                tvActivityHint.text = activityHints[npPicker1.value]

                npPicker1.setOnValueChangedListener { _, _, newVal ->
                    tvActivityHint.text = activityHints[newVal]
                }

                npPicker2.visibility = NumberPicker.GONE
                npPicker3.visibility = NumberPicker.GONE
            }

            5 -> { // 健康目标
                tvPrompt.text = "设定健康目标"
                tvActivityHint.visibility = TextView.GONE

                // 目标类型
                npPicker1.minValue = 0
                npPicker1.maxValue = goalTypes.size - 1
                npPicker1.displayedValues = goalTypes
                val currentGoalIndex = goalTypes.indexOf(goalType).let { if (it >= 0) it else 1 }
                npPicker1.value = currentGoalIndex
                npPicker1.visibility = NumberPicker.VISIBLE

                // 目标体重
                npPicker2.minValue = 30
                npPicker2.maxValue = 150
                npPicker2.value = goalWeight
                npPicker2.visibility = NumberPicker.VISIBLE

                // 目标周期（周）
                npPicker3.minValue = 1
                npPicker3.maxValue = 52
                npPicker3.value = goalWeeks
                npPicker3.visibility = NumberPicker.VISIBLE
            }
        }
    }

    private fun saveStep() {
        when (step) {
            0 -> {
                if (!isRecordChanges) {
                    gender = npPicker1.displayedValues[npPicker1.value]
                }
            }
            1 -> {
                if (!isRecordChanges) {
                    birthYear = npPicker1.value
                    birthMonth = npPicker2.value
                    birthDay = npPicker3.value
                }
            }
            2 -> height = npPicker1.value
            3 -> weight = npPicker1.value
            4 -> activityLevel = activityLevels[npPicker1.value]
            5 -> {
                goalType = goalTypes[npPicker1.value]
                goalWeight = npPicker2.value
                goalWeeks = npPicker3.value
            }
        }
    }

    private fun saveAllData() {
        val loadingToast = Toast.makeText(this, "正在保存...", Toast.LENGTH_SHORT)
        loadingToast.show()

        // 先保存到本地（用于无后端测试）
        val existingProfile = ProfileManager.getProfile(this)
        
        // 如果是Record Changes模式，保留原有的性别和出生日期
        val genderBackend = if (isRecordChanges && existingProfile?.gender != null) {
            existingProfile.gender
        } else {
            DataMapper.genderToBackend(gender)
        }
        
        val birthdate = if (isRecordChanges && existingProfile?.birthdate != null) {
            existingProfile.birthdate
        } else {
            DataMapper.birthDateToBackend(birthYear, birthMonth, birthDay)
        }
        
        val activityLevelBackend = DataMapper.activityLevelToBackend(activityLevel)
        val goalTypeBackend = DataMapper.goalTypeToBackend(goalType)
        
        // 获取现有资料或创建新的
        val updatedProfile = existingProfile?.copy(
            height = height.toDouble(),
            weight = weight.toDouble(),
            gender = genderBackend,
            birthdate = birthdate,
            activity_level = activityLevelBackend,
            health_goal_type = goalTypeBackend,
            target_weight = goalWeight.toDouble(),
            goal_period_weeks = goalWeeks
        ) ?: UserProfileResponse(
            email = "test@example.com",
            username = "Test User",
            height = height.toDouble(),
            weight = weight.toDouble(),
            gender = genderBackend,
            birthdate = birthdate,
            activity_level = activityLevelBackend,
            health_goal_type = goalTypeBackend,
            target_weight = goalWeight.toDouble(),
            goal_period_weeks = goalWeeks
        )
        
        ProfileManager.saveProfile(this, updatedProfile)
        
        // 尝试保存到后端（如果后端可用）
        lifecycleScope.launch {
            try {
                // 1. 保存身体数据
                val bodyDataRequest = BodyDataRequest(
                    height = height.toDouble(),
                    weight = weight.toDouble(),
                    birthdate = birthdate,
                    gender = genderBackend
                )
                val bodyDataResult = safeApiCall {
                    RetrofitClient.apiService.updateBodyData(bodyDataRequest)
                }
                if (bodyDataResult is ApiResult.Error) {
                    // 后端失败，但本地已保存，继续
                    android.util.Log.w("EditDataActivity", "后端保存身体数据失败，已保存到本地: ${bodyDataResult.message}")
                }

                // 2. 保存活动水平
                val activityLevelRequest = ActivityLevelRequest(
                    activity_level = activityLevelBackend
                )
                val activityResult = safeApiCall {
                    RetrofitClient.apiService.updateActivityLevel(activityLevelRequest)
                }
                if (activityResult is ApiResult.Error) {
                    // 后端失败，但本地已保存，继续
                    android.util.Log.w("EditDataActivity", "后端保存活动水平失败，已保存到本地: ${activityResult.message}")
                }

                // 3. 保存健康目标
                val healthGoalRequest = HealthGoalRequest(
                    health_goal_type = goalTypeBackend,
                    target_weight = goalWeight.toDouble(),
                    goal_period_weeks = goalWeeks
                )
                val goalResult = safeApiCall {
                    RetrofitClient.apiService.updateHealthGoal(healthGoalRequest)
                }
                // 无论后端成功与否，本地已保存
                loadingToast.cancel()
                Toast.makeText(this@EditDataActivity, "信息保存成功", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                // 网络错误，但本地已保存
                loadingToast.cancel()
                Toast.makeText(this@EditDataActivity, "信息已保存到本地", Toast.LENGTH_SHORT).show()
                android.util.Log.w("EditDataActivity", "后端保存失败，已保存到本地", e)
                finish()
            }
        }
    }
}

