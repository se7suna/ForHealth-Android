package com.example.forhealth.network

import android.content.Context
import com.example.forhealth.model.TokenResponse
import com.example.forhealth.model.User
import com.example.forhealth.utils.PrefsHelper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit网络客户端
 * 负责与后端API通信
 */
object RetrofitClient {
    
    // 后端服务器地址
    // Android模拟器访问本机使用 10.0.2.2
    // 真机测试需要使用电脑的局域网IP
    private const val BASE_URL = "http://10.0.2.2:8000/"
    
    private var context: Context? = null
    
    /**
     * 初始化RetrofitClient
     * 必须在Application的onCreate中调用
     */
    fun init(appContext: Context) {
        context = appContext
    }
    
    // 日志拦截器
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    // OkHttp客户端配置
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Gson配置
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()
    
    // Retrofit实例
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    // API接口实例
    private val apiInterface: ApiInterface = retrofit.create(ApiInterface::class.java)
    
    /**
     * 从响应中提取错误信息
     */
    private fun getErrorMessage(response: retrofit2.Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                // 尝试解析JSON格式的错误信息
                try {
                    // 使用TypeToken来正确解析泛型Map
                    val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                    val errorJson = gson.fromJson<Map<String, Any>>(errorBody, type)
                    errorJson["detail"]?.toString() ?: errorJson["message"]?.toString() ?: response.message()
                } catch (e: Exception) {
                    // 如果解析失败，直接返回原始错误体
                    errorBody
                }
            } else {
                response.message() ?: "未知错误"
            } ?: "请求失败 (${response.code()})"
        } catch (e: Exception) {
            response.message() ?: "网络错误"
        }
    }
    
    /**
     * ApiService实现
     * 将Retrofit接口封装为Result类型
     */
    val apiService: ApiService = object : ApiService {
        
        override suspend fun login(params: Map<String, String>): Result<TokenResponse> {
            return try {
                val response = apiInterface.login(params)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = getErrorMessage(response)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
        
        override suspend fun register(params: Map<String, String>): Result<TokenResponse> {
            return try {
                val response = apiInterface.register(params)
                if (response.isSuccessful) {
                    // 注册成功后，调用登录接口获取Token
                    login(params)
                } else {
                    val errorMsg = getErrorMessage(response)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
        
        override suspend fun getProfile(): Result<User> {
            return try {
                val ctx = context ?: return Result.failure(Exception("未初始化"))
                val token = PrefsHelper.getToken(ctx)
                if (token.isEmpty()) {
                    return Result.failure(Exception("未登录，请先登录"))
                }
                
                val response = apiInterface.getProfile("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    when (response.code()) {
                        401 -> Result.failure(Exception("登录已过期，请重新登录"))
                        else -> {
                            val errorMsg = getErrorMessage(response)
                            Result.failure(Exception(errorMsg))
                        }
                    }
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
        
        override suspend fun updateProfile(params: Map<String, Any>): Result<User> {
            return try {
                val ctx = context ?: return Result.failure(Exception("未初始化"))
                val token = PrefsHelper.getToken(ctx)
                if (token.isEmpty()) {
                    return Result.failure(Exception("未登录，请先登录"))
                }
                
                // 将Map转换为JSON字符串，然后创建RequestBody
                val json = gson.toJson(params)
                android.util.Log.d("RetrofitClient", "更新用户资料 - 请求数据: $json")
                val requestBody = json.toRequestBody("application/json".toMediaType())
                
                val response = apiInterface.updateProfile("Bearer $token", requestBody)
                if (response.isSuccessful && response.body() != null) {
                    android.util.Log.d("RetrofitClient", "更新成功")
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = getErrorMessage(response)
                    android.util.Log.e("RetrofitClient", "更新失败 - HTTP ${response.code()}: $errorMsg")
                    when (response.code()) {
                        401 -> Result.failure(Exception("登录已过期，请重新登录"))
                        400 -> {
                            Result.failure(Exception("数据格式错误: $errorMsg"))
                        }
                        500 -> {
                            Result.failure(Exception("服务器错误: $errorMsg"))
                        }
                        else -> {
                            Result.failure(Exception(errorMsg))
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RetrofitClient", "更新异常", e)
                Result.failure(Exception("网络错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }
    /**
     * 保存Token到SharedPreferences
     */
    fun saveToken(newToken: String) {
        context?.let { ctx ->
            PrefsHelper.saveToken(ctx, newToken)
        }
    }
}
