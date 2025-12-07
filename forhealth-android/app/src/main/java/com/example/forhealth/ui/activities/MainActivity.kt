package com.example.forhealth.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.forhealth.R
import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.safeApiCall
import com.example.forhealth.utils.ProfileManager
import com.example.forhealth.utils.TokenManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 检查是否已登录
        if (!TokenManager.isLoggedIn(this)) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        // 初始化RetrofitClient的TokenProvider
        RetrofitClient.setTokenProvider {
            TokenManager.getAccessToken(this)
        }
        
        // 设置系统UI，让内容延伸到状态栏下方
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_main)
        
        // 检查用户是否首次登录（数据是否完整）
        checkFirstTimeLogin()
    }
    
    private fun checkFirstTimeLogin() {
        // 先从本地检查
        val localProfile = ProfileManager.getProfile(this)
        val isLocalDataComplete = localProfile?.let { profile ->
            profile.height != null && 
            profile.weight != null && 
            profile.gender != null && 
            profile.birthdate != null &&
            profile.activity_level != null &&
            profile.health_goal_type != null
        } ?: false
        
        if (!isLocalDataComplete) {
            // 本地数据不完整，跳转到编辑数据页面
            val intent = Intent(this, EditDataActivity::class.java)
            startActivity(intent)
            return
        }
        
        // 尝试从API获取（如果后端可用）
        lifecycleScope.launch {
            val result = safeApiCall {
                RetrofitClient.apiService.getProfile()
            }
            when (result) {
                is ApiResult.Success -> {
                    val profile = result.data
                    // 保存到本地
                    ProfileManager.saveProfile(this@MainActivity, profile)
                    // 检查关键数据是否完整
                    val isDataComplete = profile.height != null && 
                                        profile.weight != null && 
                                        profile.gender != null && 
                                        profile.birthdate != null &&
                                        profile.activity_level != null &&
                                        profile.health_goal_type != null
                    
                    if (!isDataComplete) {
                        // 数据不完整，跳转到编辑数据页面
                        val intent = Intent(this@MainActivity, EditDataActivity::class.java)
                        startActivity(intent)
                    }
                }
                is ApiResult.Error -> {
                    // 如果获取失败，使用本地数据（已在上面检查）
                }
                is ApiResult.Loading -> {
                    // Loading state
                }
            }
        }
    }
}

