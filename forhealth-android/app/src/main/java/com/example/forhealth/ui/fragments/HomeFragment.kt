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
            onWorkoutGroupClick = { workoutGroup -> openEditWorkoutDialog(workoutGroup) }
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
        // 使用 DialogFragment 作为覆盖层，显示添加运动界面
        val dialog = AddExerciseFragment().apply {
            // 设置回调，当添加成功后更新数据
            setOnExerciseAddedListener { exercises ->
                viewModel.addExercises(exercises)
            }
        }
        dialog.show(parentFragmentManager, "AddExerciseDialog")
        toggleFabMenu() // 收起菜单
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
    
    private fun openEditWorkoutDialog(workoutGroup: com.example.forhealth.models.WorkoutGroup) {
        val dialog = EditWorkoutFragment().apply {
            setWorkoutGroup(workoutGroup)
            setOnWorkoutUpdatedListener { updatedGroup ->
                viewModel.updateWorkoutGroup(updatedGroup)
            }
            setOnWorkoutDeletedListener { workoutGroupId ->
                viewModel.deleteWorkoutGroup(workoutGroupId)
            }
        }
        dialog.show(parentFragmentManager, "EditWorkoutDialog")
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
                    
                    // 更新Analytics视图
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
            updateAnalyticsDisplay()
        }
        binding.btnRangeWeek.setOnClickListener {
            currentRange = AnalyticsRange.WEEK
            updateRangeButtons()
            updateAnalyticsDisplay()
        }
        binding.btnRangeMonth.setOnClickListener {
            currentRange = AnalyticsRange.MONTH
            updateRangeButtons()
            updateAnalyticsDisplay()
        }
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
        val meals = viewModel.meals.value ?: emptyList()
        val exercises = viewModel.exercises.value ?: emptyList()
        
        // 根据范围更新Activity Trend标题
        val activityTitle = binding.root.findViewById<android.widget.TextView>(R.id.tvActivityTrendTitle)
        activityTitle?.text = if (currentRange == AnalyticsRange.DAY) {
            getString(R.string.todays_activity)
        } else {
            getString(R.string.activity_trend)
        }
        
        // 生成图表数据
        val chartData = generateChartData(stats, meals, exercises)
        drawActivityChart(chartData)
        
        // 计算显示值（日：总数，周/月：平均值）
        val (intakeDisplay, burnedDisplay, labelText) = when (currentRange) {
            AnalyticsRange.DAY -> {
                Triple(stats.calories.current, stats.burned, getString(R.string.total_intake))
            }
            AnalyticsRange.WEEK -> {
                val avgIntake = chartData.sumOf { it.intake } / 7.0
                val avgBurned = chartData.sumOf { it.burned } / 7.0
                Triple(avgIntake, avgBurned, getString(R.string.avg_intake))
            }
            AnalyticsRange.MONTH -> {
                val avgIntake = chartData.sumOf { it.intake } / 28.0
                val avgBurned = chartData.sumOf { it.burned } / 28.0
                Triple(avgIntake, avgBurned, getString(R.string.avg_intake))
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
        
        // 计算宏量营养素（基于卡路里）
        val proteinCal = stats.protein.current * 4
        val carbsCal = stats.carbs.current * 4
        val fatCal = stats.fat.current * 9
        val totalMacroCal = proteinCal + carbsCal + fatCal
        
        // 计算总摄入量（根据当前范围）
        val totalIntakeValue = when (currentRange) {
            AnalyticsRange.DAY -> intakeDisplay
            AnalyticsRange.WEEK -> chartData.sumOf { it.intake }
            AnalyticsRange.MONTH -> chartData.sumOf { it.intake }
        }
        
        // 检查是否有记录（meals或exercises不为空）
        val hasRecords = meals.isNotEmpty() || exercises.isNotEmpty()
        
        // 更新甜甜圈图（如果没有记录，传入0值以显示灰色圆环）
        if (hasRecords) {
            binding.macroDonutChart.setMacros(proteinCal, carbsCal, fatCal, totalIntakeValue)
        } else {
            binding.macroDonutChart.setMacros(0.0, 0.0, 0.0, 0.0)
        }
        
        if (totalMacroCal > 0) {
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
            binding.tvMacroProtein.text = "Protein: 0%"
            binding.tvMacroProtein.setTextColor(resources.getColor(R.color.blue_500, null))
            binding.tvMacroCarbs.text = "Carbs: 0%"
            binding.tvMacroCarbs.setTextColor(resources.getColor(R.color.amber_400, null))
            binding.tvMacroFat.text = "Fat: 0%"
            binding.tvMacroFat.setTextColor(resources.getColor(R.color.rose_400, null))
        }
    }
    
    private fun generateChartData(
        stats: DailyStats,
        meals: List<com.example.forhealth.models.MealItem>,
        exercises: List<com.example.forhealth.models.ActivityItem>
    ): List<com.example.forhealth.ui.views.ChartDataPoint> {
        return when (currentRange) {
            AnalyticsRange.DAY -> {
                // 日视图：单个数据点（用于柱状图）
                val totalCals = meals.sumOf { it.calories }.toDouble()
                val totalBurn = exercises.sumOf { it.caloriesBurned }.toDouble()
                
                listOf(
                    com.example.forhealth.ui.views.ChartDataPoint(
                        label = "Today",
                        intake = if (totalCals > 0) totalCals else 1800.0,
                        burned = if (totalBurn > 0) totalBurn else 400.0
                    )
                )
            }
            AnalyticsRange.WEEK -> {
                // 周视图：7天
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val totalCals = meals.sumOf { it.calories }.toDouble()
                val totalBurn = exercises.sumOf { it.caloriesBurned }.toDouble()
                
                days.mapIndexed { index, day ->
                    if (day == "Tue") {
                        // 当前日使用真实数据
                        com.example.forhealth.ui.views.ChartDataPoint(
                            label = day,
                            intake = if (totalCals > 0) totalCals else 1800.0,
                            burned = if (totalBurn > 0) totalBurn else 400.0
                        )
                    } else {
                        // 其他天使用模拟数据
                        com.example.forhealth.ui.views.ChartDataPoint(
                            label = day,
                            intake = 1800.0 + Math.random() * 600 - 300,
                            burned = 400.0 + Math.random() * 300 - 100
                        )
                    }
                }
            }
            AnalyticsRange.MONTH -> {
                // 月视图：4周
                val weeks = listOf("W1", "W2", "W3", "W4")
                weeks.map { week ->
                    com.example.forhealth.ui.views.ChartDataPoint(
                        label = week,
                        intake = 12000.0 + Math.random() * 2000,
                        burned = 3000.0 + Math.random() * 1000
                    )
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
            // 如果Analytics视图可见，也更新它
            if (binding.scrollAnalytics.visibility == View.VISIBLE) {
                updateAnalyticsDisplay()
            }
        }
        
        // 观察meals和exercises变化，更新Analytics视图
        viewModel.meals.observe(viewLifecycleOwner) {
            if (binding.scrollAnalytics.visibility == View.VISIBLE) {
                updateAnalyticsDisplay()
            }
        }
        
        viewModel.exercises.observe(viewLifecycleOwner) {
            if (binding.scrollAnalytics.visibility == View.VISIBLE) {
                updateAnalyticsDisplay()
            }
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
