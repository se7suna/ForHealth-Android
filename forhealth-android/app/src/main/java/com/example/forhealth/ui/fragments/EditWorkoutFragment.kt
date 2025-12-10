package com.example.forhealth.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.forhealth.R
import com.example.forhealth.databinding.FragmentAddExerciseBinding
import com.example.forhealth.models.*
import com.example.forhealth.ui.adapters.CartExerciseAdapter
import com.example.forhealth.ui.adapters.ExerciseListAdapter
import com.example.forhealth.utils.Constants
import com.example.forhealth.utils.DateUtils
import com.google.android.material.button.MaterialButton
import java.util.*

class EditWorkoutFragment : DialogFragment() {
    
    private var _binding: FragmentAddExerciseBinding? = null
    private val binding get() = _binding!!
    
    private var onWorkoutUpdatedListener: ((WorkoutGroup) -> Unit)? = null
    private var onWorkoutDeletedListener: ((String) -> Unit)? = null
    
    private val selectedItems = mutableListOf<SelectedExerciseItem>()
    private var selectedCategory: ExerciseType = ExerciseType.CARDIO
    private var filteredExercises: List<ExerciseItem> = Constants.EXERCISE_DB.filter { it.category == ExerciseType.CARDIO }
    private var isCartExpanded = false
    private var originalWorkoutGroup: WorkoutGroup? = null
    
    private lateinit var exerciseAdapter: ExerciseListAdapter
    private lateinit var cartAdapter: CartExerciseAdapter
    
    fun setOnWorkoutUpdatedListener(listener: (WorkoutGroup) -> Unit) {
        onWorkoutUpdatedListener = listener
    }
    
    fun setOnWorkoutDeletedListener(listener: (String) -> Unit) {
        onWorkoutDeletedListener = listener
    }
    
    fun setWorkoutGroup(workoutGroup: WorkoutGroup) {
        originalWorkoutGroup = workoutGroup
        
        // 将ActivityItem转换为SelectedExerciseItem
        selectedItems.clear()
        workoutGroup.activities.forEach { activity ->
            val exerciseItem = Constants.EXERCISE_DB.find { it.name == activity.name }
            if (exerciseItem != null) {
                selectedItems.add(SelectedExerciseItem(
                    exerciseItem = exerciseItem,
                    count = activity.duration.toDouble()
                ))
            }
        }
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
        _binding = FragmentAddExerciseBinding.inflate(inflater, container, false)
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
        
        // 更新标题
        val titleView = binding.root.findViewById<android.widget.TextView>(R.id.tvTitle)
        titleView?.text = "Edit Exercise"
        
        setupCloseButton()
        setupSearchBar()
        setupCategoryTabs()
        setupCreateCustomSportButton()
        setupExerciseList()
        setupCart()
        
        // 初始显示购物车
        if (selectedItems.isNotEmpty()) {
            updateCart()
            isCartExpanded = true
            toggleCartExpansion()
        }
    }
    
    private fun setupCloseButton() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterExercises(s?.toString() ?: "")
            }
        })
    }
    
    private fun setupCategoryTabs() {
        val categories = listOf(
            ExerciseType.CARDIO to "Cardio",
            ExerciseType.STRENGTH to "Strength",
            ExerciseType.FLEXIBILITY to "Flexibility",
            ExerciseType.SPORTS to "Sports"
        )
        
        binding.layoutCategories.removeAllViews()
        
        categories.forEach { (type, label) ->
            val button = MaterialButton(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = type == selectedCategory
                setOnClickListener {
                    selectedCategory = type
                    filterExercises(binding.etSearch.text.toString())
                    updateCategoryButtons()
                }
            }
            binding.layoutCategories.addView(button)
        }
        
        updateCategoryButtons()
    }
    
    private fun updateCategoryButtons() {
        for (i in 0 until binding.layoutCategories.childCount) {
            val button = binding.layoutCategories.getChildAt(i) as MaterialButton
            val type = when (i) {
                0 -> ExerciseType.CARDIO
                1 -> ExerciseType.STRENGTH
                2 -> ExerciseType.FLEXIBILITY
                else -> ExerciseType.SPORTS
            }
            button.isChecked = type == selectedCategory
            
            if (type == selectedCategory) {
                button.setBackgroundColor(resources.getColor(R.color.orange_500, null))
                button.setTextColor(resources.getColor(R.color.white, null))
            } else {
                button.setBackgroundColor(resources.getColor(R.color.slate_100, null))
                button.setTextColor(resources.getColor(R.color.slate_600, null))
            }
        }
    }
    
    private fun setupCreateCustomSportButton() {
        binding.btnCreateCustomSport.setOnClickListener {
            val customSportFragment = CustomSportFragment().apply {
                setOnCustomSportCreatedListener { exerciseItem ->
                    // 将自定义运动添加到购物车
                    addToCart(exerciseItem)
                    // 展开购物车
                    if (!isCartExpanded) {
                        toggleCartExpansion()
                    }
                }
            }
            customSportFragment.show(parentFragmentManager, "CustomSportFragment")
        }
    }
    
    private fun setupExerciseList() {
        exerciseAdapter = ExerciseListAdapter(
            exerciseList = filteredExercises,
            selectedItems = selectedItems,
            onAddClick = { exercise -> addToCart(exercise) }
        )
        
        binding.rvExerciseList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExerciseList.adapter = exerciseAdapter
    }
    
    private fun filterExercises(query: String) {
        filteredExercises = if (query.isBlank()) {
            Constants.EXERCISE_DB.filter { it.category == selectedCategory }
        } else {
            Constants.EXERCISE_DB.filter {
                it.name.contains(query, ignoreCase = true) &&
                (query.isNotBlank() || it.category == selectedCategory)
            }
        }
        updateExerciseList()
    }
    
    private fun updateExerciseList() {
        exerciseAdapter = ExerciseListAdapter(
            exerciseList = filteredExercises,
            selectedItems = selectedItems,
            onAddClick = { exercise -> addToCart(exercise) }
        )
        binding.rvExerciseList.adapter = exerciseAdapter
    }
    
    private fun addToCart(exercise: ExerciseItem) {
        val existing = selectedItems.find { it.exerciseItem.id == exercise.id }
        
        if (existing != null) {
            existing.count += 15.0
        } else {
            selectedItems.add(SelectedExerciseItem(
                exerciseItem = exercise,
                count = 30.0
            ))
        }
        
        updateExerciseList()
        updateCart()
    }
    
    private fun setupCart() {
        binding.layoutCartHeader.setOnClickListener {
            toggleCartExpansion()
        }
        
        binding.btnLogWorkoutCollapsed.setOnClickListener {
            saveWorkout()
        }
        
        binding.btnSave.setOnClickListener {
            saveWorkout()
        }
        
        binding.btnDeleteRecord.setOnClickListener {
            originalWorkoutGroup?.id?.let { workoutGroupId ->
                onWorkoutDeletedListener?.invoke(workoutGroupId)
            }
            dismiss()
        }
        
        cartAdapter = CartExerciseAdapter(
            items = selectedItems,
            onDurationChange = { exerciseId, delta -> updateDuration(exerciseId, delta) },
            onDurationInput = { exerciseId, value -> handleDurationInput(exerciseId, value) },
            onDurationBlur = { exerciseId -> handleDurationBlur(exerciseId) },
            onRemove = { exerciseId -> removeItem(exerciseId) },
            calculateCalories = { item -> item.exerciseItem.caloriesPerUnit * item.count }
        )
        
        binding.rvCartItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCartItems.adapter = cartAdapter
        
        updateCart()
    }
    
    private fun toggleCartExpansion() {
        isCartExpanded = !isCartExpanded
        
        if (isCartExpanded) {
            binding.scrollCartContent.visibility = View.VISIBLE
            binding.layoutCartFooter.visibility = View.VISIBLE
            binding.btnLogWorkoutCollapsed.visibility = View.GONE
            binding.ivCartChevron.setImageResource(R.drawable.ic_chevron_down)
        } else {
            binding.scrollCartContent.visibility = View.GONE
            binding.layoutCartFooter.visibility = View.GONE
            binding.btnLogWorkoutCollapsed.visibility = View.VISIBLE
            binding.ivCartChevron.setImageResource(R.drawable.ic_chevron_up)
        }
    }
    
    private fun updateDuration(exerciseId: String, delta: Double) {
        val item = selectedItems.find { it.exerciseItem.id == exerciseId } ?: return
        val minVal = 5.0
        val newCount = maxOf(minVal, item.count + delta)
        item.count = newCount
        
        updateCartAdapter()
        updateCart()
    }
    
    private fun handleDurationInput(exerciseId: String, value: String) {
        val item = selectedItems.find { it.exerciseItem.id == exerciseId } ?: return
        if (value.isBlank()) return
        val parsed = value.toDoubleOrNull()
        if (parsed != null && parsed > 0) {
            item.count = parsed
            updateCart()
        }
    }
    
    private fun handleDurationBlur(exerciseId: String) {
        val item = selectedItems.find { it.exerciseItem.id == exerciseId } ?: return
        val minVal = 5.0
        
        if (item.count <= 0) {
            item.count = minVal
        }
        
        updateCartAdapter()
        updateCart()
    }
    
    private fun removeItem(exerciseId: String) {
        selectedItems.removeAll { it.exerciseItem.id == exerciseId }
        if (selectedItems.isEmpty()) {
            isCartExpanded = false
        }
        updateCartAdapter()
        updateExerciseList()
        updateCart()
    }
    
    private fun updateCartAdapter() {
        cartAdapter = CartExerciseAdapter(
            items = selectedItems,
            onDurationChange = { exerciseId, delta -> updateDuration(exerciseId, delta) },
            onDurationInput = { exerciseId, value -> handleDurationInput(exerciseId, value) },
            onDurationBlur = { exerciseId -> handleDurationBlur(exerciseId) },
            onRemove = { exerciseId -> removeItem(exerciseId) },
            calculateCalories = { item -> item.exerciseItem.caloriesPerUnit * item.count }
        )
        binding.rvCartItems.adapter = cartAdapter
    }
    
    private fun updateCart() {
        if (selectedItems.isEmpty()) {
            // 隐藏购物车
            binding.layoutCart.visibility = View.GONE
            // 如果是编辑模式且购物车为空，显示独立的删除按钮
            if (originalWorkoutGroup != null) {
                binding.layoutDeleteButton.visibility = View.VISIBLE
            } else {
                binding.layoutDeleteButton.visibility = View.GONE
            }
        } else {
            // 显示购物车，隐藏删除按钮
            binding.layoutCart.visibility = View.VISIBLE
            binding.layoutDeleteButton.visibility = View.GONE
            
            val totalBurn = selectedItems.sumOf { it.exerciseItem.caloriesPerUnit * it.count }
            binding.tvCartTotalCalories.text = "${totalBurn.toInt()} kcal"
            
            if (selectedItems.size > 0) {
                binding.tvCartBadge.text = selectedItems.size.toString()
                binding.tvCartBadge.visibility = View.VISIBLE
            } else {
                binding.tvCartBadge.visibility = View.GONE
            }
            
            // 确保购物车内容区域在展开时可见
            if (isCartExpanded) {
                binding.scrollCartContent.visibility = View.VISIBLE
                binding.layoutCartFooter.visibility = View.VISIBLE
                binding.btnLogWorkoutCollapsed.visibility = View.GONE
            }
            
            // 编辑模式下显示"Add Exercise"，添加模式下显示"Confirm Workout"
            if (originalWorkoutGroup != null) {
                binding.btnSave.text = getString(R.string.log_workout)
            } else {
                binding.btnSave.text = getString(R.string.confirm_workout)
            }
            binding.btnSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.orange_500, null)
            ))
            
            // 确保"Add Exercise"按钮在收起状态下可见
            if (!isCartExpanded) {
                binding.btnLogWorkoutCollapsed.visibility = View.VISIBLE
            }
        }
    }
    
    private fun saveWorkout() {
        // 如果购物车为空，删除整个workout group
        if (selectedItems.isEmpty()) {
            originalWorkoutGroup?.id?.let { workoutGroupId ->
                onWorkoutDeletedListener?.invoke(workoutGroupId)
            }
            dismiss()
            return
        }
        
        val activities = selectedItems.map { item ->
            ActivityItem(
                id = UUID.randomUUID().toString(),
                name = item.exerciseItem.name,
                caloriesBurned = item.exerciseItem.caloriesPerUnit * item.count,
                duration = item.count.toInt(),
                time = originalWorkoutGroup?.time ?: DateUtils.getCurrentTime(),
                type = item.exerciseItem.category,
                image = item.exerciseItem.image
            )
        }
        
        val workoutGroup = WorkoutGroup(
            id = originalWorkoutGroup?.id ?: UUID.randomUUID().toString(),
            activities = activities,
            time = originalWorkoutGroup?.time ?: DateUtils.getCurrentTime()
        )
        
        onWorkoutUpdatedListener?.invoke(workoutGroup)
        dismiss()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

