package com.example.forhealth.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.forhealth.R
import com.example.forhealth.model.TokenResponse
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.ApiService
import com.example.forhealth.user.BodyDataActivity
import com.example.forhealth.HomeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        btnLogin.setOnClickListener { login() }
        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun login() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
            return
        }

        // 模拟网络请求延迟，方便观察UI变化
        CoroutineScope(Dispatchers.Main).launch {
            // 模拟延时1秒，假装在请求服务器
            kotlinx.coroutines.delay(1000)

            // 假设登录成功的账号密码是 test@example.com / 123456
            if (email == "test@example.com" && password == "123456") {
                Toast.makeText(this@LoginActivity, "登录成功（模拟）", Toast.LENGTH_SHORT).show()

                // 模拟检查用户信息（这里随便写个判断）
                checkUserProfileAndNavigateSimulated()
            } else {
                Toast.makeText(this@LoginActivity, "登录失败，账号或密码错误（模拟）", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserProfileAndNavigateSimulated() {
        // 模拟用户信息
        val userHeight: Double? = null // null代表信息未填写，需要跳转填写页面
        val userWeight: Double? = 60.0

        if (userHeight == null || userWeight == null) {
            // 跳转到信息填写页面
            startActivity(Intent(this, BodyDataActivity::class.java))
        } else {
            // 跳转主页
            startActivity(Intent(this, HomeActivity::class.java))
        }
        finish()
    }
}

