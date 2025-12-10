package com.example.forhealth.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.forhealth.R
import com.example.forhealth.databinding.ActivityEditAccountBinding
import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.dto.user.UserProfileUpdate
import com.example.forhealth.repositories.UserRepository
import kotlinx.coroutines.launch

class EditAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditAccountBinding
    
    // 用户数据仓库
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAvatar()
        setupSaveButton()
        loadUserData()
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
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveAccountInfo()
        }
    }

    private fun saveAccountInfo() {
        val username = binding.etUsername.text.toString().trim()
        val currentPassword = binding.etCurrentPassword.text.toString()
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

        // 如果填写了新密码，需要验证
        if (newPassword.isNotEmpty()) {
            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "修改密码需要输入当前密码", Toast.LENGTH_SHORT).show()
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
                val updateRequest = UserProfileUpdate(username = username)
                val result = userRepository.updateProfile(updateRequest)
                
                loadingToast.cancel()
                
                when (result) {
                    is ApiResult.Success -> {
                        Toast.makeText(this@EditAccountActivity, "账号信息保存成功", Toast.LENGTH_SHORT).show()
                        
                        // 如果修改了密码，需要重新登录
                        if (newPassword.isNotEmpty()) {
                            // TODO: 调用修改密码API（如果后端提供）
                            Toast.makeText(this@EditAccountActivity, "密码修改功能待实现", Toast.LENGTH_SHORT).show()
                        }
                        
                        finish()
                    }
                    is ApiResult.Error -> {
                        Toast.makeText(
                            this@EditAccountActivity,
                            "保存失败: ${result.message}",
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

