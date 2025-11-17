package com.example.forhealth.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class CustomExerciseDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "custom_exercises.db"
        const val DATABASE_VERSION = 1

        // 表格名
        const val TABLE_EXERCISE_LOG = "exercise_log"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_EXERCISE_LOG (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_name TEXT NOT NULL,
                duration REAL,
                intensity REAL,
                distance REAL,
                mets_value REAL,
                date TEXT NOT NULL  -- 添加日期字段
            )
        """
        db.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXERCISE_LOG")
        onCreate(db)
    }

    // 插入新运动记录
    fun insertExercise(exerciseName: String, duration: Float, intensity: Float, distance: Float, metsValue: Float) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("exercise_name", exerciseName)
            put("duration", duration)
            put("intensity", intensity)
            put("distance", distance)
            put("mets_value", metsValue)
            put("date", getCurrentDate())  // 获取当前日期
        }
        db.insert(TABLE_EXERCISE_LOG, null, values)
    }

    // 获取当前日期
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    // 根据日期范围查询运动记录
    // 根据日期范围查询运动记录
    fun getExerciseHistoryByDateRange(startDate: String, endDate: String): List<String> {
        val db = readableDatabase
        val query = "SELECT exercise_name, duration, intensity, distance, mets_value, date FROM $TABLE_EXERCISE_LOG WHERE date BETWEEN ? AND ?"
        val cursor = db.rawQuery(query, arrayOf(startDate, endDate))

        val historyList = mutableListOf<String>()

        // 确保列名和数据库中的列名一致
        val exerciseNameColumnIndex = cursor.getColumnIndex("exercise_name")
        val durationColumnIndex = cursor.getColumnIndex("duration")
        val intensityColumnIndex = cursor.getColumnIndex("intensity")
        val distanceColumnIndex = cursor.getColumnIndex("distance")
        val metsValueColumnIndex = cursor.getColumnIndex("mets_value")
        val dateColumnIndex = cursor.getColumnIndex("date")

        // 检查返回的列索引是否有效
        if (exerciseNameColumnIndex == -1 || durationColumnIndex == -1 || intensityColumnIndex == -1 ||
            distanceColumnIndex == -1 || metsValueColumnIndex == -1 || dateColumnIndex == -1) {
            throw Exception("One or more columns not found")
        }

        while (cursor.moveToNext()) {
            val exerciseName = cursor.getString(exerciseNameColumnIndex)
            val duration = cursor.getFloat(durationColumnIndex)
            val intensity = cursor.getFloat(intensityColumnIndex)
            val distance = cursor.getFloat(distanceColumnIndex)
            val metsValue = cursor.getFloat(metsValueColumnIndex)
            val date = cursor.getString(dateColumnIndex)

            historyList.add("Exercise: $exerciseName, Date: $date, Duration: $duration, Intensity: $intensity, Distance: $distance, METs: $metsValue")
        }

        cursor.close()
        return historyList
    }


    // 查询所有运动记录
    // 查询所有运动记录
    fun getAllExerciseRecords(): List<String> {
        val db = readableDatabase
        val cursor = db.query(TABLE_EXERCISE_LOG, arrayOf("exercise_name"), null, null, null, null, null)
        val exerciseList = mutableListOf<String>()

        // 获取所有列名
        val columnNames = cursor.columnNames
        println("Column names: ${columnNames.joinToString(", ")}")  // 输出列名，检查是否有 "exercise_name"

        // 获取列索引
        val exerciseNameColumnIndex = cursor.getColumnIndex("exercise_name")

        // 如果列索引返回 -1，说明没有找到该列
        if (exerciseNameColumnIndex == -1) {
            throw Exception("Column 'exercise_name' not found in the database")
        }

        // 遍历查询结果
        while (cursor.moveToNext()) {
            val exerciseName = cursor.getString(exerciseNameColumnIndex)
            exerciseList.add(exerciseName)
        }

        cursor.close()
        return exerciseList
    }


    // 根据运动名称查询 METs 值
    fun getMetsValueForExercise(exerciseName: String): Float {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EXERCISE_LOG,
            arrayOf("mets_value"),
            "exercise_name = ?",
            arrayOf(exerciseName),
            null, null, null
        )

        var metsValue: Float = 0f

        // 获取所有列名并输出
        val columnNames = cursor.columnNames
        println("Column names: ${columnNames.joinToString(", ")}")  // 输出所有列名，确保 "mets_value" 存在

        // 获取列索引
        val metsValueColumnIndex = cursor.getColumnIndex("mets_value")

        // 如果列索引返回 -1，说明没有找到该列
        if (metsValueColumnIndex == -1) {
            throw Exception("Column 'mets_value' not found in the database")
        }

        if (cursor.moveToFirst()) {
            metsValue = cursor.getFloat(metsValueColumnIndex)
        }

        cursor.close()
        return metsValue
    }

}
