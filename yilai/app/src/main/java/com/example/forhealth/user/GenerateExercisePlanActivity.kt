package com.example.forhealth.user

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.forhealth.R
import com.example.forhealth.model.ExercisePlanRequest
import com.example.forhealth.model.Message
import com.example.forhealth.model.ExercisePlanResponse
import com.example.forhealth.network.AIExercisePlanRetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GenerateExercisePlanActivity : AppCompatActivity() {

    private lateinit var goalSpinner: Spinner
    private lateinit var fitnessLevelSpinner: Spinner
    private lateinit var preferredExerciseEditText: EditText
    private lateinit var frequencyEditText: EditText
    private lateinit var durationEditText: EditText
    private lateinit var intensitySpinner: Spinner
    private lateinit var generateButton: Button
    private lateinit var exercisePlanTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_exercise_plan)

        // Initialize views
        goalSpinner = findViewById(R.id.goalSpinner)
        fitnessLevelSpinner = findViewById(R.id.fitnessLevelSpinner)
        preferredExerciseEditText = findViewById(R.id.preferredExerciseEditText)
        frequencyEditText = findViewById(R.id.frequencyEditText)
        durationEditText = findViewById(R.id.durationEditText)
        intensitySpinner = findViewById(R.id.intensitySpinner)
        generateButton = findViewById(R.id.generateButton)
        exercisePlanTextView = findViewById(R.id.exercisePlanTextView)

        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            // 返回到 SportActivity
            val intent = Intent(this, SportActivity::class.java)
            startActivity(intent)
            finish()  // 结束当前 Activity
        }
        // Generate button click listener
        generateButton.setOnClickListener {
            val goal = goalSpinner.selectedItem.toString()
            val fitnessLevel = fitnessLevelSpinner.selectedItem.toString()
            val preferredExercise = preferredExerciseEditText.text.toString().trim()
            val frequency = frequencyEditText.text.toString().toInt()
            val duration = durationEditText.text.toString().toInt()
            val intensity = intensitySpinner.selectedItem.toString()

            // Create request data
            val messages = listOf(
                Message(role = "system", content = "You are a helpful assistant."),
                Message(role = "user", content = "Generate a personalized exercise plan based on the goal: $goal and fitness level: $fitnessLevel.")
            )

            val request = ExercisePlanRequest(messages = messages)

            // Call the external DeepSeek API
            AIExercisePlanRetrofitClient.api.getChatCompletion(request).enqueue(object : Callback<ExercisePlanResponse> {
                override fun onResponse(call: Call<ExercisePlanResponse>, response: Response<ExercisePlanResponse>) {
                    if (response.isSuccessful) {
                        val plan = response.body()
                        if (plan != null) {
                            // Display the generated exercise plan
                            exercisePlanTextView.text = plan.choices[0].message.content
                        }
                    } else {
                        Toast.makeText(this@GenerateExercisePlanActivity, "生成运动计划失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ExercisePlanResponse>, t: Throwable) {
                    Toast.makeText(this@GenerateExercisePlanActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
