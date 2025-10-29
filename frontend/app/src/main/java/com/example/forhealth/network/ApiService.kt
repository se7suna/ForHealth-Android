package com.example.forhealth.network

import com.example.forhealth.model.TokenResponse

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

    // 后续可以增加身体数据、活动水平、健康目标上传接口
}
