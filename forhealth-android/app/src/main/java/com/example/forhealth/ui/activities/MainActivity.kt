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
        
        // 初始化RetrofitClient
        RetrofitClient.setApplicationContext(this)
        RetrofitClient.setTokenProvider {
            TokenManager.getAccessToken(this)
        }
        
        // 设置系统UI，让内容延伸到状态栏下方
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_main)
        
        // 不再自动检查首次登录，用户需要主动操作才会打开编辑页面
        // 首次注册后的数据填写应该在注册流程中完成，而不是每次启动都检查
    }
}

