package com.example.forhealth.user

import com.example.forhealth.R
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class BodyDataActivity : AppCompatActivity() {

    private lateinit var tvPrompt: TextView
    private lateinit var npPicker1: NumberPicker
    private lateinit var npPicker2: NumberPicker
    private lateinit var npPicker3: NumberPicker
    private lateinit var btnNext: Button

    private var step = 0

    private var gender = "男"
    private var birthYear = Calendar.getInstance().get(Calendar.YEAR)
    private var birthMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var birthDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private var height = 170
    private var weight = 55

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_body_data)

        tvPrompt = findViewById(R.id.tvPrompt)
        npPicker1 = findViewById(R.id.npPicker1)
        npPicker2 = findViewById(R.id.npPicker2)
        npPicker3 = findViewById(R.id.npPicker3)
        btnNext = findViewById(R.id.btnNext)

        setupStep()

        btnNext.setOnClickListener {
            saveStep()
            step++
            if (step > 3) {
                // 跳转到活动水平录入
                val intent = Intent(this, ActivityLevelActivity::class.java)
                intent.putExtra("gender", gender)
                intent.putExtra("birthYear", birthYear)
                intent.putExtra("birthMonth", birthMonth)
                intent.putExtra("birthDay", birthDay)
                intent.putExtra("height", height)
                intent.putExtra("weight", weight)
                startActivity(intent)
                finish()
            } else {
                setupStep()
            }
        }
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
            0 -> { // 性别
                tvPrompt.text = "请选择性别"
                npPicker1.minValue = 0
                npPicker1.maxValue = 1
                npPicker1.displayedValues = arrayOf("男", "女")
                npPicker1.value = 0

                npPicker2.visibility = NumberPicker.GONE
                npPicker3.visibility = NumberPicker.GONE
            }

            1 -> {
                tvPrompt.text = "请选择出生日期"

                val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                // 年
                npPicker1.minValue = currentYear - 100
                npPicker1.maxValue = currentYear
                npPicker1.value = currentYear - 30
                npPicker1.visibility = NumberPicker.VISIBLE

                // 月
                npPicker2.minValue = 1
                npPicker2.maxValue = 12
                npPicker2.value = 1
                npPicker2.visibility = NumberPicker.VISIBLE

                // 日
                npPicker3.minValue = 1
                npPicker3.maxValue = 31
                npPicker3.value = 1
                npPicker3.visibility = NumberPicker.VISIBLE
            }

            2 -> { // 身高
                tvPrompt.text = "请选择身高 (cm)"
                npPicker1.minValue = 100
                npPicker1.maxValue = 220
                npPicker1.value = 170
                npPicker1.visibility = NumberPicker.VISIBLE

                npPicker2.visibility = NumberPicker.GONE
                npPicker3.visibility = NumberPicker.GONE
            }

            3 -> { // 体重
                tvPrompt.text = "请选择体重 (kg)"
                npPicker1.minValue = 30
                npPicker1.maxValue = 150
                npPicker1.value = 55
                npPicker1.visibility = NumberPicker.VISIBLE

                npPicker2.visibility = NumberPicker.GONE
                npPicker3.visibility = NumberPicker.GONE
            }
        }
    }

    private fun saveStep() {
        when (step) {
            0 -> gender = npPicker1.displayedValues[npPicker1.value]
            1 -> {
                birthYear = npPicker1.value
                birthMonth = npPicker2.value
                birthDay = npPicker3.value
            }
            2 -> height = npPicker1.value
            3 -> weight = npPicker1.value
        }
    }
}

private fun Unit.translationX(f: Float) {}
