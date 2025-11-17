package com.example.forhealth.user

import android.os.Bundle
import android.widget.Spinner
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import com.example.forhealth.R
import com.example.forhealth.database.CustomExercise
import com.example.forhealth.database.CustomExerciseDatabaseHelper

class RecordExerciseActivity : AppCompatActivity() {

    private lateinit var customExerciseSpinner: Spinner
    private lateinit var durationEditText: EditText
    private lateinit var intensityEditText: EditText
    private lateinit var calculateButton: Button
    private lateinit var dbHelper: CustomExerciseDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_exercise)

        // 初始化视图
        customExerciseSpinner = findViewById(R.id.customExerciseSpinner)
        durationEditText = findViewById(R.id.durationEditText)
        intensityEditText = findViewById(R.id.intensityEditText)
        calculateButton = findViewById(R.id.calculateButton)

        // 初始化数据库帮助类
        dbHelper = CustomExerciseDatabaseHelper(this)

        // 设置 Spinner 的适配器
        val exerciseList = listOf("室内跳绳", "跑步", "骑行") // 示例数据
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, exerciseList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        customExerciseSpinner.adapter = adapter
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            // 返回到 SportActivity
            val intent = Intent(this, SportActivity::class.java)
            startActivity(intent)
            finish()  // 结束当前 Activity
        }
        // 计算按钮点击事件
        calculateButton.setOnClickListener {
            val selectedExercise = customExerciseSpinner.selectedItem.toString()
            val duration = durationEditText.text.toString().toFloatOrNull()
            val intensity = intensityEditText.text.toString().toFloatOrNull()

            if (duration == null || intensity == null) {
                Toast.makeText(this, "请输入有效的时长和强度", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 查询数据库中的 METs 值
            val metsValue = dbHelper.getMetsValueForExercise(selectedExercise)

            // 计算卡路里
            val caloriesBurned = calculateCalories(metsValue, duration, intensity)
            Toast.makeText(this, "消耗的卡路里: $caloriesBurned kcal", Toast.LENGTH_SHORT).show()
        }
    }

    // 计算卡路里消耗的函数
    private fun calculateCalories(metsValue: Float, duration: Float, intensity: Float): Float {
        return metsValue * duration * intensity
    }
}
