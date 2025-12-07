package com.example.forhealth.repositories

import android.net.Uri
import com.example.forhealth.models.FoodItem
import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.food.FoodResponse
import com.example.forhealth.network.safeApiCall
import com.example.forhealth.utils.Constants
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * 食物数据仓库
 * 负责管理食物数据的获取（本地数据库 + 后端API）
 */
class FoodRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * 获取食物列表
     * 优先从本地数据库获取，如果没有则从API获取
     */
    suspend fun getFoods(): ApiResult<List<FoodItem>> {
        // TODO: 1. 先从本地数据库查询
        // TODO: 2. 如果本地没有，从API获取
        // TODO: 3. 保存到本地数据库
        
        // 临时返回本地常量数据
        return ApiResult.Success(Constants.FOOD_DB)
    }
    
    /**
     * 搜索食物
     */
    suspend fun searchFoods(query: String): ApiResult<List<FoodItem>> {
        // TODO: 调用后端搜索API
        // 临时使用本地搜索
        val filtered = Constants.FOOD_DB.filter {
            it.name.contains(query, ignoreCase = true)
        }
        return ApiResult.Success(filtered)
    }
    
    /**
     * 创建自定义食物
     */
    suspend fun createCustomFood(food: FoodItem, imageUri: Uri? = null): ApiResult<FoodItem> {
        return safeApiCall {
            // 准备 Multipart 参数（使用 @Part 注解，需要传递 String 类型）
            val namePart = food.name
            val servingSizePart = food.gramsPerUnit
            val caloriesPart = food.calories
            val proteinPart = food.protein
            val carbohydratesPart = food.carbs
            val fatPart = food.fat
            val servingUnitPart = food.unit
            
            // 处理图片
            val imagePart: MultipartBody.Part? = if (imageUri != null) {
                val file = File(imageUri.path ?: "")
                if (file.exists()) {
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("image", file.name, requestFile)
                } else {
                    null
                }
            } else {
                null
            }
            
            // 调用API，使用 Multipart 格式
            apiService.createFood(
                name = namePart,
                servingSize = servingSizePart,
                calories = caloriesPart,
                protein = proteinPart,
                carbohydrates = carbohydratesPart,
                fat = fatPart,
                servingUnit = servingUnitPart,
                image = imagePart
            )
        }.let { result ->
            // 将 FoodResponse 转换为 FoodItem
            when (result) {
                is ApiResult.Success -> {
                    val foodResponse = result.data
                    val savedFood = FoodItem(
                        id = foodResponse.id,
                        name = foodResponse.name,
                        calories = foodResponse.nutrition_per_serving.calories,
                        protein = foodResponse.nutrition_per_serving.protein,
                        carbs = foodResponse.nutrition_per_serving.carbohydrates,
                        fat = foodResponse.nutrition_per_serving.fat,
                        unit = foodResponse.serving_unit,
                        gramsPerUnit = foodResponse.serving_size,
                        image = foodResponse.image_url ?: food.image
                    )
                    ApiResult.Success(savedFood)
                }
                is ApiResult.Error -> result
                is ApiResult.Loading -> result
            }
        }
    }
}

