package com.example.forhealth.user

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.forhealth.database.DatabaseHelper
import com.example.forhealth.R

class FoodRecordActivity : AppCompatActivity() {

    private lateinit var foodNameTextView: TextView
    private lateinit var portionEditText: EditText
    private lateinit var unitSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button

    private lateinit var dbHelper: DatabaseHelper

    private var recordId: Long = -1  // 用于存储当前编辑记录的ID
    private var foodName: String = ""  // 用于存储当前记录的食物名称
    private var caloriesPer100g: Double = 0.0  // 用于存储食物每100克的卡路里

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_record)

        foodNameTextView = findViewById(R.id.foodNameTextView)
        portionEditText = findViewById(R.id.portionEditText)
        unitSpinner = findViewById(R.id.unitSpinner)
        saveButton = findViewById(R.id.saveButton)
        deleteButton = findViewById(R.id.deleteButton)

        dbHelper = DatabaseHelper(this)

        // 获取传递过来的记录ID和其他数据
        recordId = intent.getLongExtra("RECORD_ID", -1)
        foodName = intent.getStringExtra("FOOD_NAME") ?: ""
        caloriesPer100g = intent.getDoubleExtra("CALORIES", 0.0)

        if (recordId != -1L) {
            // 如果有传递记录ID，填充数据
            foodNameTextView.text = foodName
            portionEditText.setText(intent.getDoubleExtra("PORTION", 0.0).toString())
        } else {
            // 如果没有传递ID，说明是添加新记录
            foodNameTextView.text = foodName
        }

        // 如果食物记录的卡路里在数据库中，显示
        if (caloriesPer100g != 0.0) {
            Toast.makeText(this, "$foodName 卡路里: $caloriesPer100g 每100克", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener {
            val portionText = portionEditText.text.toString()
            val portion = portionText.toDoubleOrNull()

            if (portion != null) {
                val totalCalories = (portion / 100) * caloriesPer100g  // 重新计算总卡路里
                val timestamp = System.currentTimeMillis()

                // 更新或保存记录
                if (recordId != -1L) {
                    // 更新已有记录
                    val result = dbHelper.updateFoodRecord(recordId, foodName, portion, totalCalories)
                    if (result > 0) {
                        Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "修改失败", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 保存新的记录
                    val id = dbHelper.insertFoodRecord(foodName, portion, totalCalories, timestamp)
                    if (id != -1L) {
                        Toast.makeText(this, "记录已保存", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
                    }
                }
                finish()  // 完成后返回
            } else {
                Toast.makeText(this, "请输入有效的份量", Toast.LENGTH_SHORT).show()
            }
        }

        // 删除按钮点击事件
        deleteButton.setOnClickListener {
            if (recordId != -1L) {
                // 弹出删除确认框
                AlertDialog.Builder(this)
                    .setTitle("删除确认")
                    .setMessage("你确定要删除这条记录吗？")
                    .setPositiveButton("删除") { _, _ ->
                        val result = dbHelper.deleteFoodRecord(recordId)
                        if (result > 0) {
                            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()
                            finish()  // 删除成功后返回
                        } else {
                            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }
}

