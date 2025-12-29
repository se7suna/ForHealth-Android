package com.example.forhealth.ui.activities

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.forhealth.R
import com.example.forhealth.databinding.ActivityEditAccountBinding
import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.auth.PasswordResetRequest
import com.example.forhealth.network.dto.auth.PasswordResetVerify
import com.example.forhealth.network.dto.user.UserProfileUpdate
import com.example.forhealth.network.safeApiCall
import com.example.forhealth.repositories.UserRepository
import kotlinx.coroutines.launch

class EditAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditAccountBinding
    
    // 用户数据仓库
    private val userRepository = UserRepository()
    
    // 当前用户邮箱
    private var userEmail: String? = null
    
    // 验证码倒计时
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAvatar()
        setupSaveButton()
        setupSendCodeButton()
        loadUserData()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun setupAvatar() {
        // 设置默认头像
        binding.ivAvatar.setImageResource(R.drawable.ic_user)
        // 使用 dicebear API 生成头像
        binding.ivAvatar.load("https://api.dicebear.com/9.x/avataaars/svg?seed=Felix") {
            placeholder(R.drawable.ic_user)
            error(R.drawable.ic_user)
            transformations(CircleCropTransformation())
        }

        binding.btnChangeAvatar.setOnClickListener {
            // TODO: 实现头像更换功能（可能需要图片选择器）
            Toast.makeText(this, "头像更换功能待实现", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        // 从Intent获取用户名
        intent.getStringExtra("username")?.let {
            binding.etUsername.setText(it)
        }
        
        // 加载用户资料以获取邮箱
        lifecycleScope.launch {
            when (val result = userRepository.getProfile()) {
                is ApiResult.Success -> {
                    userEmail = result.data.email
                }
                is ApiResult.Error -> {
                    Toast.makeText(
                        this@EditAccountActivity,
                        "获取用户信息失败: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is ApiResult.Loading -> {}
            }
        }
    }
    
    private fun setupSendCodeButton() {
        binding.btnSendCode.setOnClickListener {
            sendVerificationCode()
        }
    }
    
    private fun sendVerificationCode() {
        if (userEmail == null) {
            Toast.makeText(this, "无法获取用户邮箱，请稍后重试", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.btnSendCode.isEnabled = false
        binding.btnSendCode.text = "发送中..."
        
        lifecycleScope.launch {
            val result = safeApiCall {
                RetrofitClient.apiService.sendPasswordResetCode(
                    PasswordResetRequest(email = userEmail!!)
                )
            }
            
            when (result) {
                is ApiResult.Success -> {
                    Toast.makeText(this@EditAccountActivity, "验证码已发送至邮箱", Toast.LENGTH_SHORT).show()
                    startCountDown()
                }
                is ApiResult.Error -> {
                    Toast.makeText(
                        this@EditAccountActivity,
                        "发送验证码失败: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.btnSendCode.isEnabled = true
                    binding.btnSendCode.text = getString(R.string.send_verification_code)
                }
                is ApiResult.Loading -> {}
            }
        }
    }
    
    private fun startCountDown() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.btnSendCode.text = "${seconds}秒后重发"
            }
            
            override fun onFinish() {
                binding.btnSendCode.isEnabled = true
                binding.btnSendCode.text = getString(R.string.send_verification_code)
            }
        }.start()
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveAccountInfo()
        }
    }

    private fun saveAccountInfo() {
        val username = binding.etUsername.text.toString().trim()
        val verificationCode = binding.etVerificationCode.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        // 验证用户名
        if (username.isEmpty()) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show()
            return
        }

        if (username.length < 2) {
            Toast.makeText(this, getString(R.string.username_too_short), Toast.LENGTH_SHORT).show()
            return
        }

        // 如果填写了新密码，需要验证验证码
        if (newPassword.isNotEmpty()) {
            if (userEmail == null) {
                Toast.makeText(this, "无法获取用户邮箱，请稍后重试", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (verificationCode.isEmpty()) {
                Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (verificationCode.length != 6) {
                Toast.makeText(this, "验证码必须是6位数字", Toast.LENGTH_SHORT).show()
                return
            }

            if (newPassword.length < 6) {
                Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
                return
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show()
                return
            }
        }

        val loadingToast = Toast.makeText(this, "正在保存...", Toast.LENGTH_SHORT)
        loadingToast.show()

        lifecycleScope.launch {
            try {
                // 先更新用户名
                val updateRequest = UserProfileUpdate(username = username)
                val profileResult = userRepository.updateProfile(updateRequest)
                
                when (profileResult) {
                    is ApiResult.Success -> {
                        // 如果修改了密码，调用密码重置接口
                        if (newPassword.isNotEmpty()) {
                            val passwordResult = safeApiCall {
                                RetrofitClient.apiService.resetPassword(
                                    PasswordResetVerify(
                                        email = userEmail!!,
                                        verification_code = verificationCode,
                                        new_password = newPassword,
                                        confirm_password = confirmPassword
                                    )
                                )
                            }
                            
                            when (passwordResult) {
                                is ApiResult.Success -> {
                                    loadingToast.cancel()
                                    Toast.makeText(
                                        this@EditAccountActivity,
                                        "账号信息和密码修改成功，请使用新密码登录",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                }
                                is ApiResult.Error -> {
                                    loadingToast.cancel()
                                    Toast.makeText(
                                        this@EditAccountActivity,
                                        "用户名已更新，但密码修改失败: ${passwordResult.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                is ApiResult.Loading -> {}
                            }
                        } else {
                            // 只更新了用户名，没有修改密码
                            loadingToast.cancel()
                            Toast.makeText(this@EditAccountActivity, "账号信息保存成功", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    is ApiResult.Error -> {
                        loadingToast.cancel()
                        Toast.makeText(
                            this@EditAccountActivity,
                            "保存失败: ${profileResult.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is ApiResult.Loading -> {
                        // Loading state
                    }
                }
            } catch (e: Exception) {
                loadingToast.cancel()
                Toast.makeText(this@EditAccountActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

