package com.example.forhealth.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.forhealth.R
import com.example.forhealth.databinding.FragmentCustomSportBinding
import com.example.forhealth.models.ExerciseItem
import com.example.forhealth.models.ExerciseType
import com.example.forhealth.network.ApiResult
import com.example.forhealth.repositories.ExerciseRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CustomSportFragment : DialogFragment() {
    
    private var _binding: FragmentCustomSportBinding? = null
    private val binding get() = _binding!!
    
    private var onCustomSportCreatedListener: ((ExerciseItem) -> Unit)? = null
    
    private val exerciseRepository = ExerciseRepository()
    
    fun setOnCustomSportCreatedListener(listener: (ExerciseItem) -> Unit) {
        onCustomSportCreatedListener = listener
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_NoTitleBar_Fullscreen)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomSportBinding.inflate(inflater, container, false)
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
        setupSaveButton()
        
        // Set default values
        binding.etCaloriesPerMin.setText("5.0")
    }
    
    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            dismiss()
        }
    }
    
    private fun setupSaveButton() {
        binding.btnSaveCustomSport.setOnClickListener {
            saveCustomSport()
        }
    }
    
    private fun saveCustomSport() {
        val name = binding.etSportName.text.toString().trim()
        if (name.isEmpty()) {
            Snackbar.make(binding.root, "请输入运动名称", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        val describe = binding.etDescribe.text.toString().trim()
        if (describe.isEmpty()) {
            Snackbar.make(binding.root, "请输入运动描述", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        val caloriesPerMin = binding.etCaloriesPerMin.text.toString().toDoubleOrNull()
        if (caloriesPerMin == null || caloriesPerMin <= 0) {
            Snackbar.make(binding.root, "请输入有效的每分钟卡路里值（必须大于0）", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        // 将每分钟卡路里转换为METs
        // 公式：卡路里/分钟 = METs × 体重(kg) × 3.5 / 200
        // 假设标准体重70kg，所以：METs = (卡路里/分钟) × 200 / (3.5 × 70) = (卡路里/分钟) × 200 / 245
        val standardWeight = 70.0 // 标准体重70kg
        val mets = caloriesPerMin * 200.0 / (3.5 * standardWeight)
        
        if (mets <= 0) {
            Snackbar.make(binding.root, "计算出的运动强度无效", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        // 禁用保存按钮，防止重复提交
        binding.btnSaveCustomSport.isEnabled = false
        binding.btnSaveCustomSport.text = "创建中..."
        
        lifecycleScope.launch {
            val result = exerciseRepository.createSport(
                sportName = name,
                describe = describe,
                mets = mets,
                imageFile = null // 暂时不支持图片上传
            )
            
            when (result) {
                is ApiResult.Success -> {
                    // 创建成功后，获取创建的运动信息
                    // 由于后端返回的是SimpleSportsResponse，我们需要通过名称获取详细信息
                    val sportsResult = exerciseRepository.getAvailableSportsTypes()
                    when (sportsResult) {
                        is ApiResult.Success -> {
                            // 找到刚创建的运动
                            val createdSport = sportsResult.data.find { it.sport_name == name }
                            if (createdSport != null) {
                                // 转换为ExerciseItem
                                val exerciseItem = convertToExerciseItem(createdSport)
                                onCustomSportCreatedListener?.invoke(exerciseItem)
                                Toast.makeText(requireContext(), "自定义运动创建成功", Toast.LENGTH_SHORT).show()
                                dismiss()
                            } else {
                                // 如果找不到，使用基本信息创建ExerciseItem
                                val exerciseItem = ExerciseItem(
                                    id = name,
                                    name = name,
                                    caloriesPerUnit = caloriesPerMin,
                                    unit = "min",
                                    image = "",
                                    category = ExerciseType.CARDIO
                                )
                                onCustomSportCreatedListener?.invoke(exerciseItem)
                                Toast.makeText(requireContext(), "自定义运动创建成功", Toast.LENGTH_SHORT).show()
                                dismiss()
                            }
                        }
                        is ApiResult.Error -> {
                            // 即使获取列表失败，也使用基本信息创建ExerciseItem
                            val exerciseItem = ExerciseItem(
                                id = name,
                                name = name,
                                caloriesPerUnit = caloriesPerMin,
                                unit = "min",
                                image = "",
                                category = ExerciseType.CARDIO
                            )
                            onCustomSportCreatedListener?.invoke(exerciseItem)
                            Toast.makeText(requireContext(), "自定义运动创建成功", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                        is ApiResult.Loading -> {}
                    }
                }
                is ApiResult.Error -> {
                    binding.btnSaveCustomSport.isEnabled = true
                    binding.btnSaveCustomSport.text = getString(R.string.save)
                    Snackbar.make(
                        binding.root,
                        "创建失败: ${result.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                is ApiResult.Loading -> {}
            }
        }
    }
    
    /**
     * 将SearchSportsResponse转换为ExerciseItem
     */
    private fun convertToExerciseItem(dto: com.example.forhealth.network.dto.sports.SearchSportsResponse): ExerciseItem {
        val id = dto.sport_name ?: ""
        val name = dto.sport_name ?: "Sport"
        val category = when (dto.sport_type?.uppercase()) {
            "CARDIO" -> ExerciseType.CARDIO
            "STRENGTH" -> ExerciseType.STRENGTH
            "FLEXIBILITY" -> ExerciseType.FLEXIBILITY
            "SPORTS" -> ExerciseType.SPORTS
            else -> ExerciseType.CARDIO
        }
        val mets = dto.METs ?: 5.0
        // 将METs转换为每分钟消耗（假设70kg，公式：MET * 3.5 * 70 / 200）
        val caloriesPerMin = mets * 3.5 * 70 / 200.0
        val image = dto.image_url ?: ""
        
        return ExerciseItem(
            id = id,
            name = name,
            caloriesPerUnit = caloriesPerMin,
            unit = "min",
            image = image,
            category = category
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

