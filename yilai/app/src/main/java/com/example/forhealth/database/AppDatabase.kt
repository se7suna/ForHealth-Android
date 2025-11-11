package com.example.forhealth.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CustomExercise::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customExerciseDao(): CustomExerciseDao  // 获取 DAO
}
