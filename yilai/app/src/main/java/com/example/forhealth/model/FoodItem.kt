package com.example.forhealth.model

import android.os.Parcel
import android.os.Parcelable

// 定义 FoodItem 数据类，表示食物条目
data class FoodItem(
    val name: String,       // 食物名称
    val calories: Double,     // 热量（kcal）
    val protein: Double,   // 蛋白质（克）
    val fat: Double,       // 脂肪（克）
    val carbs: Double      // 碳水化合物（克）
) : Parcelable {

    // 用于支持 Parcelable 实现，将对象序列化以便在不同的 Activity 或 Fragment 之间传递
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeDouble(calories)
        parcel.writeDouble(protein)
        parcel.writeDouble(fat)
        parcel.writeDouble(carbs)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FoodItem> {
        override fun createFromParcel(parcel: Parcel): FoodItem {
            return FoodItem(parcel)
        }

        override fun newArray(size: Int): Array<FoodItem?> {
            return arrayOfNulls(size)
        }
    }
}
