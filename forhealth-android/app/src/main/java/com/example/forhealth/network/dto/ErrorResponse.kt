package com.example.forhealth.network.dto

import com.google.gson.annotations.SerializedName

/**
 * FastAPI 标准错误响应格式
 * 用于解析后端返回的错误信息
 */
data class ErrorResponse(
    @SerializedName("detail")
    val detail: String? = null,
    @SerializedName("message")
    val message: String? = null
) {
    /**
     * 获取错误消息，优先使用 detail，其次使用 message
     */
    fun getErrorMessage(): String {
        return detail ?: message ?: "Unknown error"
    }
}

