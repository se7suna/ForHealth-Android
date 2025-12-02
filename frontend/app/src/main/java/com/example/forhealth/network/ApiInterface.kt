package com.example.forhealth.network

import com.example.forhealth.model.*
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
    
    // ========== 食物管理接口 ==========
    
    /**
     * 搜索食物（简化版）
     * GET /api/food/search
     */
    @GET("api/food/search")
    suspend fun searchFoods(
        @Header("Authorization") token: String,
        @Query("keyword") keyword: String?,
        @Query("page") page: Int = 1,
        @Query("simplified") simplified: Boolean = true
    ): Response<SimplifiedFoodListResponse>
    
    /**
     * 创建自定义食物
     * POST /api/food
     */
    @POST("api/food")
    suspend fun createFood(
        @Header("Authorization") token: String,
        @Body request: FoodCreateRequest
    ): Response<FoodResponse>
    
    /**
     * 获取每日营养摘要
     * GET /api/food/record/daily/{date}
     */
    @GET("api/food/record/daily/{date}")
    suspend fun getDailyNutrition(
        @Header("Authorization") token: String,
        @Path("date") date: String
    ): Response<DailyNutritionSummary>
    
    /**
     * 创建食物记录
     * POST /api/food/record
     */
    @POST("api/food/record")
    suspend fun createFoodRecord(
        @Header("Authorization") token: String,
        @Body request: CreateFoodRecordRequest
    ): Response<FoodRecordResponse>
    
    /**
     * 删除食物记录
     * DELETE /api/food/record/{record_id}
     */
    @DELETE("api/food/record/{record_id}")
    suspend fun deleteFoodRecord(
        @Header("Authorization") token: String,
        @Path("record_id") recordId: String
    ): Response<MessageResponse>
    
    // ========== 运动管理接口 ==========
    
    /**
     * 获取可用运动类型列表
     * GET /api/sports/get-available-sports-types
     */
    @GET("api/sports/get-available-sports-types")
    suspend fun getAvailableSportsTypes(
        @Header("Authorization") token: String
    ): Response<List<SearchSportsResponse>>
    
    /**
     * 记录运动
     * POST /api/sports/log-sports
     */
    @POST("api/sports/log-sports")
    suspend fun logSports(
        @Header("Authorization") token: String,
        @Body request: LogSportsRequest
    ): Response<SimpleSportsResponse>


    /**
     * 添加自定义运动
     */
    @POST("sports/custom")
    suspend fun addCustomSport(
        @Header("Authorization") authHeader: String,
        @Body customSport: AddCustomSportRequest
    ): Response<SimpleSportsResponse>
    /**
     * 搜索运动记录
     * POST /api/sports/search-sports-records
     */
    @POST("api/sports/search-sports-records")
    suspend fun searchSportsRecords(
        @Header("Authorization") token: String,
        @Body request: SearchSportRecordsRequest
    ): Response<List<SearchSportRecordsResponse>>
    
    /**
     * 删除运动记录
     * DELETE /api/sports/delete-sport-record/{record_id}
     */
    @DELETE("api/sports/delete-sport-record/{record_id}")
    suspend fun deleteSportsRecord(
        @Header("Authorization") token: String,
        @Path("record_id") recordId: String
    ): Response<SimpleSportsResponse>
}


