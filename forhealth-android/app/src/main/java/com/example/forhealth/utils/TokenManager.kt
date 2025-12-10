package com.example.forhealth.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Token管理工具类
 * 使用SharedPreferences存储和读取访问令牌
 */
object TokenManager {
    
    private const val PREFS_NAME = "forhealth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 保存访问令牌和刷新令牌
     * refreshToken 可以为空（如果后端不提供）
     */
    fun saveTokens(context: Context, accessToken: String, refreshToken: String?) {
        getSharedPreferences(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken ?: "")
            .apply()
    }
    
    /**
     * 保存访问令牌（仅更新access token，保留refresh token）
     */
    fun saveAccessToken(context: Context, accessToken: String) {
        getSharedPreferences(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply()
    }
    
    /**
     * 获取访问令牌
     */
    fun getAccessToken(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * 获取刷新令牌
     */
    fun getRefreshToken(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_REFRESH_TOKEN, null)
    }
    
    /**
     * 清除所有令牌（登出时使用）
     */
    fun clearTokens(context: Context) {
        getSharedPreferences(context).edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }
    
    /**
     * 检查是否已登录
     */
    fun isLoggedIn(context: Context): Boolean {
        return getAccessToken(context) != null
    }
    
    /**
     * 兼容旧方法：保存访问令牌（仅保存access token）
     */
    @Deprecated("Use saveTokens instead", ReplaceWith("saveTokens(context, token, \"\")"))
    fun saveToken(context: Context, token: String) {
        saveAccessToken(context, token)
    }
    
    /**
     * 兼容旧方法：获取访问令牌
     */
    fun getToken(context: Context): String? {
        return getAccessToken(context)
    }
    
    /**
     * 兼容旧方法：清除访问令牌
     */
    @Deprecated("Use clearTokens instead")
    fun clearToken(context: Context) {
        clearTokens(context)
    }
}

