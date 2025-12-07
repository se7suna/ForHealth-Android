package com.example.forhealth.network.dto.auth

/**
 * 认证相关 DTO
 */

// 发送注册验证码请求
data class SendRegistrationCodeRequest(
    val email: String
)

// 用户注册请求
data class UserRegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val verification_code: String
)

// 用户登录请求
data class UserLoginRequest(
    val email: String,
    val password: String
)

// Token 响应
data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "bearer"
)

// 刷新Token请求
data class RefreshTokenRequest(
    val refresh_token: String
)

// 发送密码重置验证码请求
data class PasswordResetRequest(
    val email: String
)

// 验证并重置密码请求
data class PasswordResetVerify(
    val email: String,
    val verification_code: String,
    val new_password: String,
    val confirm_password: String
)

// 通用消息响应
data class MessageResponse(
    val message: String,
    val data: Any? = null
)

