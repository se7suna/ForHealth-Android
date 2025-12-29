package com.example.forhealth.repositories

import android.net.Uri
import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.dto.ai.FoodRecognitionConfirmResponse
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.food.*
import com.example.forhealth.network.safeApiCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * 食物数据仓库
 * 负责管理食物数据的获取（只返回DTO，不进
 */
class FoodRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * 搜索食物（薄荷健康数据库）
     * @param keyword 搜索关键词
     * @param page 页码（默认1）
     * @param simplified 是否返回简化版本（默认false）
     */
    suspend fun searchFoods(
        keyword: String? = null,
        page: Int = 1,
        simplified: Boolean = false
    ): ApiResult<Any> {
        return safeApiCall {
            apiService.searchFoods(
                keyword = keyword,
                page = page,
                includeFullNutrition = !simplified,
                simplified = simplified
            )
        }
    }

    /**
     * 通过食物名称搜索本地数据库（仅返回ID和名称）
     */
    suspend fun searchFoodById(
        keyword: String,
        limit: Int = 20
    ): ApiResult<FoodIdSearchResponse> {
        return safeApiCall {
            apiService.searchFoodById(keyword, limit)
        }
    }

    /**
     * 根据ID获取食物详情
     */
    suspend fun getFood(foodId: String): ApiResult<FoodResponse> {
        return safeApiCall {
            apiService.getFood(foodId)
        }
    }

    /**
     * 创建自定义食物
     */
    suspend fun createFood(
        name: String,
        servingSize: Double,
        calories: Double,
        protein: Double,
        carbohydrates: Double,
        fat: Double,
        category: String? = null,
        servingUnit: String = "克",
        brand: String? = null,
        barcode: String? = null,
        fiber: Double? = null,
        sugar: Double? = null,
        sodium: Double? = null,
        imageUri: Uri? = null
    ): ApiResult<FoodResponse> {
        return safeApiCall {
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
                name = name,
                servingSize = servingSize,
                calories = calories,
                protein = protein,
                carbohydrates = carbohydrates,
                fat = fat,
                category = category,
                servingUnit = servingUnit,
                brand = brand,
                barcode = barcode,
                fiber = fiber,
                sugar = sugar,
                sodium = sodium,
                image = imagePart
            )
        }
    }

    /**
     * 更新食物信息
     */
    suspend fun updateFood(
        foodId: String,
        request: FoodUpdateRequest
    ): ApiResult<FoodResponse> {
        return safeApiCall {
            apiService.updateFood(foodId, request)
        }
    }

    /**
     * 删除食物
     */
    suspend fun deleteFood(foodId: String): ApiResult<com.example.forhealth.network.dto.auth.MessageResponse> {
        return safeApiCall {
            apiService.deleteFood(foodId)
        }
    }

    /**
     * 更新食物图片
     */
    suspend fun updateFoodImage(
        foodId: String,
        imageUri: Uri
    ): ApiResult<FoodResponse> {
        val file = File(imageUri.path ?: "")
        if (!file.exists()) {
            return ApiResult.Error("图片文件不存在")
        }
        return safeApiCall {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
            apiService.updateFoodImage(foodId, imagePart)
        }
    }

    /**
     * 创建食物记录
     */
    suspend fun createFoodRecord(
        request: FoodRecordCreateRequest
    ): ApiResult<FoodRecordResponse> {
        return safeApiCall {
            apiService.createFoodRecord(request)
        }
    }

    /**
     * 获取食物记录列表
     */
    suspend fun getFoodRecords(
        startDate: String? = null,
        endDate: String? = null,
        mealType: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): ApiResult<FoodRecordListResponse> {
        return safeApiCall {
            apiService.getFoodRecords(startDate, endDate, mealType, limit, offset)
        }
    }

    /**
     * 获取某日的营养摘要
     */
    suspend fun getDailyNutrition(
        targetDate: String
    ): ApiResult<DailyNutritionSummary> {
        return safeApiCall {
            apiService.getDailyNutrition(targetDate)
        }
    }

    /**
     * 更新食物记录
     */
    suspend fun updateFoodRecord(
        recordId: String,
        request: FoodRecordUpdateRequest
    ): ApiResult<FoodRecordResponse> {
        return safeApiCall {
            apiService.updateFoodRecord(recordId, request)
        }
    }

    /**
     * 删除食物记录
     */
    suspend fun deleteFoodRecord(
        recordId: String
    ): ApiResult<com.example.forhealth.network.dto.auth.MessageResponse> {
        return safeApiCall {
            apiService.deleteFoodRecord(recordId)
        }
    }

    /**
     * 从图片识别条形码
     */
    suspend fun recognizeBarcode(
        imageUri: Uri
    ): ApiResult<BarcodeImageRecognitionResponse> {
        val file = File(imageUri.path ?: "")
        if (!file.exists()) {
            return ApiResult.Error("图片文件不存在")
        }
        return safeApiCall {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            apiService.recognizeBarcode(imagePart)
        }
    }

    /**
     * 扫描条形码查询食品信息
     */
    suspend fun scanBarcode(
        barcode: String
    ): ApiResult<BarcodeScanResponse> {
        return safeApiCall {
            apiService.scanBarcode(barcode)
        }
    }

    /**
     * 拍照识别食品信息
     */
    suspend fun recognizeFood(
        imageFile: File,
        mealType: String? = null,
        notes: String? = null,
        recordedAt: String? = null
    ): ApiResult<FoodRecognitionConfirmResponse> {
        val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)
        return safeApiCall {
            apiService.recognizeFood(
                file = body,
                mealType = mealType,
                notes = notes,
                recordedAt = recordedAt
            )
        }
    }
}

