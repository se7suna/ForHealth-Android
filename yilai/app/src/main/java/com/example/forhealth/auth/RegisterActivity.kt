package com.example.forhealth.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.forhealth.R
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.ApiService
import com.example.forhealth.user.BodyDataActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.fold

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPasswordConfirm: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnBackToLogin: Button
    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 设置ActionBar
        supportActionBar?.title = "注册"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm)
        btnRegister = findViewById(R.id.btnRegister)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)

        btnRegister.setOnClickListener { register() }
        btnBackToLogin.setOnClickListener { 
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun register() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val passwordConfirm = etPasswordConfirm.text.toString().trim()

        // 验证输入
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
            return
        }

        if (username.length < 2) {
            Toast.makeText(this, "用户名至少2个字符", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "请输入有效的邮箱地址", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "密码长度至少6位", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != passwordConfirm) {
            Toast.makeText(this, "两次密码输入不一致", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 注册请求需要包含username
                val result = apiService.register(mapOf(
                    "username" to username,
                    "email" to email,
                    "password" to password
                ))
                result.fold(
                    onSuccess = { tokenResponse ->
                        val token = tokenResponse.access_token
                        RetrofitClient.saveToken(token)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "注册成功，请填写个人信息", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@RegisterActivity, BodyDataActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    },
                    onFailure = { e ->
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "注册失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
