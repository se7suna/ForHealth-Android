package com.example.forhealth.network

import com.example.forhealth.model.MessageResponse
import com.example.forhealth.model.TokenResponse
import com.example.forhealth.model.User
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API接口定义
 * 定义所有与后端通信的HTTP接口
 */
interface ApiInterface {
    
    /**
     * 用户登录
     * POST /api/auth/login
     */
    @POST("api/auth/login")
    suspend fun login(@Body params: Map<String, String>): Response<TokenResponse>
    
    /**
     * 用户注册
     * POST /api/auth/register
     * 注意：后端返回MessageResponse而不是TokenResponse
     */
    @POST("api/auth/register")
    suspend fun register(@Body params: Map<String, String>): Response<MessageResponse>
    
    /**
     * 获取用户资料
     * GET /api/user/profile
     */
    @GET("api/user/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<User>
    
    /**
     * 更新用户资料
     * PUT /api/user/profile
     * 使用RequestBody来避免泛型类型问题
     */
    @PUT("api/user/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body body: RequestBody
    ): Response<User>
}


