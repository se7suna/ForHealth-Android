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
 * RetrofitClient —— 已支持测试账号免后端逻辑
 */
object RetrofitClient {

    // 测试账户
    private const val TEST_EMAIL = "test@example.com"
    private const val TEST_PASSWORD = "123456"

    // Fake API Service（专为测试用户使用）
    private val fakeApiService = FakeApiService()

    // 后端服务器地址
    private const val BASE_URL = "http://124.70.161.90:8000"

    private var context: Context? = null

    fun init(appContext: Context) {
        context = appContext
    }

    // 日志拦截器
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttp
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Gson
    private val gson: Gson = GsonBuilder().setLenient().create()

    // Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val apiInterface: ApiInterface = retrofit.create(ApiInterface::class.java)

    /**
     * 直接访问 API 接口（用于新的食物功能）
     */
    val api: ApiInterface = apiInterface

    /**
     * 从错误响应中提取信息
     */
    private fun getErrorMessage(response: retrofit2.Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                try {
                    val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                    val errorJson = gson.fromJson<Map<String, Any>>(errorBody, type)
                    errorJson["detail"]?.toString()
                        ?: errorJson["message"]?.toString()
                        ?: response.message()
                } catch (_: Exception) {
                    errorBody
                }
            } else {
                response.message() ?: "未知错误"
            } ?: "请求失败 (${response.code()})"
        } catch (e: Exception) {
            response.message() ?: "网络错误"
        }
    }


    // ===========================
    //     ApiService 封装层
    // ===========================
    val apiService: ApiService = object : ApiService {

        /**
         * 登录
         * —— 登录前检查用户名 + 密码是否是测试账号
         * —— 若是测试账号，全部返回 fakeApiService
         */
        override suspend fun login(params: Map<String, String>): Result<TokenResponse> {
            val email = params["email"]
            val password = params["password"]

            // ⭐⭐⭐ 测试账号逻辑（永远不访问后端）
            if (email == TEST_EMAIL && password == TEST_PASSWORD) {
                return fakeApiService.login(params)
            }

            // 正常账号 -> 真正访问后端
            return try {
                val response = apiInterface.login(params)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception(getErrorMessage(response)))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message}"))
            }
        }

        /**
         * 注册
         * —— 测试账号注册直接成功，不访问后端
         */
        override suspend fun register(params: Map<String, String>): Result<TokenResponse> {
            val email = params["email"]
            val password = params["password"]

            if (email == TEST_EMAIL && password == TEST_PASSWORD) {
                return fakeApiService.register(params)
            }

            return try {
                val response = apiInterface.register(params)
                if (response.isSuccessful) {
                    login(params)
                } else {
                    Result.failure(Exception(getErrorMessage(response)))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message}"))
            }
        }

        /**
         * 获取用户资料
         * —— 如果 token 是 FAKE_TEST_TOKEN，则直接使用 fakeApiService，不访问后端
         */
        override suspend fun getProfile(): Result<User> {
            val ctx = context ?: return Result.failure(Exception("未初始化"))
            val token = PrefsHelper.getToken(ctx)

            // ⭐⭐⭐ token 判断 —— 若为测试 token，直接返回本地假用户资料
            if (token == "FAKE_TEST_TOKEN") {
                return fakeApiService.getProfile()
            }

            if (token.isEmpty()) {
                return Result.failure(Exception("未登录"))
            }

            return try {
                val response = apiInterface.getProfile("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception(getErrorMessage(response)))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message}"))
            }
        }

        /**
         * 更新资料
         * —— 若 token 是 FAKE_TEST_TOKEN，全部返回本地成功
         */
        override suspend fun updateProfile(params: Map<String, Any>): Result<User> {
            val ctx = context ?: return Result.failure(Exception("未初始化"))
            val token = PrefsHelper.getToken(ctx)

            // ⭐⭐⭐ 测试账号：不访问后端
            if (token == "FAKE_TEST_TOKEN") {
                return fakeApiService.updateProfile(params)
            }

            if (token.isEmpty()) {
                return Result.failure(Exception("未登录"))
            }

            return try {
                val json = gson.toJson(params)
                val requestBody = json.toRequestBody("application/json".toMediaType())

                val response = apiInterface.updateProfile("Bearer $token", requestBody)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception(getErrorMessage(response)))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message}"))
            }
        }
    }


    /**
     * 保存 token
     */
    fun saveToken(newToken: String) {
        context?.let { PrefsHelper.saveToken(it, newToken) }
    }
}
