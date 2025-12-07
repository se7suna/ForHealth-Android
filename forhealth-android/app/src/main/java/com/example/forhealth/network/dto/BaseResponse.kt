package com.example.forhealth.network.dto

/**
 * 后端API统一响应格式
 * 根据实际后端响应格式调整
 */
data class BaseResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null
) {
    fun isSuccess(): Boolean = code == 200 || code == 0 // 根据后端实际成功码调整
}

/**
 * 如果后端没有统一响应格式，可以直接使用数据模型
 * 这里提供两种方案：
 * 1. 使用BaseResponse包装（推荐，统一错误处理）
 * 2. 直接返回数据模型（简单，但需要单独处理错误）
 */

