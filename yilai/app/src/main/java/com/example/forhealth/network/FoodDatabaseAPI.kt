package com.example.forhealth.network

import com.example.forhealth.model.FoodItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

object FoodDatabaseAPI {

    // Retrofit配置，直接在这里设置
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://your-api-endpoint.com/") // 替换为您的API基础URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // 创建FoodApiService实例
    private val apiService: FoodApiService = retrofit.create(FoodApiService::class.java)

    /**
     * 获取食物数据
     */
    fun getFoodItems(query: String, callback: FoodDataCallback) {
        // 调用API服务获取食物数据
        val call = apiService.searchFoodItems(query)

        // 异步请求
        call.enqueue(object : Callback<List<FoodItem>> {
            override fun onResponse(call: Call<List<FoodItem>>, response: Response<List<FoodItem>>) {
                if (response.isSuccessful) {
                    // 请求成功，返回数据
                    callback.onSuccess(response.body() ?: emptyList())
                } else {
                    // 请求失败，返回错误信息
                    callback.onError("请求失败: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<FoodItem>>, t: Throwable) {
                // 网络请求失败，返回错误信息
                callback.onError("网络请求失败: ${t.message}")
            }
        })
    }

    // 定义回调接口
    interface FoodDataCallback {
        fun onSuccess(foodItems: List<FoodItem>)
        fun onError(error: String)
    }

    // FoodApiService接口，定义API请求
    interface FoodApiService {
        @GET("food/search")
        fun searchFoodItems(@Query("query") query: String): Call<List<FoodItem>>
    }
}

