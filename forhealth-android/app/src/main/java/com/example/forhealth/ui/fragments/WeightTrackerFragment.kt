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
        // 确保体重值在有效范围内（30-150 kg）
        currentWeight = weight.coerceIn(30.0, 150.0)
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
                // 使用BigDecimal避免浮点数精度问题，确保值符合stepSize要求
                val newValue = (current * 10 - 1).toInt() / 10.0f
                binding.sliderWeight.value = newValue.coerceAtLeast(30f)
            }
        }
        
        binding.btnIncrease.setOnClickListener {
            val current = binding.sliderWeight.value
            if (current < 150f) {
                // 使用BigDecimal避免浮点数精度问题，确保值符合stepSize要求
                val newValue = (current * 10 + 1).toInt() / 10.0f
                binding.sliderWeight.value = newValue.coerceAtMost(150f)
            }
        }
        
        binding.sliderWeight.addOnChangeListener { _, value, _ ->
            binding.tvWeightInput.text = String.format(Locale.getDefault(), "%.1f", value)
        }
    }
    
    private var chartStartDate: Date? = null
    private var chartEndDate: Date? = null
    
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
        
        // 初始化日期范围
        if (chartStartDate == null && mockHistory.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            chartStartDate = dateFormat.parse(mockHistory.first().date)
        }
        if (chartEndDate == null) {
            chartEndDate = Date()
        }
        
        // 绘制体重趋势图（根据日期范围过滤和采样数据）
        val filteredData = filterAndSampleData(mockHistory)
        drawWeightChart(filteredData)
        
        // 更新图表日期显示
        updateChartDateDisplay()
        
        // 设置日期点击事件
        setupDateClickListeners()
    }
    
    private fun updateChartDateDisplay() {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        binding.tvChartStartDate.text = chartStartDate?.let { dateFormat.format(it) } ?: "Oct 1, 2024"
        val todayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        binding.tvChartEndDate.text = chartEndDate?.let { 
            if (isToday(it)) "Today" else dateFormat.format(it)
        } ?: "Today"
    }
    
    private fun isToday(date: Date): Boolean {
        val calendar1 = Calendar.getInstance()
        val calendar2 = Calendar.getInstance()
        calendar1.time = date
        calendar2.time = Date()
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
               calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun setupDateClickListeners() {
        binding.tvChartStartDate.setOnClickListener {
            openDatePicker(true)
        }
        
        binding.tvChartEndDate.setOnClickListener {
            openDatePicker(false)
        }
    }
    
    private fun openDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val currentDate = if (isStartDate) chartStartDate else chartEndDate
        currentDate?.let { calendar.time = it }
        
        val dialog = BirthdatePickerDialogFragment.newInstance(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        dialog.setOnDateSelectedListener { year, month, day ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month - 1, day)
            val selectedDate = selectedCalendar.time
            
            if (isStartDate) {
                chartStartDate = selectedDate
                // 确保起始日期不晚于结束日期
                chartEndDate?.let { endDate ->
                    if (selectedDate.after(endDate)) {
                        chartEndDate = selectedDate
                    }
                }
            } else {
                chartEndDate = selectedDate
                // 确保结束日期不早于起始日期
                chartStartDate?.let { startDate ->
                    if (selectedDate.before(startDate)) {
                        chartStartDate = selectedDate
                    }
                }
            }
            
            updateChartDateDisplay()
            // 重新绘制图表（根据日期范围过滤和采样数据）
            val filteredData = filterAndSampleData(weightHistory)
            drawWeightChart(filteredData)
        }
        
        dialog.show(parentFragmentManager, "DatePickerDialog")
    }
    
    /**
     * 根据选择的日期范围过滤数据，并动态调整点的密度
     * 如果日期范围很大，则采样数据以减少点的数量
     */
    private fun filterAndSampleData(data: List<WeightRecord>): List<WeightRecord> {
        if (data.isEmpty()) return data
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // 确定日期范围
        val startDate = chartStartDate ?: run {
            // 如果没有设置起始日期，使用数据的第一条记录
            try {
                dateFormat.parse(data.first().date)
            } catch (e: Exception) {
                null
            }
        }
        val endDate = chartEndDate ?: Date()
        
        if (startDate == null) return data
        
        // 过滤出日期范围内的数据（包含边界）
        val filtered = data.filter { record ->
            try {
                val recordDate = dateFormat.parse(record.date)
                if (recordDate == null) return@filter false
                
                // 使用Calendar来比较日期（忽略时间部分）
                val cal1 = Calendar.getInstance()
                cal1.time = recordDate
                cal1.set(Calendar.HOUR_OF_DAY, 0)
                cal1.set(Calendar.MINUTE, 0)
                cal1.set(Calendar.SECOND, 0)
                cal1.set(Calendar.MILLISECOND, 0)
                
                val cal2 = Calendar.getInstance()
                cal2.time = startDate
                cal2.set(Calendar.HOUR_OF_DAY, 0)
                cal2.set(Calendar.MINUTE, 0)
                cal2.set(Calendar.SECOND, 0)
                cal2.set(Calendar.MILLISECOND, 0)
                
                val cal3 = Calendar.getInstance()
                cal3.time = endDate
                cal3.set(Calendar.HOUR_OF_DAY, 0)
                cal3.set(Calendar.MINUTE, 0)
                cal3.set(Calendar.SECOND, 0)
                cal3.set(Calendar.MILLISECOND, 0)
                
                !cal1.before(cal2) && !cal1.after(cal3)
            } catch (e: Exception) {
                false
            }
        }
        
        if (filtered.isEmpty()) return filtered
        
        // 如果只有1个或2个点，不需要采样
        if (filtered.size <= 2) return filtered
        
        // 计算日期范围（天数）
        val calStart = Calendar.getInstance()
        calStart.time = startDate
        calStart.set(Calendar.HOUR_OF_DAY, 0)
        calStart.set(Calendar.MINUTE, 0)
        calStart.set(Calendar.SECOND, 0)
        calStart.set(Calendar.MILLISECOND, 0)
        
        val calEnd = Calendar.getInstance()
        calEnd.time = endDate
        calEnd.set(Calendar.HOUR_OF_DAY, 0)
        calEnd.set(Calendar.MINUTE, 0)
        calEnd.set(Calendar.SECOND, 0)
        calEnd.set(Calendar.MILLISECOND, 0)
        
        val daysDiff = ((calEnd.timeInMillis - calStart.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        
        // 动态决定采样间隔（两点之间的相差天数）
        // 如果范围 <= 30天：每1天一个点（不采样）
        // 如果范围 <= 90天：每3天一个点
        // 如果范围 <= 180天：每7天一个点
        // 如果范围 <= 365天：每14天一个点
        // 如果范围 <= 730天（2年）：每30天一个点
        // 如果范围 > 730天：每60天一个点
        val sampleInterval = when {
            daysDiff <= 30 -> 1
            daysDiff <= 90 -> 3
            daysDiff <= 180 -> 7
            daysDiff <= 365 -> 14
            daysDiff <= 730 -> 30
            else -> 60
        }
        
        // 如果不需要采样，直接返回
        if (sampleInterval == 1) return filtered
        
        // 采样数据：保留第一个和最后一个点，中间按间隔采样
        val sampled = mutableListOf<WeightRecord>()
        sampled.add(filtered.first()) // 总是包含第一个点
        
        for (i in sampleInterval until filtered.size - 1 step sampleInterval) {
            sampled.add(filtered[i])
        }
        
        // 确保最后一个点被包含（如果还没有被添加）
        if (sampled.lastOrNull() != filtered.last()) {
            sampled.add(filtered.last())
        }
        
        return sampled
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
        // 设置滑块初始值，确保值在有效范围内（30-150）
        val validWeight = currentWeight.coerceIn(30.0, 150.0)
        if (currentWeight < 30.0 || currentWeight > 150.0) {
            currentWeight = validWeight
        }
        binding.sliderWeight.value = validWeight.toFloat()
        binding.tvWeightInput.text = String.format(Locale.getDefault(), "%.1f", validWeight)
        
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
    
    // 简单的体重图表View（支持点击显示数值）
    private class WeightChartView(context: android.content.Context) : View(context) {
        private var data: List<WeightRecord> = emptyList()
        private var selectedIndex: Int? = null
        
        private val leftPadding = 40f
        private val rightPadding = 40f
        private val topPadding = 20f
        private val bottomPadding = 40f
        
        private val gridPaint = Paint().apply {
            color = resources.getColor(R.color.slate_200, null)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        
        private val linePaint = Paint().apply {
            color = resources.getColor(R.color.emerald_500, null)
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        
        private val pointPaint = Paint().apply {
            color = resources.getColor(R.color.white, null)
            style = Paint.Style.FILL
        }
        
        private val strokePaint = Paint().apply {
            color = resources.getColor(R.color.emerald_500, null)
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        
        private val tooltipBackgroundPaint = Paint().apply {
            color = resources.getColor(R.color.white, null)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        private val tooltipBorderPaint = Paint().apply {
            color = resources.getColor(R.color.slate_200, null)
            strokeWidth = 2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        
        private val tooltipTextPaint = Paint().apply {
            color = resources.getColor(R.color.slate_800, null)
            textSize = resources.displayMetrics.scaledDensity * 12f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        
        fun setData(weightData: List<WeightRecord>) {
            data = weightData
            invalidate()
        }
        
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            
            if (data.size < 2) return
            
            val width = width.toFloat()
            val height = height.toFloat()
            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - topPadding - bottomPadding
            val bottomY = topPadding + chartHeight
            
            val weights = data.map { it.weight }
            val minWeight = weights.minOrNull() ?: 0.0
            val maxWeight = weights.maxOrNull() ?: 100.0
            val weightRange = maxWeight - minWeight
            
            if (weightRange == 0.0) return
            
            // 绘制网格线
            canvas.drawLine(leftPadding, bottomY, width - rightPadding, bottomY, gridPaint)
            
            // 绘制折线
            val path = Path()
            data.forEachIndexed { index, record ->
                val x = leftPadding + (index.toFloat() / (data.size - 1)) * chartWidth
                val y = bottomY - ((record.weight - minWeight) / weightRange).toFloat() * chartHeight
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            canvas.drawPath(path, linePaint)
            
            // 绘制点
            data.forEachIndexed { index, record ->
                val x = leftPadding + (index.toFloat() / (data.size - 1)) * chartWidth
                val y = bottomY - ((record.weight - minWeight) / weightRange).toFloat() * chartHeight
                
                // 如果被选中，绘制更大的点
                val pointRadius = if (selectedIndex == index) 6f else 4f
                canvas.drawCircle(x, y, pointRadius, pointPaint)
                canvas.drawCircle(x, y, pointRadius, strokePaint)
            }
            
            // 绘制选中点的提示框
            selectedIndex?.let { index ->
                if (index >= 0 && index < data.size) {
                    val record = data[index]
                    val x = leftPadding + (index.toFloat() / (data.size - 1)) * chartWidth
                    val y = bottomY - ((record.weight - minWeight) / weightRange).toFloat() * chartHeight
                    
                    val tooltipText = "${String.format(Locale.getDefault(), "%.1f", record.weight)} kg"
                    val textWidth = tooltipTextPaint.measureText(tooltipText)
                    val tooltipPadding = 16f
                    val tooltipWidth = textWidth + tooltipPadding * 2
                    val tooltipHeight = 40f
                    val tooltipSpacing = 10f
                    
                    // 优先放在点的上方
                    var tooltipY = y - tooltipSpacing
                    var tooltipTop = tooltipY - tooltipHeight
                    
                    // 如果上方超出，放在下方
                    if (tooltipTop < topPadding) {
                        tooltipY = y + tooltipSpacing
                        tooltipTop = tooltipY - tooltipHeight
                        
                        if (tooltipY > bottomY) {
                            tooltipY = topPadding + tooltipHeight
                            tooltipTop = topPadding
                        }
                    }
                    
                    // 确保在图表区域内
                    if (tooltipTop < topPadding) {
                        tooltipY = topPadding + tooltipHeight
                    }
                    if (tooltipY > bottomY) {
                        tooltipY = bottomY
                    }
                    
                    // 计算X位置
                    var tooltipX = x - tooltipWidth / 2
                    if (tooltipX < leftPadding) {
                        tooltipX = leftPadding + 5f
                    } else if (tooltipX + tooltipWidth > width - rightPadding) {
                        tooltipX = width - rightPadding - tooltipWidth - 5f
                    }
                    
                    val tooltipRect = android.graphics.RectF(
                        tooltipX,
                        tooltipY - tooltipHeight,
                        tooltipX + tooltipWidth,
                        tooltipY
                    )
                    
                    // 绘制提示框
                    canvas.drawRoundRect(tooltipRect, 8f, 8f, tooltipBackgroundPaint)
                    canvas.drawRoundRect(tooltipRect, 8f, 8f, tooltipBorderPaint)
                    
                    // 绘制文本
                    canvas.drawText(
                        tooltipText,
                        tooltipX + tooltipWidth / 2,
                        tooltipY - tooltipHeight / 2 + tooltipTextPaint.textSize / 3,
                        tooltipTextPaint
                    )
                }
            }
        }
        
        override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> {
                    if (data.size < 2) return false
                    
                    val width = width.toFloat()
                    val height = height.toFloat()
                    val chartWidth = width - leftPadding - rightPadding
                    val chartHeight = height - topPadding - bottomPadding
                    val bottomY = topPadding + chartHeight
                    
                    val weights = data.map { it.weight }
                    val minWeight = weights.minOrNull() ?: 0.0
                    val maxWeight = weights.maxOrNull() ?: 100.0
                    val weightRange = maxWeight - minWeight
                    
                    if (weightRange == 0.0) return false
                    
                    val x = event.x
                    val y = event.y
                    
                    // 找到最近的数据点
                    var minDistance = Float.MAX_VALUE
                    var closestIndex = -1
                    
                    data.forEachIndexed { index, record ->
                        val pointX = leftPadding + (index.toFloat() / (data.size - 1)) * chartWidth
                        val pointY = bottomY - ((record.weight - minWeight) / weightRange).toFloat() * chartHeight
                        
                        val distance = kotlin.math.sqrt(
                            (x - pointX) * (x - pointX) + (y - pointY) * (y - pointY)
                        )
                        
                        if (distance < minDistance && distance < 50f) {
                            minDistance = distance
                            closestIndex = index
                        }
                    }
                    
                    if (closestIndex >= 0) {
                        selectedIndex = closestIndex
                        invalidate()
                        return true
                    }
                }
                android.view.MotionEvent.ACTION_UP -> {
                    selectedIndex = null
                    invalidate()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }
}

