package com.example.forhealth.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "custom_exercises")  // 定义表格名称
data class CustomExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,  // 自动生成 ID 作为主键
    @ColumnInfo(name = "exercise_name") val name: String,  // 运动名称
    @ColumnInfo(name = "mets_value") val metsValue: Float  // METs 值
)

