package com.example.forhealth.network

import com.example.forhealth.model.TokenResponse
import com.example.forhealth.model.User

interface ApiService {

    /**
     * 登录接口
     * @param params mapOf("email" to email, "password" to password)
     */
    suspend fun login(params: Map<String, String>): Result<TokenResponse>

    /**
     * 注册接口
     * @param params mapOf("email" to email, "password" to password)
     */
    suspend fun register(params: Map<String, String>): Result<TokenResponse>

    /**
     * 获取用户资料
     */
    suspend fun getProfile(): Result<User>

    /**
     * 更新用户资料
     * @param params 用户数据（所有字段可选）
     */
    suspend fun updateProfile(params: Map<String, Any>): Result<User>
}
