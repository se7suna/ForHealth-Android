package com.example.forhealth.user

import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.forhealth.R
import com.example.forhealth.database.AppDatabase  // 确保导入正确的数据库类
import com.example.forhealth.database.CustomExercise  // 确保导入正确的数据库实体类

class CreateCustomExerciseActivity : AppCompatActivity() {

    private lateinit var exerciseNameEditText: EditText
    private lateinit var metsValueEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_custom_exercise)

        // 初始化视图
        exerciseNameEditText = findViewById(R.id.exerciseNameEditText)
        metsValueEditText = findViewById(R.id.metsValueEditText)
        saveButton = findViewById(R.id.saveButton)

        // 初始化 Room 数据库
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "app_database").build()

        // 保存按钮点击事件
        saveButton.setOnClickListener {
            val exerciseName = exerciseNameEditText.text.toString().trim()
            val metsValueString = metsValueEditText.text.toString().trim()

            // 验证输入
            if (exerciseName.isEmpty() || metsValueString.isEmpty()) {
                Toast.makeText(this, "请填写完整的信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 验证 METs 值是否合法（范围：0 - 20）
            val metsValue = metsValueString.toFloatOrNull()
            if (metsValue == null || metsValue < 0 || metsValue > 20) {
                Toast.makeText(this, "请输入有效的METs值（0-20）", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 创建 CustomExercise 实例并插入到数据库
            val customExercise = CustomExercise(name = exerciseName, metsValue = metsValue)

            // 使用协程进行数据库操作
            GlobalScope.launch(Dispatchers.IO) {
                db.customExerciseDao().insert(customExercise)

                // 操作完成后切换回主线程更新 UI
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateCustomExerciseActivity, "自定义运动创建成功", Toast.LENGTH_SHORT).show()
                    finish()  // 创建成功后关闭该 Activity
                }
            }
        }
    }
}
