package com.example.forhealth.network

import com.example.forhealth.model.TokenResponse
import com.example.forhealth.model.User
import kotlin.Double
import kotlin.String

/**
 * 测试账号专用：不访问后端，全部返回本地假数据
 */
class FakeApiService : ApiService {

    override suspend fun login(params: Map<String, String>): Result<TokenResponse> {
        return Result.success(
            TokenResponse(access_token = "FAKE_TEST_TOKEN")
        )
    }

    override suspend fun register(params: Map<String, String>): Result<TokenResponse> {
        // 注册也直接成功
        return Result.success(
            TokenResponse(access_token = "FAKE_TEST_TOKEN")
        )
    }

    override suspend fun getProfile(): Result<User> {
        // 模拟一个已经填写的完整资料
        return Result.success(
            User(
                email = "test@example.com",
                username = "test_user",
                height = 170.0,
                weight = 55.0,
                age = 30,
                gender = "male",
                birthdate = "1995-01-01",
                birth_year = 1995,
                birth_month = 1,
                birth_day = 1,
                activity_level = "中度活跃",
                health_goal_type = "维持体重",
                goal_type = "维持体重",
                target_weight = 55.0,
                goal_weight= 55.0,
                goal_period_weeks = 10,
                goal_weeks = 10,
                bmr = null,
                tdee = null,
                daily_calorie_goal = null
            )
        )
    }

    override suspend fun updateProfile(params: Map<String, Any>): Result<User> {
        // 直接返回“更新成功”（只是把参数 merge 回去）
        val updatedUser = User(
            email = "test@example.com",
            height = params["height"] as? Double ?: 170.0,
            weight = params["weight"] as? Double ?: 55.0,
            age = params["age"] as? Int ?: 25,
            gender = params[""] as? String ?: "male",
            activity_level = params["activity_level"] as? String ?: "中度活跃"
        )
        return Result.success(updatedUser)
    }
}
