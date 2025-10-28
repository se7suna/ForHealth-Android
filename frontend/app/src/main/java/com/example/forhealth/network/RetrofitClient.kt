package com.example.forhealth.network

import com.example.forhealth.model.TokenResponse

/**
 * 简化的网络客户端模拟类
 * 不使用 Retrofit/OkHttp，直接返回模拟结果
 */
object RetrofitClient {

    var token: String = ""

    val apiService: ApiService = object : ApiService {
        override suspend fun login(params: Map<String, String>): Result<TokenResponse> {
            // 模拟网络请求
            return if (params["email"] == "test@example.com" && params["password"] == "123456") {
                token = "mock_jwt_token"
                Result.success(TokenResponse(access_token = token))
            } else {
                Result.failure(Exception("登录失败"))
            }
        }

        override suspend fun register(params: Map<String, String>): Result<TokenResponse> {
            // 模拟注册成功
            token = "mock_jwt_token"
            return Result.success(TokenResponse(access_token = token))
        }
    }

    fun saveToken(newToken: String) {
        token = newToken
        // 可以加 SharedPreferences 保存逻辑
    }
}
