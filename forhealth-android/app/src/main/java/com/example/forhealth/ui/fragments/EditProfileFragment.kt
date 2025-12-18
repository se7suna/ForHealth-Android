package com.example.forhealth.ui.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.example.forhealth.ui.activities.MainActivity
import com.example.forhealth.utils.ProfileManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditProfileFragment : DialogFragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val goMainOnSave by lazy {
        arguments?.getBoolean(ARG_GO_MAIN_ON_SAVE, false) ?: false
    }

    private var currentProfile: UserProfileResponse? = null
    private var selectedGender: String = "Male"
    private var selectedActivityLevel: String = "Moderately Active"
    private var selectedBirthdate: String? = null
    private var selectedTargetWeight: Double? = null
    private var selectedGoalPeriod: Int? = null
    private var selectedWeight: Double? = null
    private var selectedGoalType: String = "maintain_weight"

    private val userRepository = UserRepository()
    private val activityLevels = arrayOf(
        "Sedentary",
        "Lightly Active",
        "Moderately Active",
        "Very Active",
        "Extremely Active"
    )
    private val goalTypes = arrayOf(
        "减重",
        "增重",
        "保持体重"
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private const val ARG_GO_MAIN_ON_SAVE = "arg_go_main_on_save"

        fun newInstance(goMainOnSave: Boolean = false): EditProfileFragment {
            return EditProfileFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_GO_MAIN_ON_SAVE, goMainOnSave)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_NoTitleBar_Fullscreen)
        // 如果是从登录页面跳转过来的，不允许通过返回键关闭
        val shouldGoMainOnSave = arguments?.getBoolean(ARG_GO_MAIN_ON_SAVE, false) ?: false
        isCancelable = !shouldGoMainOnSave
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
        setupSystemBackButton()
        setupAvatar()
        setupGenderButtons()
        setupActivityLevelSelector()
        setupGoalTypeSelector()
        setupBirthdateSelector()
        setupTargetWeightSelector()
        setupGoalPeriodSelector()
        setupSaveButton()
        loadProfileData()
    }
    
    private fun setupSystemBackButton() {
        // 处理系统返回键
        if (goMainOnSave) {
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // 显示提示对话框
                    showBackPressDialog()
                }
            }
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        }
    }
    
    private fun showBackPressDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("提示")
            .setMessage("请填写完整的个人信息后才能进入软件主页")
            .setPositiveButton("继续填写") { _, _ ->
                // 不关闭对话框，继续填写
            }
            .setNegativeButton("退出登录") { _, _ ->
                // 退出登录，清除token
                com.example.forhealth.utils.TokenManager.clearTokens(requireContext())
                val intent = Intent(requireContext(), com.example.forhealth.ui.activities.LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            if (goMainOnSave) {
                // 如果是从登录页面跳转过来的，提示用户需要填写个人信息
                AlertDialog.Builder(requireContext())
                    .setTitle("提示")
                    .setMessage("请填写完整的个人信息后才能进入软件主页")
                    .setPositiveButton("继续填写") { _, _ ->
                        // 不关闭对话框，继续填写
                    }
                    .setNegativeButton("退出登录") { _, _ ->
                        // 退出登录，清除token
                        com.example.forhealth.utils.TokenManager.clearTokens(requireContext())
                        val intent = Intent(requireContext(), com.example.forhealth.ui.activities.LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                dismiss()
            }
        }
        
        // 处理系统返回键
        isCancelable = !goMainOnSave
    }

    private fun setupAvatar() {
        binding.ivAvatar.setImageResource(R.drawable.ic_user)
        binding.ivAvatar.load("https://api.dicebear.com/9.x/avataaars/svg?seed=Felix") {
            placeholder(R.drawable.ic_user)
            error(R.drawable.ic_user)
            transformations(CircleCropTransformation())
        }

        binding.flAvatar.setOnClickListener {
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

    private fun setupGoalTypeSelector() {
        binding.etGoalType.setOnClickListener {
            showGoalTypeDialog()
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

    private fun showGoalTypeDialog() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            goalTypes
        )

        AlertDialog.Builder(requireContext())
            .setTitle("健康目标")
            .setAdapter(adapter) { dialog, which ->
                val selected = goalTypes[which]
                selectedGoalType = when (selected) {
                    "减重" -> "lose_weight"
                    "增重" -> "gain_weight"
                    else -> "maintain_weight"
                }
                binding.etGoalType.setText(selected)
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
        val calendar = Calendar.getInstance()
        selectedBirthdate?.let { dateStr ->
            try {
                val date = dateFormat.parse(dateStr)
                if (date != null) {
                    calendar.time = date
                }
            } catch (_: Exception) {
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
        val localProfile = ProfileManager.getProfile(requireContext())
        if (localProfile != null) {
            currentProfile = localProfile
            populateFields(localProfile)
        }

        lifecycleScope.launch {
            val result = userRepository.getProfile()
            when (result) {
                is ApiResult.Success -> {
                    currentProfile = result.data
                    populateFields(result.data)
                }
                is ApiResult.Error -> {
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
        binding.etDisplayName.setText(profile.username ?: "")

        profile.birthdate?.let {
            selectedBirthdate = it
            binding.etBirthdate.setText(it)
        }

        profile.height?.toInt()?.let {
            binding.etHeight.setText(it.toString())
        }
        profile.weight?.let {
            selectedWeight = it
            binding.etWeight.setText(it.toString())
        }

        profile.gender?.let {
            selectedGender = when (it) {
                "male" -> "Male"
                "female" -> "Female"
                else -> "Male"
            }
            updateGenderButtonStyles()
        }

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

        profile.target_weight?.let {
            selectedTargetWeight = it
            binding.etTargetWeight.setText("${it.toInt()} kg")
        }

        profile.goal_period_weeks?.let {
            selectedGoalPeriod = it
            binding.etGoalPeriod.setText("$it weeks")
        }

        profile.health_goal_type?.let {
            selectedGoalType = it
            val display = when (it) {
                "lose_weight" -> "减重"
                "gain_weight" -> "增重"
                else -> "保持体重"
            }
            binding.etGoalType.setText(display)
        }
    }

    private fun saveProfile() {
        val displayName = binding.etDisplayName.text.toString().trim()
        val heightText = binding.etHeight.text.toString().trim()
        val weightText = binding.etWeight.text.toString().trim()

        val profileSnapshot = currentProfile
        if (profileSnapshot == null) {
            Toast.makeText(requireContext(), "正在加载用户数据，请稍候...", Toast.LENGTH_SHORT).show()
            return
        }

        val genderBackend = when (selectedGender) {
            "Male" -> "male"
            "Female" -> "female"
            else -> profileSnapshot.gender
        }

        val activityLevelBackend = when (selectedActivityLevel) {
            "Sedentary" -> "sedentary"
            "Lightly Active" -> "lightly_active"
            "Moderately Active" -> "moderately_active"
            "Very Active" -> "very_active"
            "Extremely Active" -> "extremely_active"
            else -> profileSnapshot.activity_level
        }

        val resolvedHeight = heightText.toDoubleOrNull() ?: profileSnapshot.height
        val resolvedWeight = weightText.toDoubleOrNull() ?: profileSnapshot.weight
        val resolvedBirthdate = selectedBirthdate ?: profileSnapshot.birthdate
        val resolvedTargetWeight = selectedTargetWeight ?: profileSnapshot.target_weight
        val resolvedGoalPeriod = selectedGoalPeriod ?: profileSnapshot.goal_period_weeks

        // 验证身高值是否合理（50-250厘米）
        if (resolvedHeight != null) {
            if (resolvedHeight < 50 || resolvedHeight > 250) {
                Toast.makeText(requireContext(), "身高值不合理，请输入50-250厘米之间的值", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 验证体重值是否合理（20-300公斤）
        if (resolvedWeight != null) {
            if (resolvedWeight < 20 || resolvedWeight > 300) {
                Toast.makeText(requireContext(), "体重值不合理，请输入20-300公斤之间的值", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val requiredFieldsFilled = displayName.isNotEmpty() &&
                !resolvedBirthdate.isNullOrBlank() &&
                (resolvedHeight ?: 0.0) > 0 &&
                (resolvedWeight ?: 0.0) > 0 &&
                !genderBackend.isNullOrBlank() &&
                !activityLevelBackend.isNullOrBlank() &&
                selectedGoalType.isNotBlank() &&
                (resolvedTargetWeight ?: 0.0) > 0 &&
                (resolvedGoalPeriod ?: 0) > 0

        if (!requiredFieldsFilled) {
            Toast.makeText(requireContext(), "请完整填写所有字段", Toast.LENGTH_SHORT).show()
            return
        }

        val updateRequest = UserProfileUpdate(
            username = displayName.takeIf { it.isNotEmpty() } ?: profileSnapshot.username,
            height = resolvedHeight,
            weight = resolvedWeight,
            birthdate = resolvedBirthdate,
            gender = genderBackend,
            activity_level = activityLevelBackend,
            health_goal_type = selectedGoalType,
            target_weight = resolvedTargetWeight,
            goal_period_weeks = resolvedGoalPeriod,
            liked_foods = profileSnapshot.liked_foods,
            disliked_foods = profileSnapshot.disliked_foods,
            allergies = profileSnapshot.allergies,
            dietary_restrictions = profileSnapshot.dietary_restrictions,
            preferred_tastes = profileSnapshot.preferred_tastes,
            cooking_skills = profileSnapshot.cooking_skills,
            budget_per_day = profileSnapshot.budget_per_day,
            include_budget = profileSnapshot.include_budget
        )

        lifecycleScope.launch {
            val loadingToast = Toast.makeText(requireContext(), "正在保存...", Toast.LENGTH_SHORT)
            loadingToast.show()

            val result = userRepository.updateProfile(updateRequest)

            loadingToast.cancel()

            when (result) {
                is ApiResult.Success -> {
                    currentProfile = result.data
                    ProfileManager.saveProfile(requireContext(), result.data)

                    Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
                    if (goMainOnSave) {
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        (parentFragment as? ProfileFragment)?.refreshProfile()
                        dismiss()
                    }
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

