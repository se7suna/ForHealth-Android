package com.example.forhealth.model

import com.google.gson.annotations.SerializedName

/**
 * 通用消息响应模型
 * 用于注册等返回消息的接口
 */
data class MessageResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: Map<String, Any>? = null
)


