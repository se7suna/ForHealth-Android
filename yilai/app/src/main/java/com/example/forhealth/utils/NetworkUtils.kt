package com.example.forhealth.network.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object NetworkUtils {

    private val client = OkHttpClient()

    // 简单的 GET 请求
    fun makeApiRequest(url: String, callback: ApiCallback) {
        val request = Request.Builder().url(url).build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                callback.onSuccess(response.body?.string() ?: "")
            } else {
                callback.onError("请求失败: ${response.message}")
            }
        } catch (e: IOException) {
            callback.onError("网络错误: ${e.message}")
        }
    }

    interface ApiCallback {
        fun onSuccess(response: String)
        fun onError(error: String)
    }
}
