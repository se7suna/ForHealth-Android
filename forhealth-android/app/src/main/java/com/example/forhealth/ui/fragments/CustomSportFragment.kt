package com.example.forhealth.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.forhealth.R
import com.example.forhealth.databinding.FragmentCustomSportBinding
import com.example.forhealth.models.ExerciseItem
import com.example.forhealth.models.ExerciseType
import com.google.android.material.snackbar.Snackbar

class CustomSportFragment : DialogFragment() {
    
    private var _binding: FragmentCustomSportBinding? = null
    private val binding get() = _binding!!
    
    private var onCustomSportCreatedListener: ((ExerciseItem) -> Unit)? = null
    
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
        
        val caloriesPerMin = binding.etCaloriesPerMin.text.toString().toDoubleOrNull() ?: 5.0
        if (caloriesPerMin <= 0) {
            Snackbar.make(binding.root, "每分钟卡路里必须大于0", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        // Create ExerciseItem
        val customSport = ExerciseItem(
            id = "custom-${System.currentTimeMillis()}",
            name = name,
            caloriesPerUnit = caloriesPerMin,
            unit = "min",
            image = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=150&q=80", // Generic placeholder
            category = ExerciseType.CARDIO // Default category
        )
        
        onCustomSportCreatedListener?.invoke(customSport)
        dismiss()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

