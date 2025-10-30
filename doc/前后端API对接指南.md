# For Health 前后端 API 对接与打通精简指南

本指南面向前端开发者，教你快速把 Android 前端与 FastAPI 后端打通：如何启动后端、配置前端网络层、进行数据转换与 Token 认证、完成注册/登录/资料拉取与更新。按顺序完成即可跑通主流程。

标签说明：
- [已配好]：仓库中已实现，通常只需检查或根据环境调整。
- [开发参考]：后续写页面或新功能时需要参考与复用的内容。

## [开发参考] 启动后端服务

```bash
cd backend
uvicorn app.main:app --reload
```

- 本地 Swagger 文档：`http://127.0.0.1:8000/docs`

## [已配好] 配置前端网络权限与依赖

- 在 `frontend/app/src/main/AndroidManifest.xml` 中确保：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 若使用 http 开发地址 -->
<application
    ...
    android:usesCleartextTraffic="true" />
```

- 在 `frontend/app/build.gradle.kts` 添加（或确认存在）：

```kotlin
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

## [已配好] 统一配置 BASE_URL

在 `frontend/app/src/main/java/com/example/forhealth/network/RetrofitClient.kt`：

```kotlin
private const val BASE_URL = "http://10.0.2.2:8000/"  // Android 模拟器访问本机
// 真机测试替换为电脑局域网 IP，例如："http://192.168.1.100:8000/"
```

并在应用启动时初始化（例如 `ForHealthApplication.kt` 或 `MainActivity.kt`）：

```kotlin
RetrofitClient.init(applicationContext)
```

## [开发参考] 定义 Retrofit 接口与数据模型

- 接口 `frontend/app/src/main/java/com/example/forhealth/network/ApiInterface.kt`

```kotlin
interface ApiInterface {
    @POST("/api/auth/login")
    suspend fun login(@Body params: Map<String, String>): Response<TokenResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body params: Map<String, String>): Response<TokenResponse>

    @GET("/api/user/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<User>

    @PUT("/api/user/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body params: Map<String, Any>
    ): Response<User>
}
```

- 模型对齐后端字段，确认 `User.kt`、`TokenResponse.kt` 已匹配后端返回。

## [开发参考] 数据转换（中英文字段映射）

在 `frontend/app/src/main/java/com/example/forhealth/utils/DataMapper.kt` 维护统一转换：

```kotlin
object DataMapper {
    fun genderToBackend(g: String) = when (g) { "男" -> "male"; "女" -> "female"; else -> "male" }
    fun genderFromBackend(g: String) = when (g) { "male" -> "男"; "female" -> "女"; else -> "男" }

    fun activityLevelToBackend(l: String) = when (l) {
        "久坐" -> "sedentary"
        "轻度活跃" -> "lightly_active"
        "中度活跃" -> "moderately_active"
        "非常活跃" -> "very_active"
        "极其活跃" -> "extremely_active"
        else -> "moderately_active"
    }
    fun activityLevelFromBackend(l: String) = when (l) {
        "sedentary" -> "久坐"
        "lightly_active" -> "轻度活跃"
        "moderately_active" -> "中度活跃"
        "very_active" -> "非常活跃"
        "extremely_active" -> "极其活跃"
        else -> "中度活跃"
    }

    fun goalTypeToBackend(t: String) = when (t) {
        "减重" -> "lose_weight"
        "维持体重" -> "maintain_weight"
        "增重" -> "gain_weight"
        else -> "maintain_weight"
    }
    fun goalTypeFromBackend(t: String) = when (t) {
        "lose_weight" -> "减重"
        "maintain_weight" -> "维持体重"
        "gain_weight" -> "增重"
        else -> "维持体重"
    }

    fun birthDateToBackend(year: Int, month: Int, day: Int) = String.format("%04d-%02d-%02d", year, month, day)
}
```

## [已配好] 网络客户端与 Token 管理（使用时参考示例）

`RetrofitClient.kt` 负责创建 Retrofit/OkHttp 与统一错误包装；并通过 `PrefsHelper` 读写 Token：

- 登录/注册成功后保存 Token：`PrefsHelper.saveToken(context, token)`
- 发起需鉴权请求时自动附加 `Authorization: Bearer {token}`
- 退出登录时清空 Token

示例（片段）：

```kotlin
val response = apiInterface.getProfile("Bearer $token")
if (response.isSuccessful) { /* 处理 User */ } else { /* 处理错误 */ }
```

## [开发参考] 关键页面接入要点

- 注册页：新增并校验 `username`，成功后自动登录，保存 Token。
- 登录页：登录成功保存 Token，按是否已完善资料决定跳转。
- 资料填写页（如 `HealthGoalActivity.kt`）：提交前使用 `DataMapper` 转换字段，并一次性 `PUT /api/user/profile`。

示例参数构造：

```kotlin
val payload = mapOf(
    "gender" to DataMapper.genderToBackend(gender),
    "birthdate" to DataMapper.birthDateToBackend(y, m, d),
    "height" to height.toDouble(),
    "weight" to weight.toDouble(),
    "activity_level" to DataMapper.activityLevelToBackend(level),
    "health_goal_type" to DataMapper.goalTypeToBackend(goalType),
    "target_weight" to targetWeight.toDouble(),
    "goal_period_weeks" to goalWeeks
)
```

- 个人信息页（如 `ProfileActivity.kt`）：拉取 `GET /api/user/profile`，用 `DataMapper` 将英文枚举转中文展示。

## [开发参考] 测试清单

1) 启动后端，确认 `http://127.0.0.1:8000/docs` 通畅。
2) 配置 `BASE_URL`（模拟器用 `10.0.2.2`，真机用电脑局域网 IP）。
3) 在 Android Studio 运行应用：
   - 新用户注册 → 自动登录 → Token 保存
   - 登录 → 获取个人资料 → 跳转逻辑正确
   - 填写资料 → 提交更新 → 个人信息页显示正确

## [开发参考] 常见问题速查

- 网络失败：检查后端进程/`BASE_URL`/权限/防火墙。
- 401 未授权：检查 Token 是否保存、是否过期、`Authorization` 头格式。
- JSON 解析报错：核对模型字段命名与后端响应字段一致（Gson 策略）。
- 跨域：移动端直连一般不涉 CORS；若经 WebView/网关，请检查后端 CORS。

---

状态：前后端已可用，按本指南完成配置即可直连后端 API 进行注册、登录、资料获取与更新。
