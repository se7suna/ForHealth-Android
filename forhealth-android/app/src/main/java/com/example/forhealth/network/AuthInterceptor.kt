package com.example.forhealth.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 认证拦截器
 * 自动在请求头中添加 Bearer Token
 */
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val token = tokenProvider()
        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(newRequest)
    }
}

