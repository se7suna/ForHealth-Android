package com.example.forhealth.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor

class ExerciseSearchDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "sports_activities.db"
        const val DATABASE_VERSION = 1

        const val TABLE_SPORTS = "sports"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_SPORTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_name TEXT NOT NULL,
                recommendation TEXT,
                mets_value REAL NOT NULL
            )
        """
        db.execSQL(createTableSQL)

        // 插入静态数据
        val sampleData = listOf(
            Pair("Running", Pair("Suitable for beginners, aerobic exercise", 7.0f)),
            Pair("Cycling", Pair("Good for cardio, moderate intensity", 6.0f)),
            Pair("Swimming", Pair("Full-body workout, high intensity", 8.0f)),
            Pair("Jumping Rope", Pair("High-intensity interval training", 12.0f)),
            Pair("Walking", Pair("Low intensity, suitable for all levels", 3.8f)),
            Pair("Yoga", Pair("Great for flexibility, low intensity", 2.5f)),
            Pair("Basketball", Pair("Team sport, moderate to high intensity", 6.5f)),
            Pair("Tennis", Pair("Great for endurance, moderate intensity", 7.3f)),
            Pair("Hiking", Pair("Outdoor exercise, moderate intensity", 5.0f)),
            Pair("Weightlifting", Pair("Strength training, moderate intensity", 4.5f)),
            Pair("Rowing", Pair("Full-body workout, moderate intensity", 6.5f)),
            Pair("Running on Treadmill", Pair("Indoor cardio workout, moderate intensity", 7.5f)),
            Pair("Soccer", Pair("Team sport, high intensity", 8.0f)),
            Pair("CrossFit", Pair("Intense full-body workout", 10.0f)),
            Pair("Boxing", Pair("High-intensity training, great for strength and cardio", 9.0f)),
            Pair("Dancing", Pair("Fun, moderate to high intensity", 6.0f)),
            Pair("Rock Climbing", Pair("Strength and endurance training", 7.5f)),
            Pair("Skiing", Pair("Outdoor activity, moderate to high intensity", 8.5f)),
            Pair("Golf", Pair("Low intensity, good for walking", 3.5f)),
            Pair("Martial Arts", Pair("High intensity, strength and endurance", 8.0f))
        )

        val insertSQL = "INSERT INTO $TABLE_SPORTS (exercise_name, recommendation, mets_value) VALUES (?, ?, ?)"
        val stmt = db.compileStatement(insertSQL)

        for (data in sampleData) {
            val exerciseName = data.first
            val recommendation = data.second.first
            val metsValue = data.second.second

            stmt.bindString(1, exerciseName)
            stmt.bindString(2, recommendation)
            stmt.bindDouble(3, metsValue.toDouble())
            stmt.execute()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SPORTS")
        onCreate(db)
    }

    // 通过关键词搜索运动
    fun searchExercisesByKeyword(keyword: String): List<String> {
        val db = readableDatabase
        val query = "SELECT exercise_name, recommendation, mets_value FROM $TABLE_SPORTS WHERE exercise_name LIKE ?"
        val cursor = db.rawQuery(query, arrayOf("%$keyword%"))

        val resultList = mutableListOf<String>()

        // 输出所有列名，调试用
        val columnNames = cursor.columnNames
        println("Column names: ${columnNames.joinToString(", ")}")  // 打印列名，确保列存在

        // 获取列索引并检查
        val exerciseNameColumnIndex = cursor.getColumnIndex("exercise_name")
        val recommendationColumnIndex = cursor.getColumnIndex("recommendation")
        val metsValueColumnIndex = cursor.getColumnIndex("mets_value")

        // 如果返回的列索引为 -1，说明列名不匹配
        if (exerciseNameColumnIndex == -1 || recommendationColumnIndex == -1 || metsValueColumnIndex == -1) {
            throw Exception("One or more columns not found in the database")
        }

        // 获取数据并添加到结果列表
        while (cursor.moveToNext()) {
            val exerciseName = cursor.getString(exerciseNameColumnIndex)
            val recommendation = cursor.getString(recommendationColumnIndex)
            val metsValue = cursor.getFloat(metsValueColumnIndex)

            resultList.add("$exerciseName\nRecommendation: $recommendation\nMETs: $metsValue")
        }

        cursor.close()
        return resultList
    }

}

