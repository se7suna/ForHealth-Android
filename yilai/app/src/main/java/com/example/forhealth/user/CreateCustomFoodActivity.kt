package com.example.forhealth.user

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.forhealth.R
import com.example.forhealth.database.CustomFoodDatabaseHelper

class CreateCustomFoodActivity : AppCompatActivity() {

    private lateinit var etFoodName: EditText
    private lateinit var etQuantity: EditText
    private lateinit var etCalories: EditText
    private lateinit var etProtein: EditText
    private lateinit var etFat: EditText
    private lateinit var etCarbs: EditText
    private lateinit var btnSave: Button
    private lateinit var dbHelper: CustomFoodDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_custom_food)

        // Initialize views
        etFoodName = findViewById(R.id.etFoodName)
        etQuantity = findViewById(R.id.etQuantity)
        etCalories = findViewById(R.id.etCalories)
        etProtein = findViewById(R.id.etProtein)
        etFat = findViewById(R.id.etFat)
        etCarbs = findViewById(R.id.etCarbs)
        btnSave = findViewById(R.id.btnSave)

        dbHelper = CustomFoodDatabaseHelper(this)

        // Save button click listener
        btnSave.setOnClickListener {
            val foodName = etFoodName.text.toString()
            val quantity = etQuantity.text.toString()
            val calories = etCalories.text.toString()
            val protein = etProtein.text.toString()
            val fat = etFat.text.toString()
            val carbs = etCarbs.text.toString()

            // Validate input data
            if (foodName.isNotEmpty() && quantity.isNotEmpty() && calories.isNotEmpty() && protein.isNotEmpty() && fat.isNotEmpty() && carbs.isNotEmpty()) {
                // Save to database
                val success = dbHelper.insertCustomFood(foodName, quantity, calories.toDouble(), protein.toDouble(), fat.toDouble(), carbs.toDouble())
                if (success != -1L) {
                    Toast.makeText(this, "创建成功", Toast.LENGTH_SHORT).show()
                    finish()  // Close activity after saving
                } else {
                    Toast.makeText(this, "保存失败，请稍后再试", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
