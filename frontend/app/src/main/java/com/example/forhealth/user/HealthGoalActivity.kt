package com.example.forhealth.user

import com.example.forhealth.R
import android.content.Intent
import android.widget.Button
import android.os.Bundle
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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

            Toast.makeText(this,
                "目标: $goalType\n目标体重: $goalWeight kg\n周期: $goalWeeks 周",
                Toast.LENGTH_LONG
            ).show()

            // TODO: 跳转到下一页面或保存数据
            finish()
        }
    }
}