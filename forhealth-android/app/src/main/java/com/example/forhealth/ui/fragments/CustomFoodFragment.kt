package com.example.forhealth.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.forhealth.R
import com.example.forhealth.databinding.FragmentCustomFoodBinding
import com.example.forhealth.models.FoodItem
import com.example.forhealth.network.ApiResult
import com.example.forhealth.repositories.FoodRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CustomFoodFragment : DialogFragment() {
    
    private var _binding: FragmentCustomFoodBinding? = null
    private val binding get() = _binding!!
    
    private var onCustomFoodCreatedListener: ((FoodItem) -> Unit)? = null
    
    private val foodRepository = FoodRepository()
    
    fun setOnCustomFoodCreatedListener(listener: (FoodItem) -> Unit) {
        onCustomFoodCreatedListener = listener
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
        _binding = FragmentCustomFoodBinding.inflate(inflater, container, false)
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
        binding.etUnitName.setText("1 serving")
        binding.etWeightPerUnit.setText("100")
    }
    
    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            dismiss()
        }
    }
    
    private fun setupSaveButton() {
        binding.btnSaveCustomFood.setOnClickListener {
            saveCustomFood()
        }
    }
    
    private fun saveCustomFood() {
        val name = binding.etFoodName.text.toString().trim()
        if (name.isEmpty()) {
            Snackbar.make(binding.root, "请输入食物名称", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        val unitName = binding.etUnitName.text.toString().trim().takeIf { it.isNotEmpty() } ?: "1 serving"
        val weightPerUnit = binding.etWeightPerUnit.text.toString().toDoubleOrNull() ?: 100.0
        val calories = binding.etCalories.text.toString().toDoubleOrNull() ?: 0.0
        val protein = binding.etProtein.text.toString().toDoubleOrNull() ?: 0.0
        val carbs = binding.etCarbs.text.toString().toDoubleOrNull() ?: 0.0
        val fat = binding.etFat.text.toString().toDoubleOrNull() ?: 0.0
        
        // Create temporary FoodItem for API call
        val tempFood = FoodItem(
            id = "", // Will be set by backend
            name = name,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            unit = unitName,
            gramsPerUnit = weightPerUnit,
            image = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=150&q=80" // Generic placeholder
        )
        
        // Save to backend (or locally if API fails)
        lifecycleScope.launch {
            try {
                binding.btnSaveCustomFood.isEnabled = false
                binding.btnSaveCustomFood.text = "保存中..."
                
                // 调用Repository的createFood方法，需要传递多个参数
                val result = foodRepository.createFood(
                    name = tempFood.name,
                    servingSize = tempFood.gramsPerUnit,
                    calories = tempFood.calories,
                    protein = tempFood.protein,
                    carbohydrates = tempFood.carbs,
                    fat = tempFood.fat,
                    servingUnit = tempFood.unit,
                    imageUri = null
                )
                
                when (result) {
                    is ApiResult.Success -> {
                        // API成功，将FoodResponse转换为FoodItem
                        val foodResponse = result.data
                        val savedFood = FoodItem(
                            id = foodResponse.id,
                            name = foodResponse.name,
                            calories = foodResponse.nutrition_per_serving.calories,
                            protein = foodResponse.nutrition_per_serving.protein,
                            carbs = foodResponse.nutrition_per_serving.carbohydrates,
                            fat = foodResponse.nutrition_per_serving.fat,
                            unit = foodResponse.serving_unit,
                            gramsPerUnit = foodResponse.serving_size,
                            image = foodResponse.image_url ?: tempFood.image
                        )
                        onCustomFoodCreatedListener?.invoke(savedFood)
                        dismiss()
                    }
                    is ApiResult.Error -> {
                        // API失败，但先添加到本地列表（使用临时ID）
                        // 这样即使后端未对接，用户也能继续使用
                        val localFood = tempFood.copy(
                            id = "custom-${System.currentTimeMillis()}"
                        )
                        onCustomFoodCreatedListener?.invoke(localFood)
                        dismiss()
                        // 注意：这里不显示错误，因为用户说如果是前后端对接问题可以后面再说
                    }
                    is ApiResult.Loading -> {
                        // Loading state is already handled by button state
                    }
                }
            } catch (e: Exception) {
                // 异常情况，也先添加到本地列表
                val localFood = tempFood.copy(
                    id = "custom-${System.currentTimeMillis()}"
                )
                onCustomFoodCreatedListener?.invoke(localFood)
                dismiss()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

