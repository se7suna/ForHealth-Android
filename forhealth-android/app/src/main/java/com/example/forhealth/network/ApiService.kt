package com.example.forhealth.network

import com.example.forhealth.network.dto.auth.*
import com.example.forhealth.network.dto.user.*
import com.example.forhealth.network.dto.sports.*
import com.example.forhealth.network.dto.food.*
import com.example.forhealth.network.dto.recipe.*
import com.example.forhealth.network.dto.visualization.*
import com.example.forhealth.network.dto.ai.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 后端API接口定义
 * 基于 OpenAPI 3.1.0 规范
 */
interface ApiService {
    
    // ==================== 认证相关 ====================
    
    /**
     * 发送注册验证码
     * POST /api/auth/send-verification-code
     */
    @POST("auth/send-verification-code")
    suspend fun sendVerificationCode(
        @Body request: SendRegistrationCodeRequest
    ): Response<MessageResponse>
    
    /**
     * 用户注册
     * POST /api/auth/register
     */
    @POST("auth/register")
    suspend fun register(
        @Body request: UserRegisterRequest
    ): Response<MessageResponse>
    
    /**
     * 用户登录
     * POST /api/auth/login
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: UserLoginRequest
    ): Response<TokenResponse>
    
    /**
     * 发送密码重置验证码
     * POST /api/auth/password-reset/send-code
     */
    @POST("auth/password-reset/send-code")
    suspend fun sendPasswordResetCode(
        @Body request: PasswordResetRequest
    ): Response<MessageResponse>
    
    /**
     * 验证验证码并重置密码
     * POST /api/auth/password-reset/verify
     */
    @POST("auth/password-reset/verify")
    suspend fun resetPassword(
        @Body request: PasswordResetVerify
    ): Response<MessageResponse>
    
    /**
     * 刷新 access token
     * POST /api/auth/refresh
     */
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<TokenResponse>
    
    // ==================== 用户管理 ====================
    
    /**
     * 更新用户身体基本数据
     * POST /api/user/body-data
     */
    @POST("user/body-data")
    suspend fun updateBodyData(
        @Body request: BodyDataRequest
    ): Response<MessageResponse>
    
    /**
     * 更新活动水平
     * POST /api/user/activity-level
     */
    @POST("user/activity-level")
    suspend fun updateActivityLevel(
        @Body request: ActivityLevelRequest
    ): Response<MessageResponse>
    
    /**
     * 设定健康目标
     * POST /api/user/health-goal
     */
    @POST("user/health-goal")
    suspend fun updateHealthGoal(
        @Body request: HealthGoalRequest
    ): Response<MessageResponse>
    
    /**
     * 获取用户完整资料
     * GET /api/user/profile
     */
    @GET("user/profile")
    suspend fun getProfile(): Response<UserProfileResponse>
    
    /**
     * 更新用户资料
     * PUT /api/user/profile
     */
    @PUT("user/profile")
    suspend fun updateProfile(
        @Body request: UserProfileUpdate
    ): Response<UserProfileResponse>
    
    /**
     * 创建体重记录
     * POST /api/user/weight-record
     */
    @POST("user/weight-record")
    suspend fun createWeightRecord(
        @Body request: WeightRecordCreateRequest
    ): Response<WeightRecordResponse>
    
    /**
     * 获取体重记录列表
     * GET /api/user/weight-records
     */
    @GET("user/weight-records")
    suspend fun getWeightRecords(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<WeightRecordListResponse>
    
    /**
     * 更新体重记录
     * PUT /api/user/weight-record/{record_id}
     */
    @PUT("user/weight-record/{record_id}")
    suspend fun updateWeightRecord(
        @Path("record_id") recordId: String,
        @Body request: WeightRecordUpdateRequest
    ): Response<WeightRecordResponse>
    
    /**
     * 删除体重记录
     * DELETE /api/user/weight-record/{record_id}
     */
    @DELETE("user/weight-record/{record_id}")
    suspend fun deleteWeightRecord(
        @Path("record_id") recordId: String
    ): Response<MessageResponse>
    
    // ==================== 运动记录 ====================
    
    /**
     * 创建自定义运动类型
     * POST /api/sports/create-sport
     * 注意：使用 multipart/form-data 格式，支持图片上传
     */
    @Multipart
    @POST("sports/create-sport")
    suspend fun createSport(
        @Part("sport_name") sportName: String?,
        @Part("describe") describe: String?,
        @Part("METs") mets: Double?,
        @Part imageFile: okhttp3.MultipartBody.Part? = null
    ): Response<SimpleSportsResponse>
    
    /**
     * 更新自定义运动类型
     * POST /api/sports/update-sport
     * 注意：使用 multipart/form-data 格式，支持图片上传
     */
    @Multipart
    @POST("sports/update-sport")
    suspend fun updateSport(
        @Part("old_sport_name") oldSportName: String?,
        @Part("new_sport_name") newSportName: String?,
        @Part("describe") describe: String?,
        @Part("METs") mets: Double?,
        @Part imageFile: okhttp3.MultipartBody.Part? = null
    ): Response<SimpleSportsResponse>
    
    /**
     * 删除自定义运动类型
     * DELETE /api/sports/delete-sport/{sport_name}
     */
    @DELETE("sports/delete-sport/{sport_name}")
    suspend fun deleteSport(
        @Path("sport_name") sportName: String
    ): Response<SimpleSportsResponse>
    
    /**
     * 获取用户可用的运动类型列表
     * GET /api/sports/get-available-sports-types
     */
    @GET("sports/get-available-sports-types")
    suspend fun getAvailableSportsTypes(): Response<List<SearchSportsResponse>>
    
    /**
     * 记录运动及消耗卡路里
     * POST /api/sports/log-sports
     */
    @POST("sports/log-sports")
    suspend fun logSports(
        @Body request: LogSportsRequest
    ): Response<SimpleSportsResponse>
    
    /**
     * 更新运动记录
     * POST /api/sports/update-sport-record
     */
    @POST("sports/update-sport-record")
    suspend fun updateSportRecord(
        @Body request: UpdateSportsRecordRequest
    ): Response<SimpleSportsResponse>
    
    /**
     * 删除运动记录
     * DELETE /api/sports/delete-sport-record/{record_id}
     */
    @DELETE("sports/delete-sport-record/{record_id}")
    suspend fun deleteSportRecord(
        @Path("record_id") recordId: String
    ): Response<SimpleSportsResponse>
    
    /**
     * 查询运动历史
     * POST /api/sports/search-sports-records
     */
    @POST("sports/search-sports-records")
    suspend fun searchSportsRecords(
        @Body request: SearchSportRecordsRequest
    ): Response<List<SearchSportRecordsResponse>>
    
    /**
     * 获取用户全部运动记录
     * GET /api/sports/get-all-sports-records
     */
    @GET("sports/get-all-sports-records")
    suspend fun getAllSportsRecords(): Response<List<SearchSportRecordsResponse>>
    
    /**
     * 获取用户运动报告
     * GET /api/sports/sports-report
     */
    @GET("sports/sports-report")
    suspend fun getSportsReport(): Response<Any> // TODO: 定义具体响应类型
    
    // ==================== 食物管理 ====================
    
    /**
     * 创建食物信息（支持图片文件上传）
     * POST /api/food/
     * 注意：使用 multipart/form-data 格式
     */
    @Multipart
    @POST("food/")
    suspend fun createFood(
        @Part("name") name: String,
        @Part("serving_size") servingSize: Double,
        @Part("calories") calories: Double,
        @Part("protein") protein: Double,
        @Part("carbohydrates") carbohydrates: Double,
        @Part("fat") fat: Double,
        @Part("category") category: String? = null,
        @Part("serving_unit") servingUnit: String? = null,
        @Part("brand") brand: String? = null,
        @Part("barcode") barcode: String? = null,
        @Part("fiber") fiber: Double? = null,
        @Part("sugar") sugar: Double? = null,
        @Part("sodium") sodium: Double? = null,
        @Part image: okhttp3.MultipartBody.Part? = null
    ): Response<FoodResponse>
    
    /**
     * 搜索食物（薄荷健康数据库）
     * GET /api/food/search
     * 
     * 注意：根据 simplified 参数，返回类型可能是 FoodListResponse 或 SimplifiedFoodListResponse
     * 调用时需要根据 simplified 参数的值进行类型判断和转换
     */
    @GET("food/search")
    suspend fun searchFoods(
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = 1,
        @Query("include_full_nutrition") includeFullNutrition: Boolean = true,
        @Query("simplified") simplified: Boolean = false
    ): Response<Any> // 返回 FoodListResponse (simplified=false) 或 SimplifiedFoodListResponse (simplified=true)
    
    /**
     * 通过食物名称搜索本地数据库
     * GET /api/food/search-id
     */
    @GET("food/search-id")
    suspend fun searchFoodById(
        @Query("keyword") keyword: String,
        @Query("limit") limit: Int = 20
    ): Response<FoodIdSearchResponse>
    
    /**
     * 根据ID获取食物详情
     * GET /api/food/{food_id}
     */
    @GET("food/{food_id}")
    suspend fun getFood(
        @Path("food_id") foodId: String
    ): Response<FoodResponse>
    
    /**
     * 更新食物信息
     * PUT /api/food/{food_id}
     */
    @PUT("food/{food_id}")
    suspend fun updateFood(
        @Path("food_id") foodId: String,
        @Body request: FoodUpdateRequest
    ): Response<FoodResponse>
    
    /**
     * 删除食物
     * DELETE /api/food/{food_id}
     */
    @DELETE("food/{food_id}")
    suspend fun deleteFood(
        @Path("food_id") foodId: String
    ): Response<MessageResponse>
    
    /**
     * 记录食物摄入
     * POST /api/food/record
     */
    @POST("food/record")
    suspend fun createFoodRecord(
        @Body request: FoodRecordCreateRequest
    ): Response<FoodRecordResponse>
    
    /**
     * 获取食物记录列表
     * GET /api/food/record/list
     */
    @GET("food/record/list")
    suspend fun getFoodRecords(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("meal_type") mealType: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<FoodRecordListResponse>
    
    /**
     * 获取某日的营养摘要
     * GET /api/food/record/daily/{target_date}
     */
    @GET("food/record/daily/{target_date}")
    suspend fun getDailyNutrition(
        @Path("target_date") targetDate: String
    ): Response<DailyNutritionSummary>
    
    /**
     * 更新食物记录
     * PUT /api/food/record/{record_id}
     */
    @PUT("food/record/{record_id}")
    suspend fun updateFoodRecord(
        @Path("record_id") recordId: String,
        @Body request: FoodRecordUpdateRequest
    ): Response<FoodRecordResponse>
    
    /**
     * 删除食物记录
     * DELETE /api/food/record/{record_id}
     */
    @DELETE("food/record/{record_id}")
    suspend fun deleteFoodRecord(
        @Path("record_id") recordId: String
    ): Response<MessageResponse>
    
    /**
     * 从图片识别条形码
     * POST /api/food/barcode/recognize
     */
    @Multipart
    @POST("food/barcode/recognize")
    suspend fun recognizeBarcode(
        @Part file: okhttp3.MultipartBody.Part
    ): Response<BarcodeImageRecognitionResponse>
    
    /**
     * 扫描条形码查询食品信息
     * GET /api/food/barcode/{barcode}
     */
    @GET("food/barcode/{barcode}")
    suspend fun scanBarcode(
        @Path("barcode") barcode: String
    ): Response<BarcodeScanResponse>
    
    /**
     * 更新食物图片
     * PUT /api/food/{food_id}/image
     */
    @Multipart
    @PUT("food/{food_id}/image")
    suspend fun updateFoodImage(
        @Path("food_id") foodId: String,
        @Part image: okhttp3.MultipartBody.Part
    ): Response<FoodResponse>
    
    // ==================== 食谱管理 ====================
    
    /**
     * 创建食谱（支持图片文件上传）
     * POST /api/recipe/
     * 注意：使用 multipart/form-data 格式
     */
    @Multipart
    @POST("recipe/")
    suspend fun createRecipe(
        @Part("name") name: String,
        @Part("foods") foods: String, // JSON数组字符串
        @Part("description") description: String? = null,
        @Part("category") category: String? = null,
        @Part("tags") tags: String? = null, // JSON数组字符串
        @Part("prep_time") prepTime: Int? = null,
        @Part image: okhttp3.MultipartBody.Part? = null
    ): Response<RecipeResponse>
    
    /**
     * 搜索食谱
     * GET /api/recipe/search
     */
    @GET("recipe/search")
    suspend fun searchRecipes(
        @Query("keyword") keyword: String? = null,
        @Query("category") category: String? = null,
        @Query("tags") tags: List<String>? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<RecipeListResponse>
    
    /**
     * 通过食谱名称搜索食谱ID
     * GET /api/recipe/search-id
     */
    @GET("recipe/search-id")
    suspend fun searchRecipeById(
        @Query("keyword") keyword: String,
        @Query("limit") limit: Int = 10
    ): Response<RecipeIdSearchResponse>
    
    /**
     * 获取所有食谱分类
     * GET /api/recipe/categories
     */
    @GET("recipe/categories")
    suspend fun getRecipeCategories(): Response<List<String>>
    
    /**
     * 记录食谱摄入
     * POST /api/recipe/record
     */
    @POST("recipe/record")
    suspend fun createRecipeRecord(
        @Body request: RecipeRecordCreateRequest
    ): Response<RecipeRecordResponse>
    
    /**
     * 获取食谱记录列表
     * GET /api/recipe/record
     */
    @GET("recipe/record")
    suspend fun getRecipeRecords(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("meal_type") mealType: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<RecipeRecordListResponse>
    
    /**
     * 更新食谱记录
     * PUT /api/recipe/record/{batch_id}
     */
    @PUT("recipe/record/{batch_id}")
    suspend fun updateRecipeRecord(
        @Path("batch_id") batchId: String,
        @Body request: RecipeRecordUpdateRequest
    ): Response<RecipeRecordUpdateResponse>
    
    /**
     * 删除食谱记录
     * DELETE /api/recipe/record/{batch_id}
     */
    @DELETE("recipe/record/{batch_id}")
    suspend fun deleteRecipeRecord(
        @Path("batch_id") batchId: String
    ): Response<MessageResponse>
    
    /**
     * 获取食谱详情
     * GET /api/recipe/{recipe_id}
     */
    @GET("recipe/{recipe_id}")
    suspend fun getRecipe(
        @Path("recipe_id") recipeId: String
    ): Response<RecipeResponse>
    
    /**
     * 更新食谱
     * PUT /api/recipe/{recipe_id}
     */
    @PUT("recipe/{recipe_id}")
    suspend fun updateRecipe(
        @Path("recipe_id") recipeId: String,
        @Body request: RecipeUpdateRequest
    ): Response<RecipeResponse>
    
    /**
     * 删除食谱
     * DELETE /api/recipe/{recipe_id}
     */
    @DELETE("recipe/{recipe_id}")
    suspend fun deleteRecipe(
        @Path("recipe_id") recipeId: String
    ): Response<MessageResponse>
    
    /**
     * 更新食谱图片
     * PUT /api/recipe/{recipe_id}/image
     */
    @Multipart
    @PUT("recipe/{recipe_id}/image")
    suspend fun updateRecipeImage(
        @Path("recipe_id") recipeId: String,
        @Part image: okhttp3.MultipartBody.Part
    ): Response<RecipeResponse>
    
    // ==================== 可视化报告 ====================
    
    /**
     * 获取每日卡路里摘要
     * GET /api/visualization/daily-calorie-summary
     */
    @GET("visualization/daily-calorie-summary")
    suspend fun getDailyCalorieSummary(
        @Query("target_date") targetDate: String? = null
    ): Response<DailyCalorieSummary>
    
    /**
     * 获取营养素与食物来源分析
     * GET /api/visualization/nutrition-analysis
     */
    @GET("visualization/nutrition-analysis")
    suspend fun getNutritionAnalysis(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): Response<NutritionAnalysisResponse>
    
    /**
     * 获取时间序列趋势分析
     * GET /api/visualization/time-series-trend
     */
    @GET("visualization/time-series-trend")
    suspend fun getTimeSeriesTrend(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("view_type") viewType: String = "day"
    ): Response<TimeSeriesTrendResponse>
    
    /**
     * 导出健康数据报告
     * GET /api/visualization/export-report
     */
    @GET("visualization/export-report")
    suspend fun exportHealthReport(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): Response<HealthReportExportResponse>
    
    // ==================== AI 助手 ====================
    
    /**
     * 拍照识别食物并自动处理
     * POST /api/ai/food/recognize
     */
    @Multipart
    @POST("ai/food/recognize")
    suspend fun recognizeFood(
        @Part file: okhttp3.MultipartBody.Part,
        @Part("meal_type") mealType: String? = null,
        @Part("notes") notes: String? = null,
        @Part("recorded_at") recordedAt: String? = null
    ): Response<FoodRecognitionConfirmResponse>
    
    /**
     * 生成个性化饮食计划
     * POST /api/ai/meal-plan/generate
     */
    @POST("ai/meal-plan/generate")
    suspend fun generateMealPlan(
        @Body request: MealPlanRequest
    ): Response<MealPlanResponse>
    
    /**
     * 健康知识问答
     * POST /api/ai/ask
     */
    @POST("ai/ask")
    suspend fun askQuestion(
        @Body request: QuestionRequest
    ): Response<QuestionResponse>
    
    /**
     * 饮食分析与建议
     * POST /api/ai/diet/analyze
     */
    @POST("ai/diet/analyze")
    suspend fun analyzeDiet(
        @Body request: DietAnalysisRequest
    ): Response<DietAnalysisResponse>
    
    /**
     * 智能菜式推荐
     * GET /api/ai/meal/recommend
     */
    @GET("ai/meal/recommend")
    suspend fun recommendMeal(): Response<MealRecommendationResponse>
    
    /**
     * 获取提醒设置
     * GET /api/ai/reminders/settings
     */
    @GET("ai/reminders/settings")
    suspend fun getReminderSettings(): Response<ReminderSettingsResponse>
    
    /**
     * 更新提醒设置
     * PUT /api/ai/reminders/settings
     */
    @PUT("ai/reminders/settings")
    suspend fun updateReminderSettings(
        @Body request: ReminderSettingsRequest
    ): Response<ReminderSettingsResponse>
    
    /**
     * 获取通知列表
     * GET /api/ai/notifications
     */
    @GET("ai/notifications")
    suspend fun getNotifications(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("unread_only") unreadOnly: Boolean = false
    ): Response<NotificationListResponse>
    
    /**
     * 标记通知为已读
     * POST /api/ai/notifications/mark-read
     */
    @POST("ai/notifications/mark-read")
    suspend fun markNotificationsRead(
        @Body request: NotificationReadRequest
    ): Response<Map<String, Any>>
    
    /**
     * 获取每日反馈
     * GET /api/ai/feedback/daily/{target_date}
     */
    @GET("ai/feedback/daily/{target_date}")
    suspend fun getDailyFeedback(
        @Path("target_date") targetDate: String
    ): Response<DailyFeedbackResponse>
}
