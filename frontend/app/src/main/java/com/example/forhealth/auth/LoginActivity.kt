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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener { login() }
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
                            startActivity(Intent(this@LoginActivity, BodyDataActivity::class.java))
                            finish()
                        }
                        finish()
                    },
                    onFailure = { e ->
                        Toast.makeText(this@LoginActivity, "登录失败，请检查邮箱和密码", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun Result<TokenResponse>.body() {
        TODO("Not yet implemented")
    }
}
