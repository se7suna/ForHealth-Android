package com.example.forhealth.user

import com.example.forhealth.R
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ActivityLevelActivity : AppCompatActivity() {

    private lateinit var tvPrompt: TextView
    private lateinit var tvActivityHint: TextView
    private lateinit var npActivity: NumberPicker
    private lateinit var btnNext: Button

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activity_level)

        tvPrompt = findViewById(R.id.tvPrompt)
        tvActivityHint = findViewById(R.id.tvActivityHint)
        npActivity = findViewById(R.id.npActivity)
        btnNext = findViewById(R.id.btnNext)

        npActivity.minValue = 0
        npActivity.maxValue = activityLevels.size - 1
        npActivity.displayedValues = activityLevels
        npActivity.value = 2 // 默认中度活跃

        tvActivityHint.text = activityHints[npActivity.value]

        npActivity.setOnValueChangedListener { _, _, newVal ->
            tvActivityHint.text = activityHints[newVal]
        }

        btnNext.setOnClickListener {
            val intent = Intent(this, HealthGoalActivity::class.java)
            intent.putExtra("activityLevel", activityLevels[npActivity.value])
            startActivity(intent)
            finish()
        }
    }
}
