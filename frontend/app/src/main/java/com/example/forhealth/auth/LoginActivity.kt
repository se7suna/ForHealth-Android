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
    private val apiService = RetrofitClient.apiService

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
 
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = RetrofitClient.apiService.login(mapOf("email" to email, "password" to password))

                result.fold(
                    onSuccess = { tokenResponse ->
                        val token = tokenResponse.access_token
                        RetrofitClient.saveToken(token)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()
                            // 检查用户是否已完成信息填写
                            checkUserProfileAndNavigate()
                        }
                    },
                    onFailure = { e ->
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "登录失败，请检查邮箱和密码", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkUserProfileAndNavigate() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = RetrofitClient.apiService.getProfile()
                result.fold(
                    onSuccess = { user ->
                        withContext(Dispatchers.Main) {
                            // 如果用户信息不完整，跳转到填写页面；否则跳转到主页
                            if (user.height == null || user.weight == null) {
                                val intent = Intent(this@LoginActivity, BodyDataActivity::class.java)
                                startActivity(intent)
                            } else {
                                val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                                startActivity(intent)
                            }
                            finish()
                        }
                    },
                    onFailure = {
                        withContext(Dispatchers.Main) {
                            // 如果获取用户信息失败，默认跳转到数据填写页面
                            val intent = Intent(this@LoginActivity, BodyDataActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@LoginActivity, BodyDataActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
