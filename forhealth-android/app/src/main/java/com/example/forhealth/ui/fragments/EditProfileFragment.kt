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
import com.example.forhealth.network.dto.user.UserProfileResponse
import com.example.forhealth.network.dto.user.UserProfileUpdate
import com.example.forhealth.repositories.UserRepository
import com.example.forhealth.utils.DataMapper
import com.example.forhealth.utils.ProfileManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditProfileFragment : DialogFragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private var currentProfile: UserProfileResponse? = null
    private var selectedGender: String = "Male"
    private var selectedActivityLevel: String = "Moderately Active"
    private var selectedBirthdate: String? = null
    private var selectedTargetWeight: Double? = null
    private var selectedGoalPeriod: Int? = null
    
    // 用户数据仓库
    private val userRepository = UserRepository()

    private val activityLevels = arrayOf(
        "Sedentary",
        "Lightly Active",
        "Moderately Active",
        "Very Active",
        "Extremely Active"
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
        setupBirthdateSelector()
        setupTargetWeightSelector()
        setupGoalPeriodSelector()
        setupSaveButton()
        loadProfileData()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            dismiss()
        }
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

    private fun setupBirthdateSelector() {
        binding.etBirthdate.setOnClickListener {
            showBirthdatePicker()
        }
    }

    private fun showBirthdatePicker() {
        // 解析当前日期
        val calendar = Calendar.getInstance()
        selectedBirthdate?.let { dateStr ->
            try {
                val date = dateFormat.parse(dateStr)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                // 解析失败，使用默认值
                calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 25)
            }
        } ?: run {
            calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 25)
        }

        val dialog = BirthdatePickerDialogFragment.newInstance(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.setOnDateSelectedListener { year, month, day ->
            selectedBirthdate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day)
            binding.etBirthdate.setText(selectedBirthdate)
        }

        dialog.show(parentFragmentManager, "BirthdatePickerDialog")
    }

    private fun setupTargetWeightSelector() {
        binding.etTargetWeight.setOnClickListener {
            showTargetWeightPicker()
        }
    }

    private fun showTargetWeightPicker() {
        val currentValue = selectedTargetWeight?.toInt() ?: 70
        val dialog = NumberPickerDialogFragment.newInstance(
            min = 20,
            max = 300,
            current = currentValue,
            unit = "kg",
            prompt = "请选择目标体重"
        )

        dialog.setOnValueSelectedListener { value ->
            selectedTargetWeight = value.toDouble()
            binding.etTargetWeight.setText("$value kg")
        }

        dialog.show(parentFragmentManager, "TargetWeightPickerDialog")
    }

    private fun setupGoalPeriodSelector() {
        binding.etGoalPeriod.setOnClickListener {
            showGoalPeriodPicker()
        }
    }

    private fun showGoalPeriodPicker() {
        val currentValue = selectedGoalPeriod ?: 12
        val dialog = NumberPickerDialogFragment.newInstance(
            min = 1,
            max = 104,
            current = currentValue,
            unit = "周",
            prompt = "请选择目标周期"
        )

        dialog.setOnValueSelectedListener { value ->
            selectedGoalPeriod = value
            binding.etGoalPeriod.setText("$value 周")
        }

        dialog.show(parentFragmentManager, "GoalPeriodPickerDialog")
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
            currentProfile = localProfile
            populateFields(localProfile)
        }

        // 尝试从API获取最新数据
        lifecycleScope.launch {
            val result = userRepository.getProfile()
            when (result) {
                is ApiResult.Success -> {
                    currentProfile = result.data
                    populateFields(result.data)
                }
                is ApiResult.Error -> {
                    // 如果获取失败，使用本地数据（如果存在）
                    if (currentProfile == null) {
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
        binding.etDisplayName.setText(profile.username ?: "")

        // Birthdate
        profile.birthdate?.let {
            selectedBirthdate = it
            binding.etBirthdate.setText(it)
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

        // Target Weight
        profile.target_weight?.let {
            selectedTargetWeight = it
            binding.etTargetWeight.setText("${it.toInt()} kg")
        }

        // Goal Period
        profile.goal_period_weeks?.let {
            selectedGoalPeriod = it
            binding.etGoalPeriod.setText("$it weeks")
        }
    }

    private fun saveProfile() {
        val displayName = binding.etDisplayName.text.toString().trim()
        val heightText = binding.etHeight.text.toString().trim()

        // 验证输入
        if (displayName.isEmpty()) {
            Toast.makeText(requireContext(), "请输入用户名", Toast.LENGTH_SHORT).show()
            return
        }

        // 确保从后端读取了数据
        if (currentProfile == null) {
            Toast.makeText(requireContext(), "正在加载用户数据，请稍候...", Toast.LENGTH_SHORT).show()
            return
        }

        // 转换数据格式
        val genderBackend = when (selectedGender) {
            "Male" -> "male"
            "Female" -> "female"
            else -> currentProfile?.gender
        }

        val activityLevelBackend = when (selectedActivityLevel) {
            "Sedentary" -> "sedentary"
            "Lightly Active" -> "lightly_active"
            "Moderately Active" -> "moderately_active"
            "Very Active" -> "very_active"
            "Extremely Active" -> "extremely_active"
            else -> currentProfile?.activity_level
        }

        // 使用从后端读取的数据填充未修改的字段
        val updateRequest = UserProfileUpdate(
            username = displayName.takeIf { it.isNotEmpty() } ?: currentProfile?.username,
            height = heightText.toDoubleOrNull() ?: currentProfile?.height,
            weight = currentProfile?.weight, // 使用读取到的数据
            birthdate = selectedBirthdate ?: currentProfile?.birthdate,
            gender = genderBackend,
            activity_level = activityLevelBackend,
            health_goal_type = currentProfile?.health_goal_type, // 使用读取到的数据
            target_weight = selectedTargetWeight ?: currentProfile?.target_weight,
            goal_period_weeks = selectedGoalPeriod ?: currentProfile?.goal_period_weeks,
            liked_foods = currentProfile?.liked_foods, // 使用读取到的数据
            disliked_foods = currentProfile?.disliked_foods, // 使用读取到的数据
            allergies = currentProfile?.allergies, // 使用读取到的数据
            dietary_restrictions = currentProfile?.dietary_restrictions, // 使用读取到的数据
            preferred_tastes = currentProfile?.preferred_tastes, // 使用读取到的数据
            cooking_skills = currentProfile?.cooking_skills, // 使用读取到的数据
            budget_per_day = currentProfile?.budget_per_day, // 使用读取到的数据
            include_budget = currentProfile?.include_budget // 使用读取到的数据
        )

        // 尝试保存到后端
        lifecycleScope.launch {
            val loadingToast = Toast.makeText(requireContext(), "正在保存...", Toast.LENGTH_SHORT)
            loadingToast.show()

            val result = userRepository.updateProfile(updateRequest)

            loadingToast.cancel()

            when (result) {
                is ApiResult.Success -> {
                    // 更新本地缓存
                    currentProfile = result.data
                    ProfileManager.saveProfile(requireContext(), result.data)
                    
                    Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
                    // 通知父Fragment刷新数据
                    (parentFragment as? ProfileFragment)?.refreshProfile()
                    dismiss()
                }
                is ApiResult.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "保存失败: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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

