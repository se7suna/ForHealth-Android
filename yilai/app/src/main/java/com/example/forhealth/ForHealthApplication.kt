package com.example.forhealth

import android.app.Application
import com.example.forhealth.network.RetrofitClient

/**
 * Application类
 * 应用启动时初始化全局组件
 */
class ForHealthApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化Retrofit客户端
        RetrofitClient.init(applicationContext)
    }
}


