package com.example.forhealth.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FOOD_NAME TEXT UNIQUE,
                $COLUMN_CALORIES REAL,
                $COLUMN_PORTION REAL,
                $COLUMN_TIMESTAMP INTEGER
            );
        """
        db.execSQL(createTable)

        // 插入一些预定义的食物数据
        insertInitialFoodData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // 插入初始数据
    private fun insertInitialFoodData(db: SQLiteDatabase) {
        val foodData = listOf(
            Pair("苹果", 52.0),
            Pair("香蕉", 89.0),
            Pair("米饭", 130.0),
            Pair("鸡胸肉", 165.0)
        )

        for (food in foodData) {
            val values = ContentValues().apply {
                put(COLUMN_FOOD_NAME, food.first)
                put(COLUMN_CALORIES, food.second)
            }
            db.insert(TABLE_NAME, null, values)
        }
    }

    // 插入用户自定义的食物数据
    fun insertFoodRecord(foodName: String, portion: Double, totalCalories: Double, timestamp: Long): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FOOD_NAME, foodName)
            put(COLUMN_CALORIES, totalCalories)
            put(COLUMN_PORTION, portion)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    // 更新饮食记录
    fun updateFoodRecord(id: Long, foodName: String, portion: Double, totalCalories: Double): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FOOD_NAME, foodName)
            put(COLUMN_PORTION, portion)
            put(COLUMN_CALORIES, totalCalories)
        }
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())
        return db.update(TABLE_NAME, values, whereClause, whereArgs)
    }

    // 删除饮食记录
    fun deleteFoodRecord(id: Long): Int {
        val db = writableDatabase
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())
        return db.delete(TABLE_NAME, whereClause, whereArgs)
    }

    // 获取食物的卡路里
    fun getFoodCalories(foodName: String): Double? {
        val db = readableDatabase
        val query = "SELECT $COLUMN_CALORIES FROM $TABLE_NAME WHERE $COLUMN_FOOD_NAME = ?"
        val cursor = db.rawQuery(query, arrayOf(foodName))

        // 打印所有列名以进行调试
        val columnCount = cursor.columnCount
        for (i in 0 until columnCount) {
            println("Column ${i + 1}: ${cursor.getColumnName(i)}")  // 打印列名
        }

        return if (cursor.moveToFirst()) {
            // 获取 COLUMN_CALORIES 的索引
            val caloriesColumnIndex = cursor.getColumnIndex(COLUMN_CALORIES)
            if (caloriesColumnIndex != -1) {
                // 正常获取卡路里值
                cursor.getDouble(caloriesColumnIndex)
            } else {
                println("Column '$COLUMN_CALORIES' not found!")
                null  // 如果没有找到列，则返回 null
            }
        } else {
            null  // 如果查询没有返回数据
        }
    }

    companion object {
        private const val DATABASE_NAME = "food_log.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "food_records"
        const val COLUMN_ID = "id"
        const val COLUMN_FOOD_NAME = "food_name"
        const val COLUMN_CALORIES = "calories"
        const val COLUMN_PORTION = "portion"
        const val COLUMN_TIMESTAMP = "timestamp"
    }
}
