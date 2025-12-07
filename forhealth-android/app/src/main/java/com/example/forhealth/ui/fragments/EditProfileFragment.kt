package com.example.forhealth.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.forhealth.R
import com.example.forhealth.databinding.FragmentEditProfileBinding
import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.user.UserProfileResponse
import com.example.forhealth.network.dto.user.UserProfileUpdate
import com.example.forhealth.network.safeApiCall
import com.example.forhealth.utils.DataMapper
import com.example.forhealth.utils.ProfileManager
import kotlinx.coroutines.launch

class EditProfileFragment : DialogFragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private var currentProfile: UserProfileResponse? = null
    private var selectedGender: String = "Male"
    private var selectedActivityLevel: String = "Moderately Active"

    private val activityLevels = arrayOf(
        "Sedentary",
        "Lightly Active",
        "Moderately Active",
        "Very Active",
        "Extremely Active"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
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
        setupAvatar()
        setupGenderButtons()
        setupActivityLevelSelector()
        setupSaveButton()
        loadProfileData()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            dismiss()
        }
    }

    private fun setupAvatar() {
        // 使用 dicebear API 生成头像
        binding.ivAvatar.load("https://api.dicebear.com/9.x/avataaars/svg?seed=Felix") {
            placeholder(R.color.slate_100)
            error(R.color.slate_100)
            transformations(CircleCropTransformation())
        }

        binding.flAvatar.setOnClickListener {
            // TODO: 实现头像更换功能（可能需要图片选择器）
            Toast.makeText(requireContext(), "头像更换功能待实现", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupGenderButtons() {
        binding.btnGenderMale.setOnClickListener {
            selectedGender = "Male"
            updateGenderButtonStyles()
        }

        binding.btnGenderFemale.setOnClickListener {
            selectedGender = "Female"
            updateGenderButtonStyles()
        }
    }

    private fun updateGenderButtonStyles() {
        val isMaleSelected = selectedGender == "Male"
        
        // Male button
        if (isMaleSelected) {
            binding.btnGenderMale.setBackgroundTintList(resources.getColorStateList(R.color.emerald_50, null))
            binding.btnGenderMale.setTextColor(resources.getColor(R.color.emerald_700, null))
            binding.btnGenderMale.strokeWidth = 2
            binding.btnGenderMale.strokeColor = resources.getColorStateList(R.color.emerald_500, null)
        } else {
            binding.btnGenderMale.setBackgroundTintList(resources.getColorStateList(R.color.white, null))
            binding.btnGenderMale.setTextColor(resources.getColor(R.color.slate_500, null))
            binding.btnGenderMale.strokeWidth = 1
            binding.btnGenderMale.strokeColor = resources.getColorStateList(R.color.slate_200, null)
        }

        // Female button
        if (!isMaleSelected) {
            binding.btnGenderFemale.setBackgroundTintList(resources.getColorStateList(R.color.emerald_50, null))
            binding.btnGenderFemale.setTextColor(resources.getColor(R.color.emerald_700, null))
            binding.btnGenderFemale.strokeWidth = 2
            binding.btnGenderFemale.strokeColor = resources.getColorStateList(R.color.emerald_500, null)
        } else {
            binding.btnGenderFemale.setBackgroundTintList(resources.getColorStateList(R.color.white, null))
            binding.btnGenderFemale.setTextColor(resources.getColor(R.color.slate_500, null))
            binding.btnGenderFemale.strokeWidth = 1
            binding.btnGenderFemale.strokeColor = resources.getColorStateList(R.color.slate_200, null)
        }
    }

    private fun setupActivityLevelSelector() {
        binding.etActivityLevel.setOnClickListener {
            showActivityLevelDialog()
        }
    }

    private fun showActivityLevelDialog() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            activityLevels
        )

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.activity_level))
            .setAdapter(adapter) { dialog, which ->
                selectedActivityLevel = activityLevels[which]
                binding.etActivityLevel.setText(selectedActivityLevel)
                dialog.dismiss()
            }
            .show()
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadProfileData() {
        // 先从本地获取数据
        val localProfile = ProfileManager.getProfile(requireContext())
        if (localProfile != null) {
            populateFields(localProfile)
        }

        // 尝试从API获取最新数据
        lifecycleScope.launch {
            val result = safeApiCall {
                RetrofitClient.apiService.getProfile()
            }
            when (result) {
                is ApiResult.Success -> {
                    currentProfile = result.data
                    populateFields(result.data)
                }
                is ApiResult.Error -> {
                    // 如果获取失败，使用本地数据
                    if (localProfile == null) {
                        Toast.makeText(
                            requireContext(),
                            "获取用户信息失败: ${result.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                is ApiResult.Loading -> {
                    // Loading state
                }
            }
        }
    }

    private fun populateFields(profile: UserProfileResponse) {
        // Display Name (使用username)
        binding.etDisplayName.setText(profile.username)

        // Age
        profile.age?.let {
            binding.etAge.setText(it.toString())
        }

        // Height
        profile.height?.toInt()?.let {
            binding.etHeight.setText(it.toString())
        }

        // Gender
        profile.gender?.let {
            selectedGender = when (it) {
                "male" -> "Male"
                "female" -> "Female"
                else -> "Male"
            }
            updateGenderButtonStyles()
        }

        // Activity Level
        profile.activity_level?.let {
            selectedActivityLevel = when (it) {
                "sedentary" -> "Sedentary"
                "lightly_active" -> "Lightly Active"
                "moderately_active" -> "Moderately Active"
                "very_active" -> "Very Active"
                "extremely_active" -> "Extremely Active"
                else -> "Moderately Active"
            }
            binding.etActivityLevel.setText(selectedActivityLevel)
        }
    }

    private fun saveProfile() {
        val displayName = binding.etDisplayName.text.toString().trim()
        val ageText = binding.etAge.text.toString().trim()
        val heightText = binding.etHeight.text.toString().trim()

        // 验证输入
        if (displayName.isEmpty()) {
            Toast.makeText(requireContext(), "请输入显示名称", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageText.toIntOrNull()
        if (age == null || age <= 0) {
            Toast.makeText(requireContext(), "请输入有效的年龄", Toast.LENGTH_SHORT).show()
            return
        }

        val height = heightText.toIntOrNull()
        if (height == null || height <= 0) {
            Toast.makeText(requireContext(), "请输入有效的身高", Toast.LENGTH_SHORT).show()
            return
        }

        // 转换数据格式
        val genderBackend = when (selectedGender) {
            "Male" -> "male"
            "Female" -> "female"
            else -> "male"
        }

        val activityLevelBackend = when (selectedActivityLevel) {
            "Sedentary" -> "sedentary"
            "Lightly Active" -> "lightly_active"
            "Moderately Active" -> "moderately_active"
            "Very Active" -> "very_active"
            "Extremely Active" -> "extremely_active"
            else -> "moderately_active"
        }

        // 构建更新请求（注意：UserProfileUpdate可能不支持age字段，需要通过birthdate计算）
        val updateRequest = UserProfileUpdate(
            username = displayName,
            height = height.toDouble(),
            gender = genderBackend,
            activity_level = activityLevelBackend
        )

        // 保存到本地
        val existingProfile = currentProfile ?: ProfileManager.getProfile(requireContext())
        val updatedProfile = existingProfile?.copy(
            username = displayName,
            height = height.toDouble(),
            age = age, // 保存到本地
            gender = genderBackend,
            activity_level = activityLevelBackend
        ) ?: UserProfileResponse(
            email = existingProfile?.email ?: "",
            username = displayName,
            height = height.toDouble(),
            age = age, // 保存到本地
            gender = genderBackend,
            activity_level = activityLevelBackend
        )
        ProfileManager.saveProfile(requireContext(), updatedProfile)

        // 尝试保存到后端
        lifecycleScope.launch {
            val loadingToast = Toast.makeText(requireContext(), "正在保存...", Toast.LENGTH_SHORT)
            loadingToast.show()

            val result = safeApiCall {
                RetrofitClient.apiService.updateProfile(updateRequest)
            }

            loadingToast.cancel()

            when (result) {
                is ApiResult.Success -> {
                    Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
                    // 通知父Fragment刷新数据
                    (parentFragment as? ProfileFragment)?.refreshProfile()
                    dismiss()
                }
                is ApiResult.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "已保存到本地，但同步到服务器失败: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    (parentFragment as? ProfileFragment)?.refreshProfile()
                    dismiss()
                }
                is ApiResult.Loading -> {
                    // Loading state
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

