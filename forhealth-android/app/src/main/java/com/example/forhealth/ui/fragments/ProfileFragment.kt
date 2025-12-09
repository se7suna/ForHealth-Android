package com.example.forhealth.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.forhealth.R
import com.example.forhealth.databinding.FragmentProfileBinding
import com.example.forhealth.network.ApiResult
import com.example.forhealth.repositories.UserRepository
import com.example.forhealth.ui.activities.EditAccountActivity
import com.example.forhealth.ui.activities.EditDataActivity
import com.example.forhealth.ui.activities.LoginActivity
import com.example.forhealth.utils.TokenManager
import com.example.forhealth.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class ProfileFragment : DialogFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // 使用 activityViewModels 以共享同一个 ViewModel 实例
    private val viewModel: MainViewModel by activityViewModels()
    
    // 用户数据仓库
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackButton()
        setupMenuItems()
        setupAvatar()
        setupHeaderClick()
        observeData()
    }

    private fun setupHeaderClick() {
        // 点击Header区域可以打开EditProfile（类似React版本）
        // 注意：需要排除back button的点击事件
        val headerView = binding.root.findViewById<View>(R.id.profileHeader)
        headerView?.setOnClickListener {
            openEditProfileDialog()
        }
        
        // 确保back button的点击不会触发header的点击
        binding.btnBack.setOnClickListener {
            dismiss()
        }
    }

    fun refreshProfile() {
        // 刷新Profile数据（从API重新获取）
        // 使用lifecycleScope确保在正确的生命周期中执行
        lifecycleScope.launch {
            val result = userRepository.getProfile()
            when (result) {
                is ApiResult.Success -> {
                    val profile = result.data
                    updateProfileUI(profile)
                }
                is ApiResult.Error -> {
                    // API获取失败，显示错误提示
                    Toast.makeText(
                        requireContext(),
                        "刷新失败: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is ApiResult.Loading -> {
                    // Loading state
                }
            }
        }
    }

    private fun openEditProfileDialog() {
        val dialog = EditProfileFragment()
        dialog.show(parentFragmentManager, "EditProfileDialog")
        // EditProfileFragment 会在保存成功后自动调用 refreshProfile()
    }

    private fun setupAvatar() {
        // 使用 dicebear API 生成头像（与 React 版本一致）
        binding.ivAvatar.load("https://api.dicebear.com/9.x/avataaars/svg?seed=Felix") {
            placeholder(R.color.slate_100)
            error(R.color.slate_100)
            transformations(CircleCropTransformation())
        }
    }

    private fun setupBackButton() {
        // Back button的点击事件在setupHeaderClick中设置，避免与header点击冲突
    }

    private fun setupMenuItems() {
        // Edit Account - 编辑账号信息（用户名、头像、密码）
        binding.btnEditAccount.setOnClickListener {
            lifecycleScope.launch {
                // 获取当前用户信息
                val result = userRepository.getProfile()
                when (result) {
                    is ApiResult.Success -> {
                        val intent = Intent(requireContext(), EditAccountActivity::class.java)
                        intent.putExtra("username", result.data.username)
                        startActivity(intent)
                    }
                    is ApiResult.Error -> {
                        Toast.makeText(requireContext(), "获取用户信息失败: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                    is ApiResult.Loading -> {
                        // Loading state
                    }
                }
            }
        }

        // Weight Tracker - 打开体重追踪界面
        binding.btnWeightTracker.setOnClickListener {
            openWeightTrackerDialog()
        }

        // Log Out - 登出
        binding.btnLogOut.setOnClickListener {
            TokenManager.clearTokens(requireContext())
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun openWeightTrackerDialog() {
        // WeightTrackerFragment现在会自己从后端加载数据
        val dialog = WeightTrackerFragment()
        dialog.show(parentFragmentManager, "WeightTrackerDialog")
    }

    private fun openEditDataActivity(isRecordChanges: Boolean = false) {
        // TODO: 对接API获取用户数据
        val intent = Intent(requireContext(), EditDataActivity::class.java)
        intent.putExtra("isRecordChanges", isRecordChanges)
        startActivity(intent)
    }

    private fun observeData() {
        // 从API获取用户资料数据
        lifecycleScope.launch {
            val result = userRepository.getProfile()
            when (result) {
                is ApiResult.Success -> {
                    val profile = result.data
                    // 更新UI显示所有数据
                    updateProfileUI(profile)
                }
                is ApiResult.Error -> {
                    // API获取失败，显示默认值
                    Toast.makeText(
                        requireContext(),
                        "获取用户信息失败: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    // 显示默认值
                    binding.tvUserName.text = "--"
                    binding.tvHeight.text = "--"
                    binding.tvWeight.text = "--"
                    binding.tvAge.text = "--"
                    updateHealthGoals(
                        activityLevel = null,
                        targetWeight = null,
                        goalPeriodWeeks = null
                    )
                }
                is ApiResult.Loading -> {
                    // Loading state - 可以在这里显示加载指示器
                }
            }
        }
    }
    
    /**
     * 更新Profile UI显示
     */
    private fun updateProfileUI(profile: com.example.forhealth.network.dto.user.UserProfileResponse) {
        // 更新用户基本信息
        binding.tvUserName.text = profile.username
        profile.height?.toInt()?.let { 
            binding.tvHeight.text = it.toString() 
        } ?: run { 
            binding.tvHeight.text = "--" 
        }
        profile.weight?.toInt()?.let { 
            binding.tvWeight.text = it.toString() 
        } ?: run { 
            binding.tvWeight.text = "--" 
        }
        
        // 根据birthdate实时计算年龄
        val age = calculateAgeFromBirthdate(profile.birthdate)
        if (age != null) {
            binding.tvAge.text = age.toString()
        } else {
            // 如果没有birthdate，尝试使用profile.age（后端返回的）
            profile.age?.let {
                binding.tvAge.text = it.toString()
            } ?: run {
                binding.tvAge.text = "--"
            }
        }
        
        // 更新健康目标
        updateHealthGoals(
            activityLevel = profile.activity_level,
            targetWeight = profile.target_weight,
            goalPeriodWeeks = profile.goal_period_weeks
        )
    }
    
    /**
     * 根据birthdate计算年龄
     */
    private fun calculateAgeFromBirthdate(birthdate: String?): Int? {
        if (birthdate == null) return null
        
        return try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val birthDate = dateFormat.parse(birthdate)
            if (birthDate != null) {
                val today = java.util.Calendar.getInstance()
                val birth = java.util.Calendar.getInstance()
                birth.time = birthDate
                var age = today.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
                // 检查是否还没过生日
                if (today.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) {
                    age--
                }
                age
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun updateHealthGoals(
        activityLevel: String?,
        targetWeight: Double?,
        goalPeriodWeeks: Int?
    ) {
        // 格式化Activity Level显示
        val activityLevelText = when (activityLevel) {
            "sedentary" -> "Sedentary"
            "lightly_active" -> "Lightly Active"
            "moderately_active" -> "Moderately Active"
            "very_active" -> "Very Active"
            "extremely_active" -> "Extremely Active"
            else -> "--"
        }
        binding.tvActivityLevel.text = activityLevelText

        // 显示目标体重
        binding.tvTargetWeight.text = if (targetWeight != null) {
            "${targetWeight.toInt()} ${getString(R.string.kg)}"
        } else {
            "--"
        }

        // 显示目标周期（周数）
        binding.tvGoalPeriodWeeks.text = if (goalPeriodWeeks != null) {
            "$goalPeriodWeeks ${getString(R.string.weeks)}"
        } else {
            "--"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
