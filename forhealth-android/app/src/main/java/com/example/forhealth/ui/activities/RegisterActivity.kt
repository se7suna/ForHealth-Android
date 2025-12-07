package com.example.forhealth.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.forhealth.R
import com.example.forhealth.databinding.ActivityRegisterBinding
import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.auth.SendRegistrationCodeRequest
import com.example.forhealth.network.dto.auth.UserRegisterRequest
import com.example.forhealth.network.safeApiCall
import com.example.forhealth.utils.TokenManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            register()
        }

        binding.btnBackToLogin.setOnClickListener {
            finish()
        }

        // 发送验证码按钮
        binding.btnSendCode.setOnClickListener {
            sendVerificationCode()
        }
    }

    private fun sendVerificationCode() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_complete_info), Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSendCode.isEnabled = false
        binding.btnSendCode.text = "发送中..."

        lifecycleScope.launch {
            val result = safeApiCall {
                RetrofitClient.apiService.sendVerificationCode(
                    SendRegistrationCodeRequest(email = email)
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    Toast.makeText(this@RegisterActivity, "验证码已发送", Toast.LENGTH_SHORT).show()
                    binding.btnSendCode.isEnabled = true
                    binding.btnSendCode.text = getString(R.string.send_verification_code)
                }
                is ApiResult.Error -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "发送验证码失败: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.btnSendCode.isEnabled = true
                    binding.btnSendCode.text = getString(R.string.send_verification_code)
                }
                is ApiResult.Loading -> {
                    // Loading state is already handled by button state
                }
            }
        }
    }

    private fun register() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val passwordConfirm = binding.etPasswordConfirm.text.toString().trim()
        val verificationCode = binding.etVerificationCode.text.toString().trim()

        // 验证输入
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty() || verificationCode.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_complete_info), Toast.LENGTH_SHORT).show()
            return
        }

        if (username.length < 2) {
            Toast.makeText(this, getString(R.string.username_too_short), Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
            return
        }

        if (password != passwordConfirm) {
            Toast.makeText(this, getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "注册中..."

        lifecycleScope.launch {
            val result = safeApiCall {
                RetrofitClient.apiService.register(
                    UserRegisterRequest(
                        email = email,
                        username = username,
                        password = password,
                        verification_code = verificationCode
                    )
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    Toast.makeText(this@RegisterActivity, getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                    // 注册成功后跳转到登录页面
                    finish()
                }
                is ApiResult.Error -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "${getString(R.string.register_failed)}: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = getString(R.string.btn_register)
                }
                is ApiResult.Loading -> {
                    // Loading state is already handled by button state
                }
            }
        }
    }
}

