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
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        
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
        
        // 先加载今日运动记录（确保_exercises有数据）
        loadTodayExercises()
        
        // 从后端加载今日饮食记录
        loadTodayMeals()
        
        // 从后端加载今日统计数据（初始化圆环和宏量营养素）
        // 注意：必须在loadTodayExercises()之后执行，以便使用_exercises的数据
        loadTodayStats()
        
        // 加载用户资料（用于显示用户名）
        viewModel.loadUserProfile()
    }
    
    private fun setupInitialData() {
        // 设置日期
        binding.tvDate.text = DateUtils.getCurrentDate()
        
        // 显示AI建议（默认显示）
        binding.cardAiInsight.visibility = View.VISIBLE
        
        // 设置圆环宽度为屏幕宽度的2/5
        binding.root.post {
            val screenWidth = resources.displayMetrics.widthPixels
            val ringSize = screenWidth / 5 * 2
            val frameLayout = binding.root.findViewById<View>(R.id.frameRingProgress)
            
            // 同步更新圆环自身的尺寸（保持圆环大小不变）
            binding.ringProgress.setSize(ringSize)
            
            // 等待圆环测量完成后再设置FrameLayout大小，使其更紧凑
            binding.ringProgress.post {
                // FrameLayout大小设置为圆环的实际测量大小，避免多余空间
                val measuredSize = maxOf(binding.ringProgress.measuredWidth, binding.ringProgress.measuredHeight)
                frameLayout?.layoutParams?.width = measuredSize
                frameLayout?.layoutParams?.height = measuredSize
                frameLayout?.requestLayout()
            }
        }
        
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
        
        // AI Insight 刷新按钮
        binding.btnRefreshAiInsight.setOnClickListener {
            viewModel.refreshAiInsight()
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
            // 设置回调，当添加成功后刷新统计数据
            setOnMealAddedListener { meals ->
                // 重新加载今日统计数据以更新圆环和营养素统计
                loadTodayStats()
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
                viewModel.recalculateStatsForced()
                viewModel.dailyStats.value?.let { stats ->
                    updateStatsDisplay(stats)
                }
            }
            setOnMealDeletedListener { mealGroupId ->
                viewModel.deleteMealGroup(mealGroupId)
                // 删除后也刷新统计数据
                viewModel.recalculateStatsForced()
                viewModel.dailyStats.value?.let { stats ->
                    updateStatsDisplay(stats)
                }
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
                setOnExerciseDeletedListener { recordId ->
                    // ViewModel 的 deleteExerciseRecord 已经处理了删除和重新加载数据
                    // 这里不需要再次调用，只需要通知即可
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
            val scrollView = binding.scrollViewMain
            val currentScrollY = scrollView.scrollY
            
            // 使用ValueAnimator实现平滑滚动
            val scrollAnimator = android.animation.ValueAnimator.ofInt(currentScrollY, 0)
            scrollAnimator.duration = 300
            scrollAnimator.interpolator = android.view.animation.DecelerateInterpolator()
            scrollAnimator.addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                scrollView.scrollTo(0, value)
            }
            scrollAnimator.start()
            
            // 同时开始淡出analytics视图
            binding.scrollAnalytics.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(300)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction {
                    binding.scrollAnalytics.visibility = View.GONE
                }
                .start()
            
            // 在动画进行到一半时开始timeline淡入，实现更流畅的过渡
            binding.root.postDelayed({
                binding.rvTimeline.alpha = 0f
                binding.rvTimeline.translationY = -100f
                binding.rvTimeline.visibility = View.VISIBLE
                binding.rvTimeline.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }, 150) // 在动画进行一半时开始切换视图，实现重叠效果
            
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
                    
                    // 上推动画：将整个NestedScrollView向上滚动到底，推到不能再推为止
                    // 延迟一下确保视图已经布局完成
                    binding.root.post {
                        val scrollView = binding.scrollViewMain
                        // 计算最大滚动距离：内容总高度 - 可见高度
                        val maxScrollY = scrollView.getChildAt(0).height - scrollView.height
                        // 滚动到底
                        scrollView.smoothScrollTo(0, maxScrollY.coerceAtLeast(0))
                    }
                    
                    binding.scrollAnalytics.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .withEndAction {
                            // 动画完成后，强制加载数据并更新所有图表
                            loadAnalyticsData()
                            // 延迟一下确保数据加载完成后再更新UI
                            binding.root.postDelayed({
                                updateAnalyticsDisplay()
                                updateNutritionDonutChart()
                            }, 100)
                        }
                        .start()
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
            // 强制更新UI
            binding.root.postDelayed({
                updateAnalyticsDisplay()
                updateNutritionDonutChart()
            }, 100)
        }
        binding.btnRangeWeek.setOnClickListener {
            currentRange = AnalyticsRange.WEEK
            updateRangeButtons()
            loadAnalyticsData()
            // 强制更新UI
            binding.root.postDelayed({
                updateAnalyticsDisplay()
                updateNutritionDonutChart()
            }, 100)
        }
        binding.btnRangeMonth.setOnClickListener {
            currentRange = AnalyticsRange.MONTH
            updateRangeButtons()
            loadAnalyticsData()
            // 强制更新UI
            binding.root.postDelayed({
                updateAnalyticsDisplay()
                updateNutritionDonutChart()
            }, 100)
        }
    }
    
    /**
     * 根据当前范围加载Analytics数据
     */
    private fun loadAnalyticsData() {
        when (currentRange) {
            AnalyticsRange.DAY -> {
                // 日视图：加载今日的时间序列趋势和营养素分析
                val today = getTodayDateString()
                viewModel.loadTimeSeriesTrend(today, today, "day")
                viewModel.loadNutritionAnalysis(today, today, null)
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
     * 获取月视图的日期范围（最近30天）
     * @return Pair(startDate, endDate) 格式为 YYYY-MM-DD
     */
    private fun getMonthDateRange(): Pair<String, String> {
        val calendar = java.util.Calendar.getInstance()
        val endDate = calendar.clone() as java.util.Calendar
        
        // 开始日期：28天前
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -29) // -29 因为包含今天，所以是28天
        
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
        val stats = viewModel.dailyStats.value ?: DailyStats.getInitial()
        val (intakeDisplay, burnedDisplay, labelText) = when (currentRange) {
            AnalyticsRange.DAY -> {
                // 日视图：从活动图表数据或本地统计数据获取
                val chartData = viewModel.activityChartData.value
                if (chartData != null && chartData.dataPoints.isNotEmpty()) {
                    val apiBurned = chartData.dataPoints.first().burned
                    // 如果API返回的burned为null或0，使用DailyStats数据代替
                    val burnedValue = if (apiBurned > 0) apiBurned else stats.burned
                    Triple(
                        chartData.dataPoints.first().intake,
                        burnedValue,
                        getString(R.string.total_intake)
                    )
                } else {
                    // 如果数据未加载，使用本地统计数据
                    Triple(
                        stats.calories.current,
                        stats.burned,
                        getString(R.string.total_intake)
                    )
                }
            }
            AnalyticsRange.WEEK -> {
                // 周视图：计算平均值
                if (chartData.isNotEmpty()) {
                    val avgIntake = chartData.sumOf { it.intake } / chartData.size.toDouble()
                    val avgBurned = chartData.sumOf { it.burned } / chartData.size.toDouble()
                    // 如果API返回的burned为null或0，使用DailyStats数据代替
                    val burnedValue = if (avgBurned > 0) avgBurned else stats.burned
                    Triple(avgIntake, burnedValue, getString(R.string.avg_intake))
                } else {
                    Triple(0.0, stats.burned, getString(R.string.avg_intake))
                }
            }
            AnalyticsRange.MONTH -> {
                // 月视图：计算平均值
                if (chartData.isNotEmpty()) {
                    val avgIntake = chartData.sumOf { it.intake } / chartData.size.toDouble()
                    val avgBurned = chartData.sumOf { it.burned } / chartData.size.toDouble()
                    // 如果API返回的burned为null或0，使用DailyStats数据代替
                    val burnedValue = if (avgBurned > 0) avgBurned else stats.burned
                    Triple(avgIntake, burnedValue, getString(R.string.avg_intake))
                } else {
                    Triple(0.0, stats.burned, getString(R.string.avg_intake))
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
        
        // 更新饼图（从后端 nutrition-analysis API 获取数据）
        updateNutritionDonutChart()
    }
    
    /**
     * 更新营养饼图（甜甜圈图）
     * 从ViewModel获取MacroRatio Model并显示
     */
    private fun updateNutritionDonutChart() {
        val macroRatio = viewModel.macroRatio.value ?: com.example.forhealth.models.MacroRatio.getInitial()
        
        // 使用百分比总和来判断是否有数据（而不是totalCalories，因为后端可能不返回calories项）
        val totalPercent = macroRatio.proteinPercent + macroRatio.carbohydratesPercent + macroRatio.fatPercent
        
        if (totalPercent > 0) {
            // 从activityChartData或dailyStats获取总卡路里用于中心显示
            val totalCaloriesForDisplay = when (currentRange) {
                AnalyticsRange.DAY -> {
                    val chartData = viewModel.activityChartData.value
                    if (chartData != null && chartData.dataPoints.isNotEmpty()) {
                        chartData.dataPoints.first().intake
                    } else {
                        val stats = viewModel.dailyStats.value ?: DailyStats.getInitial()
                        stats.calories.current
                    }
                }
                AnalyticsRange.WEEK, AnalyticsRange.MONTH -> {
                    val chartData = viewModel.activityChartData.value
                    if (chartData != null && chartData.dataPoints.isNotEmpty()) {
                        chartData.dataPoints.sumOf { it.intake } / chartData.dataPoints.size.toDouble()
                    } else {
                        0.0
                    }
                }
            }
            
            // 使用Model数据更新饼图（直接传入百分比）
            binding.macroDonutChart.setMacros(
                macroRatio.proteinPercent,
                macroRatio.carbohydratesPercent,
                macroRatio.fatPercent,
                totalCaloriesForDisplay
            )
            
            // 显示百分比（转换为整数用于显示）
            val proteinPctInt = macroRatio.proteinPercent.toInt()
            val carbsPctInt = macroRatio.carbohydratesPercent.toInt()
            val fatPctInt = macroRatio.fatPercent.toInt()
            
            binding.tvMacroProtein.text = "Protein: ${proteinPctInt}%"
            binding.tvMacroProtein.setTextColor(resources.getColor(R.color.blue_500, null))
            binding.tvMacroCarbs.text = "Carbs: ${carbsPctInt}%"
            binding.tvMacroCarbs.setTextColor(resources.getColor(R.color.amber_400, null))
            binding.tvMacroFat.text = "Fat: ${fatPctInt}%"
            binding.tvMacroFat.setTextColor(resources.getColor(R.color.rose_400, null))
        } else {
            // 如果百分比总和为0，显示灰色圆环和0%
            binding.macroDonutChart.setMacros(0.0, 0.0, 0.0, 0.0)
            binding.tvMacroProtein.text = "Protein: 0%"
            binding.tvMacroProtein.setTextColor(resources.getColor(R.color.blue_500, null))
            binding.tvMacroCarbs.text = "Carbs: 0%"
            binding.tvMacroCarbs.setTextColor(resources.getColor(R.color.amber_400, null))
            binding.tvMacroFat.text = "Fat: 0%"
            binding.tvMacroFat.setTextColor(resources.getColor(R.color.rose_400, null))
        }
    }
    
    /**
     * 生成图表数据（从ViewModel获取Model并转换为View层需要的格式）
     */
    private fun generateChartData(): List<com.example.forhealth.ui.views.ChartDataPoint> {
        return when (currentRange) {
            AnalyticsRange.DAY -> {
                // 日视图：使用活动图表数据或本地统计数据
                val chartData = viewModel.activityChartData.value
                val stats = viewModel.dailyStats.value ?: DailyStats.getInitial()
                
                if (chartData != null && chartData.dataPoints.isNotEmpty()) {
                    // 将Model转换为View层需要的ChartDataPoint
                    // 如果burned值为0，尝试使用dailyStats中的burned值
                    chartData.dataPoints.map { dataPoint ->
                        val burnedValue = if (dataPoint.burned > 0) {
                            dataPoint.burned
                        } else {
                            // 如果API返回的burned为0，使用dailyStats中的burned值
                            stats.burned
                        }
                        com.example.forhealth.ui.views.ChartDataPoint(
                            label = dataPoint.label,
                            intake = dataPoint.intake,
                            burned = burnedValue
                        )
                    }
                } else {
                    // 如果数据未加载，使用本地统计数据创建一个数据点
                    listOf(
                        com.example.forhealth.ui.views.ChartDataPoint(
                            label = "Today",
                            intake = stats.calories.current,
                            burned = stats.burned
                        )
                    )
                }
            }
            AnalyticsRange.WEEK, AnalyticsRange.MONTH -> {
                // 周/月视图：使用活动图表数据
                val chartData = viewModel.activityChartData.value
                if (chartData != null && chartData.dataPoints.isNotEmpty()) {
                    // 将Model转换为View层需要的ChartDataPoint
                    chartData.dataPoints.map {
                        com.example.forhealth.ui.views.ChartDataPoint(
                            label = it.label,
                            intake = it.intake,
                            burned = it.burned
                        )
                    }
                } else {
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
        
        
        // 观察宏量营养素比例数据，更新Analytics视图（饼图）
        viewModel.macroRatio.observe(viewLifecycleOwner) {
            if (binding.scrollAnalytics.visibility == View.VISIBLE) {
                updateAnalyticsDisplay()
            }
        }
    }
    
    /**
     * 从后端加载今日统计数据（初始化圆环和宏量营养素）
     */
    private fun loadTodayStats() {
        viewModel.loadTodayStats()
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
        // 当Fragment重新可见时，先重新加载今日运动记录（确保_exercises有数据）
        viewModel.loadTodayExercises()
        // 重新加载今日饮食记录
        viewModel.loadTodayMeals()
        // 重新加载今日统计数据（确保数据是最新的）
        // 注意：loadTodayStats会优先使用本地_exercises的数据，所以即使并行执行也没问题
        viewModel.loadTodayStats()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
