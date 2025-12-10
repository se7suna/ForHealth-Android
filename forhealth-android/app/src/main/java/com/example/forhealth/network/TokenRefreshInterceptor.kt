package com.example.forhealth.network

import android.content.Context
import com.example.forhealth.network.dto.auth.RefreshTokenRequest
import com.example.forhealth.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Token刷新拦截器
 * 当收到401响应时，自动尝试刷新token
 */
class TokenRefreshInterceptor(
    private val context: Context,
    private val baseUrl: String
) : Interceptor {
    
    private val refreshRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val refreshApiService: ApiService by lazy {
        refreshRetrofit.create(ApiService::class.java)
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var response = chain.proceed(originalRequest)
        
        // 如果收到401未授权错误，尝试刷新token
        if (response.code == 401) {
            val refreshToken = TokenManager.getRefreshToken(context)
            
            if (refreshToken != null && refreshToken.isNotEmpty()) {
                try {
                    // 使用 runBlocking 同步刷新token（在拦截器中需要同步等待）
                    val refreshResponse = runBlocking {
                        refreshApiService.refreshToken(
                            RefreshTokenRequest(refresh_token = refreshToken)
                        )
                    }
                    
                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val tokenResponse = refreshResponse.body()!!
                        // 保存新的token
                        TokenManager.saveTokens(
                            context,
                            tokenResponse.access_token,
                            tokenResponse.refresh_token
                        )
                        
                        // 使用新的token重试原始请求
                        val newToken = tokenResponse.access_token
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                        
                        // 关闭旧的响应
                        response.close()
                        
                        // 重试请求
                        response = chain.proceed(newRequest)
                    } else {
                        // 刷新失败，清除token，用户需要重新登录
                        TokenManager.clearTokens(context)
                    }
                } catch (e: Exception) {
                    // 刷新失败，清除token
                    TokenManager.clearTokens(context)
                }
            } else {
                // 没有refresh token，清除token
                TokenManager.clearTokens(context)
            }
        }
        
        return response
    }
}

