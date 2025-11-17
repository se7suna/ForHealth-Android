package com.example.forhealth.user

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.forhealth.R
import com.example.forhealth.database.ExerciseSearchDatabaseHelper

class SearchExerciseActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var exerciseListView: ListView
    private lateinit var dbHelper: ExerciseSearchDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_exercise)

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        exerciseListView = findViewById(R.id.exerciseListView)

        // Initialize database helper
        dbHelper = ExerciseSearchDatabaseHelper(this)

        // Search button click listener
        searchButton.setOnClickListener {
            val keyword = searchEditText.text.toString().trim()

            if (keyword.isEmpty()) {
                Toast.makeText(this, "Please enter a keyword", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Search exercises in database based on the keyword
            val exerciseResults = dbHelper.searchExercisesByKeyword(keyword)

            if (exerciseResults.isNotEmpty()) {
                // Display results in ListView
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, exerciseResults)
                exerciseListView.adapter = adapter
            } else {
                Toast.makeText(this, "No exercises found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
