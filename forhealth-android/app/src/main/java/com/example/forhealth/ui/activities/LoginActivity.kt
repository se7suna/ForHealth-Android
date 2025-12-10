package com.example.forhealth.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.forhealth.R
import com.example.forhealth.databinding.ActivityLoginBinding
import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.auth.UserLoginRequest
import com.example.forhealth.network.safeApiCall
import com.example.forhealth.utils.TokenManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 检查是否已登录
        if (TokenManager.isLoggedIn(this)) {
            navigateToMain()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            login()
        }

        binding.btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun login() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_complete_info), Toast.LENGTH_SHORT).show()
            return
        }

        // 禁用按钮，防止重复点击
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "登录中..."

        lifecycleScope.launch {
            val result = safeApiCall {
                RetrofitClient.apiService.login(
                    UserLoginRequest(
                        email = email,
                        password = password
                    )
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    val tokenResponse = result.data
                    // 保存access_token和refresh_token
                    TokenManager.saveTokens(
                        this@LoginActivity,
                        tokenResponse.access_token,
                        tokenResponse.refresh_token
                    )
                    // 初始化RetrofitClient的TokenProvider
                    RetrofitClient.setTokenProvider {
                        TokenManager.getAccessToken(this@LoginActivity)
                    }

                    Toast.makeText(this@LoginActivity, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is ApiResult.Error -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "${getString(R.string.login_failed)}: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.btn_login)
                }
                is ApiResult.Loading -> {
                    // Loading state is already handled by button state
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

