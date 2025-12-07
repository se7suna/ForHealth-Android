package com.example.forhealth.ui.fragments

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.forhealth.R
import com.example.forhealth.databinding.FragmentWeightTrackerBinding
import com.example.forhealth.models.WeightRecord
import com.example.forhealth.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

class WeightTrackerFragment : DialogFragment() {
    
    private var _binding: FragmentWeightTrackerBinding? = null
    private val binding get() = _binding!!
    
    private var currentWeight: Double = 72.0
    private var height: Int = 170 // in cm
    private var weightHistory: List<WeightRecord> = emptyList()
    private var onSaveListener: ((Double) -> Unit)? = null
    
    private var activeTab: Tab = Tab.OVERVIEW
    
    private enum class Tab {
        OVERVIEW, LOG
    }
    
    fun setCurrentWeight(weight: Double) {
        currentWeight = weight
    }
    
    fun setHeight(heightCm: Int) {
        height = heightCm
    }
    
    fun setWeightHistory(history: List<WeightRecord>) {
        weightHistory = history
    }
    
    fun setOnSaveListener(listener: (Double) -> Unit) {
        onSaveListener = listener
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
        _binding = FragmentWeightTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTabs()
        setupClickListeners()
        updateOverviewTab()
        updateLogTab()
    }
    
    private fun setupTabs() {
        updateTabButtons()
        
        binding.btnTabOverview.setOnClickListener {
            activeTab = Tab.OVERVIEW
            updateTabButtons()
            switchTab()
        }
        
        binding.btnTabLog.setOnClickListener {
            activeTab = Tab.LOG
            updateTabButtons()
            switchTab()
        }
    }
    
    private fun updateTabButtons() {
        val selectedColor = resources.getColor(R.color.slate_800, null)
        val unselectedColor = resources.getColor(R.color.text_secondary, null)
        val selectedBg = resources.getColor(R.color.white, null)
        val unselectedBg = android.graphics.Color.TRANSPARENT
        
        binding.btnTabOverview.apply {
            setTextColor(if (activeTab == Tab.OVERVIEW) selectedColor else unselectedColor)
            setBackgroundColor(if (activeTab == Tab.OVERVIEW) selectedBg else unselectedBg)
        }
        
        binding.btnTabLog.apply {
            setTextColor(if (activeTab == Tab.LOG) selectedColor else unselectedColor)
            setBackgroundColor(if (activeTab == Tab.LOG) selectedBg else unselectedBg)
        }
    }
    
    private fun switchTab() {
        if (activeTab == Tab.OVERVIEW) {
            binding.layoutOverview.visibility = View.VISIBLE
            binding.layoutLog.visibility = View.GONE
            binding.layoutBottomButton.visibility = View.GONE
        } else {
            binding.layoutOverview.visibility = View.GONE
            binding.layoutLog.visibility = View.VISIBLE
            binding.layoutBottomButton.visibility = View.VISIBLE
            binding.btnAction.text = getString(R.string.confirm)
            binding.btnAction.setOnClickListener {
                saveWeight()
            }
        }
    }
    
    private fun setupClickListeners() {
        // 设置滑块的步长
        binding.sliderWeight.stepSize = 0.1f
        
        binding.btnBack.setOnClickListener {
            dismiss()
        }
        
        binding.btnDecrease.setOnClickListener {
            val current = binding.sliderWeight.value
            if (current > 30f) {
                binding.sliderWeight.value = current - 0.1f
            }
        }
        
        binding.btnIncrease.setOnClickListener {
            val current = binding.sliderWeight.value
            if (current < 150f) {
                binding.sliderWeight.value = current + 0.1f
            }
        }
        
        binding.sliderWeight.addOnChangeListener { _, value, _ ->
            binding.tvWeightInput.text = String.format(Locale.getDefault(), "%.1f", value)
        }
    }
    
    private fun updateOverviewTab() {
        // 生成模拟数据用于测试
        val mockHistory = generateMockWeightHistory()
        
        // 更新当前体重
        binding.tvCurrentWeight.text = String.format(Locale.getDefault(), "%.1f", currentWeight)
        
        // 计算体重变化
        val startWeight = mockHistory.firstOrNull()?.weight ?: currentWeight
        val weightChange = currentWeight - startWeight
        val isGain = weightChange > 0
        
        binding.tvWeightChange.text = "${if (isGain) "+" else "-"}${String.format(Locale.getDefault(), "%.1f", Math.abs(weightChange))} kg"
        binding.tvWeightChange.setTextColor(resources.getColor(if (isGain) R.color.rose_300 else R.color.emerald_300, null))
        
        // 更新起始体重和目标体重
        binding.tvStartWeight.text = String.format(Locale.getDefault(), "%.1f kg", startWeight)
        binding.tvGoalWeight.text = "68.0 kg" // 可以从用户资料获取
        
        // 计算BMI
        val bmi = calculateBMI(currentWeight)
        binding.tvBMI.text = String.format(Locale.getDefault(), "%.1f", bmi)
        
        // 根据BMI健康范围（18.5-24.9）设置颜色：健康为绿色，否则为红色
        val isHealthyBMI = bmi >= 18.5 && bmi < 25.0
        binding.tvBMI.setTextColor(resources.getColor(if (isHealthyBMI) R.color.emerald_600 else R.color.rose_500, null))
        
        // 绘制体重趋势图（使用模拟数据）
        drawWeightChart(mockHistory)
        
        // 更新图表日期
        if (mockHistory.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val firstDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(mockHistory.first().date)
            binding.tvChartStartDate.text = if (firstDate != null) dateFormat.format(firstDate) else "Oct 1"
        }
    }
    
    private fun generateMockWeightHistory(): List<WeightRecord> {
        // 生成过去30天的模拟体重数据
        val calendar = Calendar.getInstance()
        val mockData = mutableListOf<WeightRecord>()
        
        for (i in 29 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            // 生成一个从75.0逐渐下降到72.0的体重趋势
            val weight = 75.0 - (i * 0.1) + (Math.random() * 0.5 - 0.25) // 添加一些随机波动
            mockData.add(WeightRecord(date = date, weight = weight))
        }
        
        return mockData
    }
    
    private fun updateLogTab() {
        // 设置滑块初始值
        binding.sliderWeight.value = currentWeight.toFloat()
        binding.tvWeightInput.text = String.format(Locale.getDefault(), "%.1f", currentWeight)
        
        // 更新日期（完整年月日格式）
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        binding.tvLogDate.text = dateFormat.format(Date())
    }
    
    private fun calculateBMI(weight: Double): Double {
        val heightM = height / 100.0
        return weight / (heightM * heightM)
    }
    
    private fun getBMICategory(bmi: Double): BMICategory {
        return when {
            bmi < 18.5 -> BMICategory(
                label = getString(R.string.underweight),
                colorRes = R.color.blue_500,
                bgRes = R.color.blue_50
            )
            bmi < 25 -> BMICategory(
                label = getString(R.string.healthy_weight),
                colorRes = R.color.emerald_700,
                bgRes = R.color.emerald_50
            )
            bmi < 30 -> BMICategory(
                label = getString(R.string.overweight),
                colorRes = R.color.orange_600,
                bgRes = R.color.orange_50
            )
            else -> BMICategory(
                label = getString(R.string.obese),
                colorRes = R.color.rose_500,
                bgRes = R.color.rose_50
            )
        }
    }
    
    private data class BMICategory(
        val label: String,
        val colorRes: Int,
        val bgRes: Int
    )
    
    private fun drawWeightChart(data: List<WeightRecord> = weightHistory) {
        if (data.size < 2) {
            binding.tvNoData.visibility = View.VISIBLE
            return
        }
        
        binding.tvNoData.visibility = View.GONE
        
        // 简单的图表绘制（后续可以用图表库实现）
        binding.viewWeightChart.post {
            val chartView = WeightChartView(requireContext())
            chartView.setData(data)
            binding.viewWeightChart.removeAllViews()
            binding.viewWeightChart.addView(chartView)
        }
    }
    
    private fun saveWeight() {
        val newWeight = binding.sliderWeight.value.toDouble()
        
        // 更新当前体重
        currentWeight = newWeight
        
        // 添加到历史记录
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val newRecord = WeightRecord(date = today, weight = newWeight)
        weightHistory = weightHistory + newRecord
        
        // 调用保存监听器
        onSaveListener?.invoke(newWeight)
        
        // 切换到Overview标签页
        activeTab = Tab.OVERVIEW
        updateTabButtons()
        switchTab()
        
        // 更新Overview显示
        updateOverviewTab()
        
        // 显示成功提示
        android.widget.Toast.makeText(
            requireContext(),
            getString(R.string.weight_added_successfully),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // 简单的体重图表View（后续可以用MPAndroidChart等库实现）
    private class WeightChartView(context: android.content.Context) : View(context) {
        private var data: List<WeightRecord> = emptyList()
        
        fun setData(weightData: List<WeightRecord>) {
            data = weightData
            invalidate()
        }
        
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            
            if (data.size < 2) return
            
            val width = width.toFloat()
            val height = height.toFloat()
            val padding = 40f
            
            val weights = data.map { it.weight }
            val minWeight = weights.minOrNull() ?: 0.0
            val maxWeight = weights.maxOrNull() ?: 100.0
            val weightRange = maxWeight - minWeight
            
            if (weightRange == 0.0) return
            
            // 绘制网格线
            val gridPaint = Paint().apply {
                color = resources.getColor(R.color.slate_200, null)
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
            canvas.drawLine(padding, height - padding, width - padding, height - padding, gridPaint)
            
            // 绘制折线
            val linePaint = Paint().apply {
                color = resources.getColor(R.color.emerald_500, null)
                strokeWidth = 3f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            
            val path = Path()
            data.forEachIndexed { index, record ->
                val x = padding + (index.toFloat() / (data.size - 1)) * (width - padding * 2)
                val y = height - padding - ((record.weight - minWeight) / weightRange).toFloat() * (height - padding * 2)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            canvas.drawPath(path, linePaint)
            
            // 绘制点
            val pointPaint = Paint().apply {
                color = resources.getColor(R.color.white, null)
                style = Paint.Style.FILL
            }
            val strokePaint = Paint().apply {
                color = resources.getColor(R.color.emerald_500, null)
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
            
            data.forEachIndexed { index, record ->
                val x = padding + (index.toFloat() / (data.size - 1)) * (width - padding * 2)
                val y = height - padding - ((record.weight - minWeight) / weightRange).toFloat() * (height - padding * 2)
                canvas.drawCircle(x, y, 4f, pointPaint)
                canvas.drawCircle(x, y, 4f, strokePaint)
            }
        }
    }
}

