package com.example.forhealth.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CustomExerciseDao {

    // 插入自定义运动
    @Insert
    suspend fun insert(customExercise: CustomExercise)

    // 查询所有自定义运动
    @Query("SELECT * FROM custom_exercises")
    suspend fun getAllExercises(): List<CustomExercise>

    // 删除所有自定义运动（可选）
    @Query("DELETE FROM custom_exercises")
    suspend fun deleteAllExercises()
}

