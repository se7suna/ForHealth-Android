package com.example.forhealth.network

import com.example.forhealth.network.dto.ErrorResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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
 * 改进的错误处理：尝试解析响应体中的错误详情
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
            val errorMessage = parseErrorResponse(response) ?: response.message() ?: "Unknown error"
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
 * 解析错误响应体，提取 detail 或 message 字段
 */
private fun <T> parseErrorResponse(response: Response<T>): String? {
    return try {
        val errorBody = response.errorBody()?.string()
        if (errorBody != null) {
            val gson = Gson()
            val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
            errorResponse.getErrorMessage()
        } else {
            null
        }
    } catch (e: JsonSyntaxException) {
        // 如果解析失败，返回 null，使用默认错误消息
        null
    } catch (e: Exception) {
        null
    }
}

