package com.example.forhealth.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit客户端配置
 */
object RetrofitClient {
    
    // TODO: 从配置文件或环境变量读取
    private const val BASE_URL = "http://124.70.161.90:8000/api/"
    
    // Token提供者（从SharedPreferences或其他存储中获取）
    private var tokenProvider: (() -> String?)? = null
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // 开发环境使用，生产环境改为NONE
    }
    
    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        
        // 添加认证拦截器
        tokenProvider?.let {
            builder.addInterceptor(AuthInterceptor(it))
        }
        
        return builder.build()
    }
    
    @Volatile
    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(createOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    @Volatile
    var apiService: ApiService = retrofit.create(ApiService::class.java)
        private set
    
    /**
     * 设置Token提供者并重新创建Retrofit实例
     */
    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
        // 重新创建Retrofit实例以应用新的拦截器
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }
    
    /**
     * 更新Base URL并重新创建Retrofit实例
     */
    fun updateBaseUrl(baseUrl: String) {
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }
}

