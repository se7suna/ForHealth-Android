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

    private val viewModel: MainViewModel by activityViewModels()
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
        setupHeaderClick()
        setupMenuItems()
        setupAvatar()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        // 回到页面时刷新，确保编辑后的资料立即展示
        refreshProfile()
    }

    private fun setupHeaderClick() {
        val headerView = binding.root.findViewById<View>(R.id.profileHeader)
        headerView?.setOnClickListener { openEditProfileDialog() }
        binding.btnBack.setOnClickListener { dismiss() }
    }

    fun refreshProfile() {
        lifecycleScope.launch {
            when (val result = userRepository.getProfile()) {
                is ApiResult.Success -> updateProfileUI(result.data)
                is ApiResult.Error -> {
                    Toast.makeText(requireContext(), "刷新失败: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    private fun openEditProfileDialog() {
        val dialog = EditProfileFragment()
        // 使用 childFragmentManager，子 Fragment 保存成功后可通过 parentFragment 回调 refreshProfile()
        dialog.show(childFragmentManager, "EditProfileDialog")
    }

    private fun setupAvatar() {
        binding.ivAvatar.setImageResource(R.drawable.ic_user)
        binding.ivAvatar.load("https://api.dicebear.com/9.x/avataaars/svg?seed=Felix") {
            placeholder(R.drawable.ic_user)
            error(R.drawable.ic_user)
            transformations(CircleCropTransformation())
        }
    }

    private fun setupBackButton() {
        // back 点击已在 header 中处理
    }

    private fun setupMenuItems() {
        binding.btnEditAccount.setOnClickListener {
            lifecycleScope.launch {
                when (val result = userRepository.getProfile()) {
                    is ApiResult.Success -> {
                        val intent = Intent(requireContext(), EditAccountActivity::class.java)
                        intent.putExtra("username", result.data.username)
                        startActivity(intent)
                    }
                    is ApiResult.Error -> {
                        Toast.makeText(requireContext(), "获取用户信息失败: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        binding.btnWeightTracker.setOnClickListener { openWeightTrackerDialog() }

        binding.btnLogOut.setOnClickListener {
            TokenManager.clearTokens(requireContext())
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun openWeightTrackerDialog() {
        val dialog = WeightTrackerFragment()
        dialog.show(parentFragmentManager, "WeightTrackerDialog")
    }

    private fun openEditDataActivity(isRecordChanges: Boolean = false) {
        val intent = Intent(requireContext(), EditDataActivity::class.java)
        intent.putExtra("isRecordChanges", isRecordChanges)
        startActivity(intent)
    }

    private fun observeData() {
        lifecycleScope.launch {
            when (val result = userRepository.getProfile()) {
                is ApiResult.Success -> updateProfileUI(result.data)
                is ApiResult.Error -> {
                    Toast.makeText(requireContext(), "获取用户信息失败: ${result.message}", Toast.LENGTH_SHORT).show()
                    binding.tvUserName.text = "--"
                    binding.tvHeight.text = "--"
                    binding.tvWeight.text = "--"
                    binding.tvAge.text = "--"
                    updateHealthGoals(
                        activityLevel = null,
                        healthGoalType = null,
                        targetWeight = null,
                        goalPeriodWeeks = null
                    )
                }
                else -> {
                    // Loading state
                }
            }
        }
    }

    private fun updateProfileUI(profile: com.example.forhealth.network.dto.user.UserProfileResponse) {
        binding.tvUserName.text = profile.username
        profile.height?.toInt()?.let { binding.tvHeight.text = it.toString() } ?: run { binding.tvHeight.text = "--" }
        profile.weight?.toInt()?.let { binding.tvWeight.text = it.toString() } ?: run { binding.tvWeight.text = "--" }

        val age = calculateAgeFromBirthdate(profile.birthdate)
        if (age != null) {
            binding.tvAge.text = age.toString()
        } else {
            profile.age?.let { binding.tvAge.text = it.toString() } ?: run { binding.tvAge.text = "--" }
        }

        updateHealthGoals(
            activityLevel = profile.activity_level,
            healthGoalType = profile.health_goal_type,
            targetWeight = profile.target_weight,
            goalPeriodWeeks = profile.goal_period_weeks
        )
    }

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
                if (today.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) {
                    age--
                }
                age
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun updateHealthGoals(
        activityLevel: String?,
        healthGoalType: String?,
        targetWeight: Double?,
        goalPeriodWeeks: Int?
    ) {
        val activityLevelText = when (activityLevel) {
            "sedentary" -> "Sedentary"
            "lightly_active" -> "Lightly Active"
            "moderately_active" -> "Moderately Active"
            "very_active" -> "Very Active"
            "extremely_active" -> "Extremely Active"
            else -> "--"
        }
        binding.tvActivityLevel.text = activityLevelText

        val goalTypeText = when (healthGoalType) {
            "lose_weight" -> "减重"
            "gain_weight" -> "增重"
            "maintain_weight" -> "保持体重"
            else -> "--"
        }
        binding.tvHealthGoalType.text = goalTypeText

        binding.tvTargetWeight.text = if (targetWeight != null) {
            "${targetWeight.toInt()} ${getString(R.string.kg)}"
        } else {
            "--"
        }

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



