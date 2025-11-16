package com.example.forhealth.user

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.forhealth.R
import com.example.forhealth.database.CustomExerciseDatabaseHelper

class ViewExerciseHistoryActivity : AppCompatActivity() {

    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var filterButton: Button
    private lateinit var exerciseHistoryTextView: TextView
    private lateinit var dbHelper: CustomExerciseDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_exercise_history)

        // Initialize views
        startDateEditText = findViewById(R.id.startDateEditText)
        endDateEditText = findViewById(R.id.endDateEditText)
        filterButton = findViewById(R.id.filterButton)
        exerciseHistoryTextView = findViewById(R.id.exerciseHistoryTextView)

        // Initialize database helper
        dbHelper = CustomExerciseDatabaseHelper(this)
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            // 返回到 SportActivity
            val intent = Intent(this, SportActivity::class.java)
            startActivity(intent)
            finish()  // 结束当前 Activity
        }
        // Filter button click listener
        filterButton.setOnClickListener {
            val startDate = startDateEditText.text.toString()
            val endDate = endDateEditText.text.toString()

            // Validate date inputs
            if (startDate.isEmpty() || endDate.isEmpty()) {
                Toast.makeText(this, "Please enter a valid date range", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Fetch data from database
            val exerciseHistory = dbHelper.getExerciseHistoryByDateRange(startDate, endDate)
            if (exerciseHistory.isNotEmpty()) {
                exerciseHistoryTextView.text = exerciseHistory.joinToString("\n")
            } else {
                Toast.makeText(this, "No records found for this date range", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
