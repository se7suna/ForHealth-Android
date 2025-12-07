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
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.safeApiCall
import com.example.forhealth.ui.activities.EditAccountActivity
import com.example.forhealth.ui.activities.EditDataActivity
import com.example.forhealth.ui.activities.LoginActivity
import com.example.forhealth.utils.DataMapper
import com.example.forhealth.utils.ProfileManager
import com.example.forhealth.utils.TokenManager
import com.example.forhealth.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class ProfileFragment : DialogFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // 使用 activityViewModels 以共享同一个 ViewModel 实例
    private val viewModel: MainViewModel by activityViewModels()

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
        // 刷新Profile数据
        observeData()
    }

    private fun openEditProfileDialog() {
        val dialog = EditProfileFragment()
        dialog.show(parentFragmentManager, "EditProfileDialog")
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
                val result = safeApiCall {
                    RetrofitClient.apiService.getProfile()
                }
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

        // Edit Data - 已删除，现在通过点击Header打开EditProfile

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
        val localProfile = ProfileManager.getProfile(requireContext())
        val dialog = WeightTrackerFragment().apply {
            localProfile?.weight?.let { setCurrentWeight(it) }
            localProfile?.height?.toInt()?.let { setHeight(it) }
            // TODO: 从数据库或API获取体重历史记录
            setWeightHistory(emptyList())
            setOnSaveListener { newWeight ->
                // TODO: 保存体重记录到数据库或API
                // 这里可以更新本地profile或发送到后端
            }
        }
        dialog.show(parentFragmentManager, "WeightTrackerDialog")
    }

    private fun openEditDataActivity(isRecordChanges: Boolean = false) {
        // 先从本地获取数据
        val localProfile = ProfileManager.getProfile(requireContext())
        val intent = Intent(requireContext(), EditDataActivity::class.java)
        intent.putExtra("isRecordChanges", isRecordChanges)
        
        if (localProfile != null) {
            // 传递现有数据
            localProfile.height?.toInt()?.let { intent.putExtra("height", it) }
            localProfile.weight?.toInt()?.let { intent.putExtra("weight", it) }
            localProfile.gender?.let { 
                intent.putExtra("gender", DataMapper.genderFromBackend(it))
            }
            DataMapper.birthDateFromBackend(localProfile.birthdate)?.let { (year, month, day) ->
                intent.putExtra("birthYear", year)
                intent.putExtra("birthMonth", month)
                intent.putExtra("birthDay", day)
            }
            localProfile.activity_level?.let {
                intent.putExtra("activityLevel", DataMapper.activityLevelFromBackend(it))
            }
            localProfile.health_goal_type?.let {
                intent.putExtra("goalType", DataMapper.goalTypeFromBackend(it))
            }
            localProfile.target_weight?.toInt()?.let { intent.putExtra("goalWeight", it) }
            localProfile.goal_period_weeks?.let { intent.putExtra("goalWeeks", it) }
        }
        
        startActivity(intent)
        
        // 尝试从API获取（如果后端可用）
        lifecycleScope.launch {
            val result = safeApiCall {
                RetrofitClient.apiService.getProfile()
            }
            when (result) {
                is ApiResult.Success -> {
                    ProfileManager.saveProfile(requireContext(), result.data)
                }
                is ApiResult.Error -> {
                    // 使用本地数据，不显示错误
                }
                is ApiResult.Loading -> {
                    // Loading state
                }
            }
        }
    }

    private fun observeData() {
        // 先从本地获取用户资料数据
        val localProfile = ProfileManager.getProfile(requireContext())
        if (localProfile != null) {
            // 更新用户信息
            binding.tvUserName.text = localProfile.username
            localProfile.height?.toInt()?.let { binding.tvHeight.text = it.toString() }
            localProfile.weight?.toInt()?.let { binding.tvWeight.text = it.toString() }
            localProfile.age?.let { binding.tvAge.text = it.toString() }
            
            // 更新健康目标
            updateHealthGoals(
                activityLevel = localProfile.activity_level,
                targetWeight = localProfile.target_weight,
                goalPeriodWeeks = localProfile.goal_period_weeks
            )
        }
        
        // 尝试从API获取用户资料数据并显示（如果后端可用）
        lifecycleScope.launch {
            val result = safeApiCall {
                RetrofitClient.apiService.getProfile()
            }
            when (result) {
                is ApiResult.Success -> {
                    val profile = result.data
                    // 保存到本地
                    ProfileManager.saveProfile(requireContext(), profile)
                    // 更新用户信息
                    binding.tvUserName.text = profile.username
                    profile.height?.toInt()?.let { binding.tvHeight.text = it.toString() }
                    profile.weight?.toInt()?.let { binding.tvWeight.text = it.toString() }
                    profile.age?.let { binding.tvAge.text = it.toString() }
                    
                    // 更新健康目标
                    updateHealthGoals(
                        activityLevel = profile.activity_level,
                        targetWeight = profile.target_weight,
                        goalPeriodWeeks = profile.goal_period_weeks
                    )
                }
                is ApiResult.Error -> {
                    // 如果获取失败，使用本地数据或默认值
                    if (localProfile == null) {
                        updateHealthGoals(
                            activityLevel = "moderately_active",
                            targetWeight = 70.0,
                            goalPeriodWeeks = 12
                        )
                    }
                }
                is ApiResult.Loading -> {
                    // Loading state
                }
            }
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
