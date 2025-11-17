package com.example.forhealth.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.forhealth.model.FoodItem
import android.util.Log

class CustomFoodDatabaseHelper(context: Context) : SQLiteOpenHelper(context, CUSTOM_FOOD_DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $CUSTOM_FOOD_TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FOOD_NAME TEXT UNIQUE,
                $COLUMN_QUANTITY TEXT,
                $COLUMN_CALORIES REAL,
                $COLUMN_PROTEIN REAL,
                $COLUMN_FAT REAL,
                $COLUMN_CARBS REAL
            );
        """
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $CUSTOM_FOOD_TABLE_NAME")
        onCreate(db)
    }

    // 插入自定义食物数据
    fun insertCustomFood(name: String, quantity: String, calories: Double, protein: Double, fat: Double, carbs: Double): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FOOD_NAME, name)
            put(COLUMN_QUANTITY, quantity)
            put(COLUMN_CALORIES, calories)
            put(COLUMN_PROTEIN, protein)
            put(COLUMN_FAT, fat)
            put(COLUMN_CARBS, carbs)
        }
        return db.insert(CUSTOM_FOOD_TABLE_NAME, null, values)
    }

    // 获取自定义食物卡路里
    fun getCustomFoodCalories(foodName: String): Double? {
        val db = readableDatabase
        val query = "SELECT $COLUMN_CALORIES FROM $CUSTOM_FOOD_TABLE_NAME WHERE $COLUMN_FOOD_NAME = ?"
        val cursor = db.rawQuery(query, arrayOf(foodName))

        return if (cursor.moveToFirst()) {
            val caloriesColumnIndex = cursor.getColumnIndex(COLUMN_CALORIES)
            if (caloriesColumnIndex != -1) {
                cursor.getDouble(caloriesColumnIndex)
            } else {
                null
            }
        } else {
            null
        }
    }


    // 获取完整的食物信息（包括卡路里、蛋白质、脂肪和碳水化合物）
    fun getFoodRecipeByName(foodName: String): FoodItem? {
        val db = readableDatabase
        val query = "SELECT * FROM $CUSTOM_FOOD_TABLE_NAME WHERE $COLUMN_FOOD_NAME = ?"
        val cursor = db.rawQuery(query, arrayOf(foodName))

        return if (cursor.moveToFirst()) {
            try {
                // 确保获取的字段是 Double 类型
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOOD_NAME))
                val calories = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_CALORIES)) // 获取卡路里
                val protein = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PROTEIN)) // 获取蛋白质
                val fat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_FAT)) // 获取脂肪
                val carbs = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_CARBS)) // 获取碳水化合物

                // 返回 FoodItem 对象
                FoodItem(name, calories, protein, fat, carbs)
            } catch (e: Exception) {
                Log.e("DatabaseError", "Error retrieving columns: ${e.message}")
                null
            } finally {
                cursor.close()
            }
        } else {
            cursor.close()
            null
        }
    }



    companion object {
        private const val DATABASE_VERSION = 1
        private const val CUSTOM_FOOD_DATABASE_NAME = "custom_food.db"
        public const val CUSTOM_FOOD_TABLE_NAME = "custom_food_records"
        const val COLUMN_ID = "id"
        const val COLUMN_FOOD_NAME = "food_name"
        const val COLUMN_QUANTITY = "quantity"
        const val COLUMN_CALORIES = "calories"
        const val COLUMN_PROTEIN = "protein"
        const val COLUMN_FAT = "fat"
        const val COLUMN_CARBS = "carbs"
    }
}
