package com.example.forhealth.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.forhealth.R
import com.example.forhealth.databinding.FragmentHomeBinding
import com.example.forhealth.models.DailyStats
import com.example.forhealth.ui.adapters.TimelineAdapter
import com.example.forhealth.utils.DateUtils
import com.example.forhealth.viewmodels.MainViewModel

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MainViewModel
    private lateinit var timelineAdapter: TimelineAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化 ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        // 初始化 RecyclerView
        timelineAdapter = TimelineAdapter(
            items = emptyList(),
            onMealGroupClick = { mealGroup -> openEditMealDialog(mealGroup) },
            onExerciseClick = { activity -> openEditExerciseDialog(activity) }
        )
        binding.rvTimeline.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTimeline.adapter = timelineAdapter
        
        // 设置初始数据
        setupInitialData()
        
        // 设置点击事件
        setupClickListeners()
        
        // 设置Analytics视图监听器
        setupAnalyticsListeners()
        updateRangeButtons() // 初始化范围选择器状态
        
        // 观察数据变化
        observeData()
        
        // 从后端加载今日饮食记录
        loadTodayMeals()
        
        // 从后端加载今日运动记录
        loadTodayExercises()
    }
    
    private fun setupInitialData() {
        // 设置日期
        binding.tvDate.text = DateUtils.getCurrentDate()
        
        // 显示AI建议（默认显示）
        binding.cardAiInsight.visibility = View.VISIBLE
        
        // 初始化统计数据
        updateStatsDisplay(DailyStats.getInitial())
    }
    
    private fun setupClickListeners() {
        // FAB 主按钮 - 展开/收起菜单
        binding.fabMain.setOnClickListener {
            toggleFabMenu()
        }
        
        // FAB 食物按钮 - 打开添加食物覆盖层
        binding.btnFabFood.setOnClickListener {
            openAddMealDialog()
        }
        
        // FAB 运动按钮 - 打开添加运动覆盖层
        binding.btnFabExercise.setOnClickListener {
            openAddExerciseDialog()
        }
        
        // 视图切换按钮
        binding.btnToggleView.setOnClickListener {
            toggleViewMode()
        }
        
        // 个人资料按钮
        binding.btnProfile.setOnClickListener {
            openProfileFragment()
        }
        
        // AI Insight 按钮 - 打开AI聊天界面
        binding.cardAiInsight.setOnClickListener {
            openAiChatDialog()
        }
    }
    
    private fun toggleFabMenu() {
        val isVisible = binding.fabMenuContainer.visibility == View.VISIBLE
        
        if (isVisible) {
            // 收起菜单
            binding.fabMenuContainer.visibility = View.GONE
            // 旋转回原位置
            binding.fabMain.rotation = 0f
            binding.fabMain.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.emerald_600, null)
            ))
        } else {
            // 展开菜单
            binding.fabMenuContainer.visibility = View.VISIBLE
            // 旋转45度并改变颜色
            binding.fabMain.rotation = 45f
            binding.fabMain.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.slate_800, null)
            ))
        }
    }
    
    private fun openAddMealDialog() {
        // 使用 DialogFragment 作为覆盖层，显示添加食物界面
        val dialog = AddMealFragment().apply {
            // 设置回调，当添加成功后更新数据
            setOnMealAddedListener { meals ->
                viewModel.addMeals(meals)
            }
        }
        dialog.show(parentFragmentManager, "AddMealDialog")
        toggleFabMenu() // 收起菜单
    }
    
    private fun openAddExerciseDialog() {
        fun showDialog(exerciseLibrary: List<com.example.forhealth.models.ExerciseItem>) {
            val dialog = AddExerciseFragment().apply {
                setExerciseLibrary(exerciseLibrary)
                // 设置回调，当添加成功后通过API创建记录
                setOnExerciseAddedListener { exercises ->
                    viewModel.createExerciseRecords(exercises) { result ->
                        when (result) {
                            is com.example.forhealth.network.ApiResult.Success -> {
                                // 数据已通过loadTodayExercises更新
                            }
                            is com.example.forhealth.network.ApiResult.Error -> {
                                android.widget.Toast.makeText(requireContext(), result.message, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            is com.example.forhealth.network.ApiResult.Loading -> {}
                        }
                    }
                }
            }
            dialog.show(parentFragmentManager, "AddExerciseDialog")
            toggleFabMenu() // 收起菜单
        }

        val cached = viewModel.exerciseLibrary.value ?: emptyList()
        if (cached.isNotEmpty()) {
            showDialog(cached)
        } else {
            viewModel.loadExerciseLibrary { result: com.example.forhealth.network.ApiResult<List<com.example.forhealth.models.ExerciseItem>> ->
                when (result) {
                    is com.example.forhealth.network.ApiResult.Success -> showDialog(result.data)
                    is com.example.forhealth.network.ApiResult.Error -> {
                        android.widget.Toast.makeText(requireContext(), result.message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    is com.example.forhealth.network.ApiResult.Loading -> {}
                }
            }
        }
    }
    
    private fun openProfileFragment() {
        // 使用 DialogFragment 作为覆盖层，显示个人信息界面
        val dialog = ProfileFragment()
        dialog.show(parentFragmentManager, "ProfileDialog")
    }
    
    private fun openAiChatDialog() {
        // 打开AI聊天界面
        val dialog = AiChatFragment().apply {
            setUserProfile(viewModel.userProfile.value ?: com.example.forhealth.models.UserProfile.getInitial())
            setCurrentStats(viewModel.dailyStats.value ?: DailyStats.getInitial())
            setInitialContext(viewModel.aiSuggestion.value ?: "")
        }
        dialog.show(parentFragmentManager, "AiChatDialog")
    }
    
    private fun openEditMealDialog(mealGroup: com.example.forhealth.models.MealGroup) {
        val dialog = EditMealFragment().apply {
            setMealGroup(mealGroup)
            setOnMealUpdatedListener { updatedGroup ->
                viewModel.updateMealGroup(updatedGroup)
            }
            setOnMealDeletedListener { mealGroupId ->
                viewModel.deleteMealGroup(mealGroupId)
            }
        }
        dialog.show(parentFragmentManager, "EditMealDialog")
    }
    
    private fun openEditExerciseDialog(activity: com.example.forhealth.models.ActivityItem) {
        fun showDialog(exerciseLibrary: List<com.example.forhealth.models.ExerciseItem>) {
            val dialog = EditExerciseFragment().apply {
                setExerciseLibrary(exerciseLibrary)
                setActivity(activity)
                setOnExerciseUpdatedListener { updatedActivity ->
                    // 直接传递ActivityItem给ViewModel，ViewModel内部使用ExerciseRepository处理
                    viewModel.updateExerciseRecord(updatedActivity) { result ->
                        when (result) {
                            is com.example.forhealth.network.ApiResult.Success -> {
                                // 数据已通过loadTodayExercises更新
                            }
                            is com.example.forhealth.network.ApiResult.Error -> {
                                android.widget.Toast.makeText(requireContext(), result.message, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            is com.example.forhealth.network.ApiResult.Loading -> {}
                        }
                    }
                }
            }
            dialog.show(parentFragmentManager, "EditExerciseDialog")
        }
        
        // 从缓存或加载运动库数据
        val cached = viewModel.exerciseLibrary.value ?: emptyList()
        if (cached.isNotEmpty()) {
            showDialog(cached)
        } else {
            viewModel.loadExerciseLibrary { result: com.example.forhealth.network.ApiResult<List<com.example.forhealth.models.ExerciseItem>> ->
                when (result) {
                    is com.example.forhealth.network.ApiResult.Success -> showDialog(result.data)
                    is com.example.forhealth.network.ApiResult.Error -> {
                        android.widget.Toast.makeText(requireContext(), result.message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    is com.example.forhealth.network.ApiResult.Loading -> {}
                }
            }
        }
    }
    
    private var currentRange: AnalyticsRange = AnalyticsRange.DAY
    
    private enum class AnalyticsRange {
        DAY, WEEK, MONTH
    }
    
    private fun toggleViewMode() {
        val isChartMode = binding.scrollAnalytics.visibility == View.VISIBLE
        
        if (isChartMode) {
            // 切换到列表视图 - 恢复header位置
            binding.scrollAnalytics.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(300)
                .withEndAction {
                    binding.scrollAnalytics.visibility = View.GONE
                    
                    // 恢复滚动位置
                    binding.root.findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollViewMain)?.let { scrollView ->
                        binding.root.post {
                            scrollView.smoothScrollTo(0, 0)
                        }
                    }
                    
                    binding.rvTimeline.alpha = 0f
                    binding.rvTimeline.translationY = -100f
                    binding.rvTimeline.visibility = View.VISIBLE
                    binding.rvTimeline.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .start()
                }
                .start()
            
            binding.tvViewTitle.text = getString(R.string.timeline)
            binding.btnToggleView.setImageResource(R.drawable.ic_bar_chart)
        } else {
            // 切换到图表视图 - 实现上推动画
            binding.rvTimeline.animate()
                .alpha(0f)
                .translationY(-100f)
                .setDuration(300)
                .withEndAction {
                    binding.rvTimeline.visibility = View.GONE
                    binding.scrollAnalytics.alpha = 0f
                    binding.scrollAnalytics.translationY = 100f
                    binding.scrollAnalytics.visibility = View.VISIBLE
                    
                    // 上推动画：将整个NestedScrollView向上滚动，直到Analytics被推到顶部
                    binding.root.findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollViewMain)?.let { scrollView ->
                        binding.root.findViewById<View>(R.id.headerSection)?.let { header ->
                            // 计算需要滚动的距离：headerSection的高度 + 一些额外空间，确保Analytics到达顶部
                            val scrollDistance = header.height + header.top
                            // 延迟一下确保视图已经布局完成
                            binding.root.post {
                                scrollView.smoothScrollTo(0, scrollDistance)
                            }
                        }
                    }
                    
                    binding.scrollAnalytics.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .start()
                    
                    // 加载Analytics数据并更新视图
                    loadAnalyticsData()
                    updateAnalyticsDisplay()
                }
                .start()
            
            binding.tvViewTitle.text = getString(R.string.analytics)
            binding.btnToggleView.setImageResource(R.drawable.ic_list)
        }
    }
    
    private fun setupAnalyticsListeners() {
        // 时间范围选择器
        binding.btnRangeDay.setOnClickListener {
            currentRange = AnalyticsRange.DAY
            updateRangeButtons()
            loadAnalyticsData()
        }
        binding.btnRangeWeek.setOnClickListener {
            currentRange = AnalyticsRange.WEEK
            updateRangeButtons()
            loadAnalyticsData()
        }
        binding.btnRangeMonth.setOnClickListener {
            currentRange = AnalyticsRange.MONTH
            updateRangeButtons()
            loadAnalyticsData()
        }
    }
    
    /**
     * 根据当前范围加载Analytics数据
     */
    private fun loadAnalyticsData() {
        when (currentRange) {
            AnalyticsRange.DAY -> {
                // 日视图：加载每日卡路里摘要（会自动加载营养素分析）
                val today = getTodayDateString()
                viewModel.loadDailyCalorieSummary(today)
            }
            AnalyticsRange.WEEK -> {
                // 周视图：加载最近7天的时间序列趋势和营养素分析
                val (startDate, endDate) = getWeekDateRange()
                viewModel.loadTimeSeriesTrend(startDate, endDate, "day")
                viewModel.loadNutritionAnalysis(startDate, endDate, null)
            }
            AnalyticsRange.MONTH -> {
                // 月视图：加载最近28天的时间序列趋势和营养素分析
                val (startDate, endDate) = getMonthDateRange()
                viewModel.loadTimeSeriesTrend(startDate, endDate, "week")
                viewModel.loadNutritionAnalysis(startDate, endDate, null)
            }
        }
    }
    
    /**
     * 获取今天的日期字符串（YYYY-MM-DD）
     */
    private fun getTodayDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }
    
    /**
     * 获取周视图的日期范围（最近7天）
     * @return Pair(startDate, endDate) 格式为 YYYY-MM-DD
     */
    private fun getWeekDateRange(): Pair<String, String> {
        val calendar = java.util.Calendar.getInstance()
        val endDate = calendar.clone() as java.util.Calendar
        
        // 开始日期：7天前
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -6) // -6 因为包含今天，所以是7天
        
        val startYear = calendar.get(java.util.Calendar.YEAR)
        val startMonth = calendar.get(java.util.Calendar.MONTH) + 1
        val startDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val startDate = String.format("%04d-%02d-%02d", startYear, startMonth, startDay)
        
        val endYear = endDate.get(java.util.Calendar.YEAR)
        val endMonth = endDate.get(java.util.Calendar.MONTH) + 1
        val endDay = endDate.get(java.util.Calendar.DAY_OF_MONTH)
        val endDateStr = String.format("%04d-%02d-%02d", endYear, endMonth, endDay)
        
        return Pair(startDate, endDateStr)
    }
    
    /**
     * 获取月视图的日期范围（最近28天）
     * @return Pair(startDate, endDate) 格式为 YYYY-MM-DD
     */
    private fun getMonthDateRange(): Pair<String, String> {
        val calendar = java.util.Calendar.getInstance()
        val endDate = calendar.clone() as java.util.Calendar
        
        // 开始日期：28天前
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -27) // -27 因为包含今天，所以是28天
        
        val startYear = calendar.get(java.util.Calendar.YEAR)
        val startMonth = calendar.get(java.util.Calendar.MONTH) + 1
        val startDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val startDate = String.format("%04d-%02d-%02d", startYear, startMonth, startDay)
        
        val endYear = endDate.get(java.util.Calendar.YEAR)
        val endMonth = endDate.get(java.util.Calendar.MONTH) + 1
        val endDay = endDate.get(java.util.Calendar.DAY_OF_MONTH)
        val endDateStr = String.format("%04d-%02d-%02d", endYear, endMonth, endDay)
        
        return Pair(startDate, endDateStr)
    }
    
    private fun updateRangeButtons() {
        val selectedColor = resources.getColor(R.color.slate_800, null)
        val unselectedColor = resources.getColor(R.color.text_secondary, null)
        val selectedBg = resources.getColor(R.color.white, null)
        val unselectedBg = android.graphics.Color.TRANSPARENT
        
        binding.btnRangeDay.apply {
            setTextColor(if (currentRange == AnalyticsRange.DAY) selectedColor else unselectedColor)
            setBackgroundColor(if (currentRange == AnalyticsRange.DAY) selectedBg else unselectedBg)
        }
        binding.btnRangeWeek.apply {
            setTextColor(if (currentRange == AnalyticsRange.WEEK) selectedColor else unselectedColor)
            setBackgroundColor(if (currentRange == AnalyticsRange.WEEK) selectedBg else unselectedBg)
        }
        binding.btnRangeMonth.apply {
            setTextColor(if (currentRange == AnalyticsRange.MONTH) selectedColor else unselectedColor)
            setBackgroundColor(if (currentRange == AnalyticsRange.MONTH) selectedBg else unselectedBg)
        }
    }
    
    private fun updateAnalyticsDisplay() {
        val stats = viewModel.dailyStats.value ?: DailyStats.getInitial()
        val dailyCalorieSummary = viewModel.dailyCalorieSummary.value
        
        // 根据范围更新Activity Trend标题
        val activityTitle = binding.root.findViewById<android.widget.TextView>(R.id.tvActivityTrendTitle)
        activityTitle?.text = if (currentRange == AnalyticsRange.DAY) {
            getString(R.string.todays_activity)
        } else {
            getString(R.string.activity_trend)
        }
        
        // 生成图表数据（使用后端数据）
        val chartData = generateChartData()
        drawActivityChart(chartData)
        
        // 计算显示值（日：总数，周/月：平均值）
        val (intakeDisplay, burnedDisplay, labelText) = when (currentRange) {
            AnalyticsRange.DAY -> {
                // 日视图：使用后端 daily-calorie-summary 数据，如果未加载则设为0
                if (dailyCalorieSummary != null) {
                    Triple(
                        dailyCalorieSummary.total_intake,
                        dailyCalorieSummary.total_burned,
                        getString(R.string.total_intake)
                    )
                } else {
                    // 如果后端数据未加载，设为0（不再使用本地数据）
                    Triple(0.0, 0.0, getString(R.string.total_intake))
                }
            }
            AnalyticsRange.WEEK -> {
                // 周视图：计算平均值
                if (chartData.isNotEmpty()) {
                    val avgIntake = chartData.sumOf { it.intake } / chartData.size.toDouble()
                    val avgBurned = chartData.sumOf { it.burned } / chartData.size.toDouble()
                    Triple(avgIntake, avgBurned, getString(R.string.avg_intake))
                } else {
                    Triple(0.0, 0.0, getString(R.string.avg_intake))
                }
            }
            AnalyticsRange.MONTH -> {
                // 月视图：计算平均值
                if (chartData.isNotEmpty()) {
                    val avgIntake = chartData.sumOf { it.intake } / chartData.size.toDouble()
                    val avgBurned = chartData.sumOf { it.burned } / chartData.size.toDouble()
                    Triple(avgIntake, avgBurned, getString(R.string.avg_intake))
                } else {
                    Triple(0.0, 0.0, getString(R.string.avg_intake))
                }
            }
        }
        
        // 更新标签文本
        binding.tvTotalIntakeLabel.text = labelText
        binding.tvTotalBurnLabel.text = if (currentRange == AnalyticsRange.DAY) getString(R.string.total_burn) else getString(R.string.avg_burn)
        
        // 更新数值，在数值后加上" kcal"
        binding.tvTotalIntake.text = "${Math.round(intakeDisplay)} ${getString(R.string.kcal)}"
        binding.tvTotalBurned.text = "${Math.round(burnedDisplay)} ${getString(R.string.kcal)}"
        
        // 计算并显示Sum
        val sumValue = intakeDisplay - burnedDisplay
        val sumLabel = if (currentRange == AnalyticsRange.DAY) getString(R.string.sum) else getString(R.string.avg_sum)
        binding.tvSumLabel.text = sumLabel
        binding.tvSum.text = "${Math.round(sumValue)} ${getString(R.string.kcal)}"
        
        // 获取宏量营养素数据（从后端 nutrition-analysis API）
        val macroData = viewModel.macroDataForChart.value
        
        // 计算宏量营养素（使用后端数据，如果未加载则设为0）
        val proteinCal = macroData?.proteinCalories ?: 0.0
        val carbsCal = macroData?.carbsCalories ?: 0.0
        val fatCal = macroData?.fatCalories ?: 0.0
        val totalMacroCal = proteinCal + carbsCal + fatCal
        val totalIntakeValue = macroData?.totalCalories ?: intakeDisplay
        
        // 更新甜甜圈图（如果后端数据未加载，传入0值以显示灰色圆环）
        if (macroData != null && totalIntakeValue > 0) {
            binding.macroDonutChart.setMacros(proteinCal, carbsCal, fatCal, totalIntakeValue)
        } else {
            binding.macroDonutChart.setMacros(0.0, 0.0, 0.0, 0.0)
        }
        
        // 计算并显示百分比
        if (totalMacroCal > 0 && macroData != null) {
            val proteinPct = (proteinCal / totalMacroCal * 100).toInt()
            val carbsPct = (carbsCal / totalMacroCal * 100).toInt()
            val fatPct = (fatCal / totalMacroCal * 100).toInt()
            
            binding.tvMacroProtein.text = "Protein: ${proteinPct}%"
            binding.tvMacroProtein.setTextColor(resources.getColor(R.color.blue_500, null))
            binding.tvMacroCarbs.text = "Carbs: ${carbsPct}%"
            binding.tvMacroCarbs.setTextColor(resources.getColor(R.color.amber_400, null))
            binding.tvMacroFat.text = "Fat: ${fatPct}%"
            binding.tvMacroFat.setTextColor(resources.getColor(R.color.rose_400, null))
        } else {
            // 如果后端数据未加载，显示0%（不再使用本地数据）
            binding.tvMacroProtein.text = "Protein: 0%"
            binding.tvMacroProtein.setTextColor(resources.getColor(R.color.blue_500, null))
            binding.tvMacroCarbs.text = "Carbs: 0%"
            binding.tvMacroCarbs.setTextColor(resources.getColor(R.color.amber_400, null))
            binding.tvMacroFat.text = "Fat: 0%"
            binding.tvMacroFat.setTextColor(resources.getColor(R.color.rose_400, null))
        }
    }
    
    /**
     * 生成图表数据（使用后端API数据）
     */
    private fun generateChartData(): List<com.example.forhealth.ui.views.ChartDataPoint> {
        return when (currentRange) {
            AnalyticsRange.DAY -> {
                // 日视图：使用 daily-calorie-summary 数据
                val dailyCalorieSummary = viewModel.dailyCalorieSummary.value
                if (dailyCalorieSummary != null) {
                    listOf(
                        com.example.forhealth.ui.views.ChartDataPoint(
                            label = "Today",
                            intake = dailyCalorieSummary.total_intake,
                            burned = dailyCalorieSummary.total_burned
                        )
                    )
                } else {
                    // 如果数据未加载，返回空数据点
                    listOf(
                        com.example.forhealth.ui.views.ChartDataPoint(
                            label = "Today",
                            intake = 0.0,
                            burned = 0.0
                        )
                    )
                }
            }
            AnalyticsRange.WEEK, AnalyticsRange.MONTH -> {
                // 周/月视图：使用 time-series-trend 数据
                val timeSeriesData = viewModel.timeSeriesTrendData.value ?: emptyList()
                if (timeSeriesData.isNotEmpty()) {
                    timeSeriesData
                } else {
                    // 如果数据未加载，返回空列表
                    emptyList()
                }
            }
        }
    }
    
    private fun drawActivityChart(data: List<com.example.forhealth.ui.views.ChartDataPoint>) {
        binding.viewActivityChart.removeAllViews()
        if (currentRange == AnalyticsRange.DAY) {
            // 日视图使用柱状图
            val chartView = com.example.forhealth.ui.views.ActivityBarChartView(requireContext())
            chartView.setData(data)
            binding.viewActivityChart.addView(chartView)
        } else {
            // 周/月视图使用折线图
            val chartView = com.example.forhealth.ui.views.ActivityTrendChartView(requireContext())
            chartView.setData(data)
            binding.viewActivityChart.addView(chartView)
        }
    }
    
    private fun observeData() {
        // 观察统计数据
        viewModel.dailyStats.observe(viewLifecycleOwner) { stats ->
            updateStatsDisplay(stats)
        }
        
        // 观察时间线数据
        viewModel.timelineItems.observe(viewLifecycleOwner) { items ->
            timelineAdapter.updateItems(items)
        }
        
        // 观察AI建议
        viewModel.aiSuggestion.observe(viewLifecycleOwner) { suggestion ->
            if (suggestion.isNotEmpty()) {
                binding.tvAiSuggestion.text = suggestion
                binding.cardAiInsight.visibility = View.VISIBLE
            } else {
                binding.cardAiInsight.visibility = View.GONE
            }
        }
        
        // 观察用户资料，更新问候语
        viewModel.userProfileResponse.observe(viewLifecycleOwner) { profileResponse ->
            profileResponse?.username?.let { username ->
                binding.tvGreeting.text = "Hello, $username"
            } ?: run {
                binding.tvGreeting.text = getString(R.string.hello_user)
            }
        }
        
        // 观察每日卡路里摘要，更新Analytics视图（日视图）
        viewModel.dailyCalorieSummary.observe(viewLifecycleOwner) {
            if (binding.scrollAnalytics.visibility == View.VISIBLE && currentRange == AnalyticsRange.DAY) {
                updateAnalyticsDisplay()
            }
        }
        
        // 观察时间序列趋势数据，更新Analytics视图（周/月视图）
        viewModel.timeSeriesTrendData.observe(viewLifecycleOwner) {
            if (binding.scrollAnalytics.visibility == View.VISIBLE && 
                (currentRange == AnalyticsRange.WEEK || currentRange == AnalyticsRange.MONTH)) {
                updateAnalyticsDisplay()
            }
        }
        
        // 观察宏量营养素数据，更新Analytics视图（饼图）
        viewModel.macroDataForChart.observe(viewLifecycleOwner) {
            if (binding.scrollAnalytics.visibility == View.VISIBLE) {
                updateAnalyticsDisplay()
            }
        }
    }
    
    /**
     * 从后端加载今日饮食记录
     */
    private fun loadTodayMeals() {
        viewModel.loadTodayMeals()
    }
    
    /**
     * 从后端加载今日运动记录
     */
    private fun loadTodayExercises() {
        viewModel.loadTodayExercises()
    }
    
    private fun updateStatsDisplay(stats: DailyStats) {
        // 更新卡路里显示
        val netCalories = stats.calories.current - stats.burned
        binding.tvNetCalories.text = Math.round(netCalories).toString()
        binding.tvCaloriesIn.text = Math.round(stats.calories.current).toString()
        binding.tvCaloriesBurned.text = Math.round(stats.burned).toString()
        binding.tvTarget.text = getString(R.string.target) + ": ${stats.calories.target}"
        
        // 更新环形进度
        binding.ringProgress.setProgress(netCalories, stats.calories.target)
        
        // 更新宏量营养素
        binding.tvCarbs.text = "${Math.round(stats.carbs.current)}/${stats.carbs.target}g"
        binding.tvFat.text = "${Math.round(stats.fat.current)}/${stats.fat.target}g"
        binding.tvProtein.text = "${Math.round(stats.protein.current)}/${stats.protein.target}g"
        
        // 更新进度条
        binding.progressCarbs.setProgress(stats.carbs.current, stats.carbs.target, R.color.amber_400)
        binding.progressFat.setProgress(stats.fat.current, stats.fat.target, R.color.rose_400)
        binding.progressProtein.setProgress(stats.protein.current, stats.protein.target, R.color.blue_400)
    }
    
    override fun onResume() {
        super.onResume()
        // 当Fragment重新可见时，重新加载今日饮食记录（确保数据是最新的）
        loadTodayMeals()
        // 重新加载今日运动记录
        loadTodayExercises()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
