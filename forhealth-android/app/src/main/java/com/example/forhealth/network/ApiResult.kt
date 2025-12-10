package com.example.forhealth.network

import com.example.forhealth.network.dto.auth.ErrorResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * API调用结果封装
 * 用于统一处理成功和失败情况
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

/**
 * 扩展函数：将Retrofit Response转换为ApiResult
 */
suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Response body is null")
            }
        } else {
            // 尝试解析错误响应体
            val errorMessage = parseErrorResponse(response.errorBody())
                ?: response.message()
                ?: "Unknown error (${response.code()})"
            
            ApiResult.Error(
                message = errorMessage,
                code = response.code()
            )
        }
    } catch (e: Exception) {
        ApiResult.Error(
            message = e.message ?: "Network error occurred",
            code = null
        )
    }
}

/**
 * 解析错误响应体
 * 支持 FastAPI 标准错误格式: {"detail": "错误信息"}
 */
private fun parseErrorResponse(errorBody: ResponseBody?): String? {
    if (errorBody == null) return null
    
    return try {
        val errorString = errorBody.string()
        val gson = Gson()
        
        // 尝试解析为 ErrorResponse 格式 {"detail": "..."}
        try {
            val errorResponse = gson.fromJson(errorString, ErrorResponse::class.java)
            errorResponse.detail
        } catch (e: JsonSyntaxException) {
            // 如果不是 JSON 格式，返回原始字符串
            errorString.takeIf { it.isNotBlank() }
        }
    } catch (e: Exception) {
        null
    }
}

