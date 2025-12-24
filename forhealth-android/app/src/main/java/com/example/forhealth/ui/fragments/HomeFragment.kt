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
import com.example.forhealth.models.ExerciseTimelineItem
import com.example.forhealth.models.ExerciseItem
import com.example.forhealth.utils.CalculationUtils
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
        
        // 鍒濆鍖?ViewModel
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        
        // 鍒濆鍖?RecyclerView
        timelineAdapter = TimelineAdapter(
            items = emptyList(),
            onMealGroupClick = { mealGroup -> openEditMealDialog(mealGroup) },
            onExerciseClick = { activity -> openEditExerciseDialog(activity) }
        )
        binding.rvTimeline.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTimeline.adapter = timelineAdapter
        
        // 璁剧疆鍒濆鏁版嵁
        setupInitialData()
        
        // 璁剧疆鐐瑰嚮浜嬩欢
        setupClickListeners()
        
        // 璁剧疆Analytics瑙嗗浘鐩戝惉鍣?
        setupAnalyticsListeners()
        updateRangeButtons() // 鍒濆鍖栬寖鍥撮€夋嫨鍣ㄧ姸鎬?
        
        // 瑙傚療鏁版嵁鍙樺寲
        observeData()
        
        // 鍏堝姞杞戒粖鏃ヨ繍鍔ㄨ褰曪紙纭繚_exercises鏈夋暟鎹級
        loadTodayExercises()
        
        // 浠庡悗绔姞杞戒粖鏃ラギ椋熻褰?
        loadTodayMeals()
        
        // 浠庡悗绔姞杞戒粖鏃ョ粺璁℃暟鎹紙鍒濆鍖栧渾鐜拰瀹忛噺钀ュ吇绱狅級
        // 娉ㄦ剰锛氬繀椤诲湪loadTodayExercises()涔嬪悗鎵ц锛屼互渚夸娇鐢╛exercises鐨勬暟鎹?
        loadTodayStats()
        
        // 鍔犺浇鐢ㄦ埛璧勬枡锛堢敤浜庢樉绀虹敤鎴峰悕锛?
        viewModel.loadUserProfile()
    }
    
    private fun setupInitialData() {
        // 璁剧疆鏃ユ湡
        binding.tvDate.text = DateUtils.getCurrentDate()
        
        // 鏄剧ずAI寤鸿锛堥粯璁ゆ樉绀猴級
        binding.cardAiInsight.visibility = View.VISIBLE
        
        // 璁剧疆鍦嗙幆瀹藉害涓哄睆骞曞搴︾殑2/5
        binding.root.post {
            val screenWidth = resources.displayMetrics.widthPixels
            val ringSize = screenWidth / 5 * 2
            val frameLayout = binding.root.findViewById<View>(R.id.frameRingProgress)
            
            // 鍚屾鏇存柊鍦嗙幆鑷韩鐨勫昂瀵革紙淇濇寔鍦嗙幆澶у皬涓嶅彉锛?
            binding.ringProgress.setSize(ringSize)
            
            // 绛夊緟鍦嗙幆娴嬮噺瀹屾垚鍚庡啀璁剧疆FrameLayout澶у皬锛屼娇鍏舵洿绱у噾
            binding.ringProgress.post {
                // FrameLayout澶у皬璁剧疆涓哄渾鐜殑瀹為檯娴嬮噺澶у皬锛岄伩鍏嶅浣欑┖闂?
                val measuredSize = maxOf(binding.ringProgress.measuredWidth, binding.ringProgress.measuredHeight)
                frameLayout?.layoutParams?.width = measuredSize
                frameLayout?.layoutParams?.height = measuredSize
                frameLayout?.requestLayout()
            }
        }
        
        // 鍒濆鍖栫粺璁℃暟鎹?
        updateStatsDisplay(DailyStats.getInitial())
    }
    
    private fun setupClickListeners() {
        // FAB 涓绘寜閽?- 灞曞紑/鏀惰捣鑿滃崟
        binding.fabMain.setOnClickListener {
            toggleFabMenu()
        }
        
        // FAB 椋熺墿鎸夐挳 - 鎵撳紑娣诲姞椋熺墿瑕嗙洊灞?
        binding.btnFabFood.setOnClickListener {
            openAddMealDialog()
        }
        
        // FAB 杩愬姩鎸夐挳 - 鎵撳紑娣诲姞杩愬姩瑕嗙洊灞?
        binding.btnFabExercise.setOnClickListener {
            openAddExerciseDialog()
        }
        
        // 瑙嗗浘鍒囨崲鎸夐挳
        binding.btnToggleView.setOnClickListener {
            toggleViewMode()
        }
        
        // 涓汉璧勬枡鎸夐挳
        binding.btnProfile.setOnClickListener {
            openProfileFragment()
        }
        
        // AI Insight 鎸夐挳 - 鎵撳紑AI鑱婂ぉ鐣岄潰
        binding.cardAiInsight.setOnClickListener {
            openAiChatDialog()
        }
        
        // AI Insight 鍒锋柊鎸夐挳
        binding.btnRefreshAiInsight.setOnClickListener {
            viewModel.refreshAiInsight()
        }
    }
    
    private fun toggleFabMenu() {
        val isVisible = binding.fabMenuContainer.visibility == View.VISIBLE
        
        if (isVisible) {
            // 鏀惰捣鑿滃崟
            binding.fabMenuContainer.visibility = View.GONE
            // 鏃嬭浆鍥炲師浣嶇疆
            binding.fabMain.rotation = 0f
            binding.fabMain.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.emerald_600, null)
            ))
        } else {
            // 灞曞紑鑿滃崟
            binding.fabMenuContainer.visibility = View.VISIBLE
            // 鏃嬭浆45搴﹀苟鏀瑰彉棰滆壊
            binding.fabMain.rotation = 45f
            binding.fabMain.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.slate_800, null)
            ))
        }
    }
    
    private fun openAddMealDialog() {
        // 浣跨敤 DialogFragment 浣滀负瑕嗙洊灞傦紝鏄剧ず娣诲姞椋熺墿鐣岄潰
        val dialog = AddMealFragment().apply {
            // 璁剧疆鍥炶皟锛屽綋娣诲姞鎴愬姛鍚庡埛鏂扮粺璁℃暟鎹?
            setOnMealAddedListener { meals ->
                // 閲嶆柊鍔犺浇浠婃棩缁熻鏁版嵁浠ユ洿鏂板渾鐜拰钀ュ吇绱犵粺璁?
                loadTodayStats()
            }
        }
        dialog.show(parentFragmentManager, "AddMealDialog")
        toggleFabMenu() // 鏀惰捣鑿滃崟
    }
    
    private fun openAddExerciseDialog() {
        fun showDialog(exerciseLibrary: List<com.example.forhealth.models.ExerciseItem>) {
            val dialog = AddExerciseFragment().apply {
                setExerciseLibrary(exerciseLibrary)
                // 璁剧疆鍥炶皟锛屽綋娣诲姞鎴愬姛鍚庨€氳繃API鍒涘缓璁板綍
                setOnExerciseAddedListener { exercises ->
                    viewModel.createExerciseRecords(exercises) { result ->
                        when (result) {
                            is com.example.forhealth.network.ApiResult.Success -> {
                                // 鏁版嵁宸查€氳繃loadTodayExercises鏇存柊
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
            toggleFabMenu() // 鏀惰捣鑿滃崟
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
        // 浣跨敤 DialogFragment 浣滀负瑕嗙洊灞傦紝鏄剧ず涓汉淇℃伅鐣岄潰
        val dialog = ProfileFragment()
        dialog.show(parentFragmentManager, "ProfileDialog")
    }
    
    private fun openAiChatDialog() {
        // 鎵撳紑AI鑱婂ぉ鐣岄潰
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
                // 鍒犻櫎鍚庝篃鍒锋柊缁熻鏁版嵁
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
                    // 鐩存帴浼犻€扐ctivityItem缁橵iewModel锛孷iewModel鍐呴儴浣跨敤ExerciseRepository澶勭悊
                    viewModel.updateExerciseRecord(updatedActivity) { result ->
                        when (result) {
                            is com.example.forhealth.network.ApiResult.Success -> {
                                // 鏁版嵁宸查€氳繃loadTodayExercises鏇存柊
                            }
                            is com.example.forhealth.network.ApiResult.Error -> {
                                android.widget.Toast.makeText(requireContext(), result.message, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            is com.example.forhealth.network.ApiResult.Loading -> {}
                        }
                    }
                }
                setOnExerciseDeletedListener { recordId ->
                    // ViewModel 鐨?deleteExerciseRecord 宸茬粡澶勭悊浜嗗垹闄ゅ拰閲嶆柊鍔犺浇鏁版嵁
                    // 杩欓噷涓嶉渶瑕佸啀娆¤皟鐢紝鍙渶瑕侀€氱煡鍗冲彲
                }
            }
            dialog.show(parentFragmentManager, "EditExerciseDialog")
        }
        
        // 浠庣紦瀛樻垨鍔犺浇杩愬姩搴撴暟鎹?
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
            // 鍒囨崲鍒板垪琛ㄨ鍥?- 鎭㈠header浣嶇疆
            val scrollView = binding.scrollViewMain
            val currentScrollY = scrollView.scrollY
            
            // 浣跨敤ValueAnimator瀹炵幇骞虫粦婊氬姩
            val scrollAnimator = android.animation.ValueAnimator.ofInt(currentScrollY, 0)
            scrollAnimator.duration = 300
            scrollAnimator.interpolator = android.view.animation.DecelerateInterpolator()
            scrollAnimator.addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                scrollView.scrollTo(0, value)
            }
            scrollAnimator.start()
            
            // 鍚屾椂寮€濮嬫贰鍑篴nalytics瑙嗗浘
            binding.scrollAnalytics.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(300)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction {
                    binding.scrollAnalytics.visibility = View.GONE
                }
                .start()
            
            // 鍦ㄥ姩鐢昏繘琛屽埌涓€鍗婃椂寮€濮媡imeline娣″叆锛屽疄鐜版洿娴佺晠鐨勮繃娓?
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
            }, 150) // 鍦ㄥ姩鐢昏繘琛屼竴鍗婃椂寮€濮嬪垏鎹㈣鍥撅紝瀹炵幇閲嶅彔鏁堟灉
            
            binding.tvViewTitle.text = getString(R.string.timeline)
            binding.btnToggleView.setImageResource(R.drawable.ic_bar_chart)
        } else {
            // 鍒囨崲鍒板浘琛ㄨ鍥?- 瀹炵幇涓婃帹鍔ㄧ敾
            binding.rvTimeline.animate()
                .alpha(0f)
                .translationY(-100f)
                .setDuration(300)
                .withEndAction {
                    binding.rvTimeline.visibility = View.GONE
                    binding.scrollAnalytics.alpha = 0f
                    binding.scrollAnalytics.translationY = 100f
                    binding.scrollAnalytics.visibility = View.VISIBLE
                    
                    // 涓婃帹鍔ㄧ敾锛氬皢鏁翠釜NestedScrollView鍚戜笂婊氬姩鍒板簳锛屾帹鍒颁笉鑳藉啀鎺ㄤ负姝?
                    // 寤惰繜涓€涓嬬‘淇濊鍥惧凡缁忓竷灞€瀹屾垚
                    binding.root.post {
                        val scrollView = binding.scrollViewMain
                        // 璁＄畻鏈€澶ф粴鍔ㄨ窛绂伙細鍐呭鎬婚珮搴?- 鍙楂樺害
                        val maxScrollY = scrollView.getChildAt(0).height - scrollView.height
                        // 婊氬姩鍒板簳
                        scrollView.smoothScrollTo(0, maxScrollY.coerceAtLeast(0))
                    }
                    
                    binding.scrollAnalytics.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .withEndAction {
                            // 鍔ㄧ敾瀹屾垚鍚庯紝寮哄埗鍔犺浇鏁版嵁骞舵洿鏂版墍鏈夊浘琛?
                            loadAnalyticsData()
                            // 寤惰繜涓€涓嬬‘淇濇暟鎹姞杞藉畬鎴愬悗鍐嶆洿鏂癠I
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
        // 鏃堕棿鑼冨洿閫夋嫨鍣?
        binding.btnRangeDay.setOnClickListener {
            currentRange = AnalyticsRange.DAY
            updateRangeButtons()
            loadAnalyticsData()
            // 寮哄埗鏇存柊UI
            binding.root.postDelayed({
                updateAnalyticsDisplay()
                updateNutritionDonutChart()
            }, 100)
        }
        binding.btnRangeWeek.setOnClickListener {
            currentRange = AnalyticsRange.WEEK
            updateRangeButtons()
            loadAnalyticsData()
            // 寮哄埗鏇存柊UI
            binding.root.postDelayed({
                updateAnalyticsDisplay()
                updateNutritionDonutChart()
            }, 100)
        }
        binding.btnRangeMonth.setOnClickListener {
            currentRange = AnalyticsRange.MONTH
            updateRangeButtons()
            loadAnalyticsData()
            // 寮哄埗鏇存柊UI
            binding.root.postDelayed({
                updateAnalyticsDisplay()
                updateNutritionDonutChart()
            }, 100)
        }
    }
    
    /**
     * 鏍规嵁褰撳墠鑼冨洿鍔犺浇Analytics鏁版嵁
     */
    private fun loadAnalyticsData() {
        when (currentRange) {
            AnalyticsRange.DAY -> {
                // 鏃ヨ鍥撅細鍔犺浇浠婃棩鐨勬椂闂村簭鍒楄秼鍔垮拰钀ュ吇绱犲垎鏋?
                val today = getTodayDateString()
                viewModel.loadTimeSeriesTrend(today, today, "day")
                viewModel.loadNutritionAnalysis(today, today, null)
            }
            AnalyticsRange.WEEK -> {
                // 鍛ㄨ鍥撅細鍔犺浇鏈€杩?澶╃殑鏃堕棿搴忓垪瓒嬪娍鍜岃惀鍏荤礌鍒嗘瀽
                val (startDate, endDate) = getWeekDateRange()
                viewModel.loadTimeSeriesTrend(startDate, endDate, "day")
                viewModel.loadNutritionAnalysis(startDate, endDate, null)
            }
            AnalyticsRange.MONTH -> {
                // 鏈堣鍥撅細鍔犺浇鏈€杩?8澶╃殑鏃堕棿搴忓垪瓒嬪娍鍜岃惀鍏荤礌鍒嗘瀽
                val (startDate, endDate) = getMonthDateRange()
                viewModel.loadTimeSeriesTrend(startDate, endDate, "week")
                viewModel.loadNutritionAnalysis(startDate, endDate, null)
            }
        }
    }
    
    /**
     * 鑾峰彇浠婂ぉ鐨勬棩鏈熷瓧绗︿覆锛圷YYY-MM-DD锛?
     */
    private fun getTodayDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }
    
    /**
     * 鑾峰彇鍛ㄨ鍥剧殑鏃ユ湡鑼冨洿锛堟渶杩?澶╋級
     * @return Pair(startDate, endDate) 鏍煎紡涓?YYYY-MM-DD
     */
    private fun getWeekDateRange(): Pair<String, String> {
        val calendar = java.util.Calendar.getInstance()
        val endDate = calendar.clone() as java.util.Calendar
        
        // 寮€濮嬫棩鏈燂細7澶╁墠
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -6) // -6 鍥犱负鍖呭惈浠婂ぉ锛屾墍浠ユ槸7澶?
        
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
     * 鑾峰彇鏈堣鍥剧殑鏃ユ湡鑼冨洿锛堟渶杩?0澶╋級
     * @return Pair(startDate, endDate) 鏍煎紡涓?YYYY-MM-DD
     */
    private fun getMonthDateRange(): Pair<String, String> {
        val calendar = java.util.Calendar.getInstance()
        val endDate = calendar.clone() as java.util.Calendar
        
        // 寮€濮嬫棩鏈燂細28澶╁墠
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -29) // -29 鍥犱负鍖呭惈浠婂ぉ锛屾墍浠ユ槸28澶?
        
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

    private fun currentUserWeight(): Double {
        val profileWeight = viewModel.userProfile.value?.weight
        val responseWeight = viewModel.userProfileResponse.value?.weight
        return (profileWeight ?: responseWeight)?.toDouble() ?: 70.0
    }

    private var cachedBurnedFromTimeline: Double? = null

    private fun computeBurnedFromExercises(): Double? {
        val items = viewModel.timelineItems.value ?: return cachedBurnedFromTimeline
        val exercises = items.filterIsInstance<ExerciseTimelineItem>()
        if (exercises.isEmpty()) return cachedBurnedFromTimeline

        val total = exercises.sumOf { recalcCalories(it.activity) }
        cachedBurnedFromTimeline = total
        return total
    }

    private fun recalcCalories(activity: com.example.forhealth.models.ActivityItem): Double {
        val library = viewModel.exerciseLibrary.value ?: emptyList()
        val weightKg = currentUserWeight()
        val match: ExerciseItem? = library.firstOrNull { it.name.equals(activity.name, ignoreCase = true) }
        val durationMinutes = activity.duration.toDouble()
        return if (match != null) {
            CalculationUtils.calculateExerciseCalories(match, durationMinutes, weightKg)
        } else {
            activity.caloriesBurned
        }
    }
    
    private fun updateAnalyticsDisplay() {
        // 根据范围更新 Activity Trend 标题
        val activityTitle = binding.root.findViewById<android.widget.TextView>(R.id.tvActivityTrendTitle)
        activityTitle?.text = if (currentRange == AnalyticsRange.DAY) {
            getString(R.string.todays_activity)
        } else {
            getString(R.string.activity_trend)
        }

        // 生成图表数据并绘制
        val chartData = generateChartData()
        drawActivityChart(chartData)

        // 计算显示值（日：总数，周/月：平均值）
        val stats = viewModel.dailyStats.value ?: DailyStats.getInitial()
        val burnedFromRecords = computeBurnedFromExercises()
        val (intakeDisplay, burnedDisplay, labelText) = when (currentRange) {
            AnalyticsRange.DAY -> {
                // 日视图：优先使用本地运动记录按 MET×体重×时间重算的消耗
                if (chartData.isNotEmpty()) {
                    val firstPoint = chartData.first()
                    val apiBurned = firstPoint.burned
                    val burnedValue = burnedFromRecords ?: if (apiBurned > 0) apiBurned else stats.burned
                    Triple(
                        firstPoint.intake,
                        burnedValue,
                        getString(R.string.total_intake)
                    )
                } else {
                    Triple(
                        stats.calories.current,
                        burnedFromRecords ?: stats.burned,
                        getString(R.string.total_intake)
                    )
                }
            }
            AnalyticsRange.WEEK -> {
                // 周视图：消耗优先用本地记录总和，保持与添加运动一致
                if (chartData.isNotEmpty()) {
                    val totalIntake = chartData.sumOf { it.intake }
                    val burnedValue = burnedFromRecords ?: chartData.sumOf { it.burned }.takeIf { it > 0 } ?: stats.burned
                    Triple(totalIntake, burnedValue, getString(R.string.total_intake))
                } else {
                    Triple(0.0, burnedFromRecords ?: stats.burned, getString(R.string.total_intake))
                }
            }
            AnalyticsRange.MONTH -> {
                // 月视图：消耗优先用本地记录总和，保持与添加运动一致
                if (chartData.isNotEmpty()) {
                    val totalIntake = chartData.sumOf { it.intake }
                    val burnedValue = burnedFromRecords ?: chartData.sumOf { it.burned }.takeIf { it > 0 } ?: stats.burned
                    Triple(totalIntake, burnedValue, getString(R.string.total_intake))
                } else {
                    Triple(0.0, burnedFromRecords ?: stats.burned, getString(R.string.total_intake))
                }
            }
        }

        // 更新标签文本
        binding.tvTotalIntakeLabel.text = labelText
        binding.tvTotalBurnLabel.text = if (currentRange == AnalyticsRange.DAY) getString(R.string.total_burn) else getString(R.string.avg_burn)

        // 更新数值
        binding.tvTotalIntake.text = "${Math.round(intakeDisplay)} ${getString(R.string.kcal)}"
        binding.tvTotalBurned.text = "${Math.round(burnedDisplay)} ${getString(R.string.kcal)}"

        // 计算并显示 Sum
        val sumValue = intakeDisplay - burnedDisplay
        val sumLabel = if (currentRange == AnalyticsRange.DAY) getString(R.string.sum) else getString(R.string.avg_sum)
        binding.tvSumLabel.text = sumLabel
        binding.tvSum.text = "${Math.round(sumValue)} ${getString(R.string.kcal)}"

        // 更新营养饼图
        updateNutritionDonutChart()
    }
    
    /**
     * 鏇存柊钀ュ吇楗煎浘锛堢敎鐢滃湀鍥撅級
     * 浠嶸iewModel鑾峰彇MacroRatio Model骞舵樉绀?
     */
    private fun updateNutritionDonutChart() {
        val macroRatio = viewModel.macroRatio.value ?: com.example.forhealth.models.MacroRatio.getInitial()
        
        // 浣跨敤鐧惧垎姣旀€诲拰鏉ュ垽鏂槸鍚︽湁鏁版嵁锛堣€屼笉鏄痶otalCalories锛屽洜涓哄悗绔彲鑳戒笉杩斿洖calories椤癸級
        val totalPercent = macroRatio.proteinPercent + macroRatio.carbohydratesPercent + macroRatio.fatPercent
        
        if (totalPercent > 0) {
            // 浠巃ctivityChartData鎴杁ailyStats鑾峰彇鎬诲崱璺噷鐢ㄤ簬涓績鏄剧ず
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
            
            // 浣跨敤Model鏁版嵁鏇存柊楗煎浘锛堢洿鎺ヤ紶鍏ョ櫨鍒嗘瘮锛?
            binding.macroDonutChart.setMacros(
                macroRatio.proteinPercent,
                macroRatio.carbohydratesPercent,
                macroRatio.fatPercent,
                totalCaloriesForDisplay
            )
            
            // 鏄剧ず鐧惧垎姣旓紙杞崲涓烘暣鏁扮敤浜庢樉绀猴級
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
            // 濡傛灉鐧惧垎姣旀€诲拰涓?锛屾樉绀虹伆鑹插渾鐜拰0%
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
     * 鐢熸垚鍥捐〃鏁版嵁锛堜粠ViewModel鑾峰彇Model骞惰浆鎹负View灞傞渶瑕佺殑鏍煎紡锛?
     */    private fun generateChartData(): List<com.example.forhealth.ui.views.ChartDataPoint> {
        return when (currentRange) {
            AnalyticsRange.DAY -> {
                // 日视图：使用后端数据，但若可用则用本地重算的消耗覆盖
                val apiChart = viewModel.activityChartData.value
                val stats = viewModel.dailyStats.value ?: DailyStats.getInitial()
                val burnedFromRecords = computeBurnedFromExercises()

                if (apiChart != null && apiChart.dataPoints.isNotEmpty()) {
                    apiChart.dataPoints.map { dataPoint ->
                        val burnedValue = burnedFromRecords ?: if (dataPoint.burned > 0) dataPoint.burned else stats.burned
                        com.example.forhealth.ui.views.ChartDataPoint(
                            label = dataPoint.label,
                            intake = dataPoint.intake,
                            burned = burnedValue
                        )
                    }
                } else {
                    listOf(
                        com.example.forhealth.ui.views.ChartDataPoint(
                            label = "Today",
                            intake = stats.calories.current,
                            burned = burnedFromRecords ?: stats.burned
                        )
                    )
                }
            }
            AnalyticsRange.WEEK, AnalyticsRange.MONTH -> {
                val apiChart = viewModel.activityChartData.value
                if (apiChart != null && apiChart.dataPoints.isNotEmpty()) {
                    apiChart.dataPoints.map {
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
            // 鏃ヨ鍥句娇鐢ㄦ煴鐘跺浘
            val chartView = com.example.forhealth.ui.views.ActivityBarChartView(requireContext())
            chartView.setData(data)
            binding.viewActivityChart.addView(chartView)
        } else {
            // 鍛?鏈堣鍥句娇鐢ㄦ姌绾垮浘
            val chartView = com.example.forhealth.ui.views.ActivityTrendChartView(requireContext())
            chartView.setData(data)
            binding.viewActivityChart.addView(chartView)
        }
    }
    
    private fun observeData() {
        // 瑙傚療缁熻鏁版嵁
        viewModel.dailyStats.observe(viewLifecycleOwner) { stats ->
            updateStatsDisplay(stats)
        }
        
        // 瑙傚療鏃堕棿绾挎暟鎹?
        viewModel.timelineItems.observe(viewLifecycleOwner) { items ->
            val recalculated = items.map { item ->
                if (item is ExerciseTimelineItem) {
                    val activity = item.activity
                    val newCalories = recalcCalories(activity)
                    item.copy(activity = activity.copy(caloriesBurned = newCalories))
                } else item
            }
            timelineAdapter.updateItems(recalculated)
            cachedBurnedFromTimeline = recalculated
                .filterIsInstance<ExerciseTimelineItem>()
                .sumOf { it.activity.caloriesBurned }
            // 时间线更新后，用最新运动消耗刷新仪表盘/首页数值
            viewModel.dailyStats.value?.let { updateStatsDisplay(it) }
        }
        
        // 瑙傚療AI寤鸿
        viewModel.aiSuggestion.observe(viewLifecycleOwner) { suggestion ->
            if (suggestion.isNotEmpty()) {
                binding.tvAiSuggestion.text = suggestion
                binding.cardAiInsight.visibility = View.VISIBLE
            } else {
                binding.cardAiInsight.visibility = View.GONE
            }
        }
        
        // 瑙傚療鐢ㄦ埛璧勬枡锛屾洿鏂伴棶鍊欒
        viewModel.userProfileResponse.observe(viewLifecycleOwner) { profileResponse ->
            profileResponse?.username?.let { username ->
                binding.tvGreeting.text = "Hello, $username"
            } ?: run {
                binding.tvGreeting.text = getString(R.string.hello_user)
            }
        }
        
        
        // 瑙傚療瀹忛噺钀ュ吇绱犳瘮渚嬫暟鎹紝鏇存柊Analytics瑙嗗浘锛堥ゼ鍥撅級
        viewModel.macroRatio.observe(viewLifecycleOwner) {
            if (binding.scrollAnalytics.visibility == View.VISIBLE) {
                updateAnalyticsDisplay()
            }
        }
    }
    
    /**
     * 浠庡悗绔姞杞戒粖鏃ョ粺璁℃暟鎹紙鍒濆鍖栧渾鐜拰瀹忛噺钀ュ吇绱狅級
     */
    private fun loadTodayStats() {
        viewModel.loadTodayStats()
    }
    
    /**
     * 浠庡悗绔姞杞戒粖鏃ラギ椋熻褰?
     */
    private fun loadTodayMeals() {
        viewModel.loadTodayMeals()
    }
    
    /**
     * 浠庡悗绔姞杞戒粖鏃ヨ繍鍔ㄨ褰?
     */
    private fun loadTodayExercises() {
        viewModel.loadTodayExercises()
    }
    
    private fun updateStatsDisplay(stats: DailyStats) {
        // 优先使用本地记录的运动消耗，回退到后端统计
        val burnedFromRecords = computeBurnedFromExercises()
        val burned = burnedFromRecords ?: stats.burned

        // 更新卡路里显示
        val netCalories = stats.calories.current - burned
        binding.tvNetCalories.text = Math.round(netCalories).toString()
        binding.tvCaloriesIn.text = Math.round(stats.calories.current).toString()
        binding.tvCaloriesBurned.text = Math.round(burned).toString()
        binding.tvTarget.text = getString(R.string.TDEE) + ": ${stats.calories.target}"
        
        // 鏇存柊鐜舰杩涘害
        binding.ringProgress.setProgress(netCalories, stats.calories.target)
        
        // 鏇存柊瀹忛噺钀ュ吇绱?
        binding.tvCarbs.text = "${Math.round(stats.carbs.current)}/${stats.carbs.target}g"
        binding.tvFat.text = "${Math.round(stats.fat.current)}/${stats.fat.target}g"
        binding.tvProtein.text = "${Math.round(stats.protein.current)}/${stats.protein.target}g"
        
        // 鏇存柊杩涘害鏉?
        binding.progressCarbs.setProgress(stats.carbs.current, stats.carbs.target, R.color.amber_400)
        binding.progressFat.setProgress(stats.fat.current, stats.fat.target, R.color.rose_400)
        binding.progressProtein.setProgress(stats.protein.current, stats.protein.target, R.color.blue_400)
    }
    
    override fun onResume() {
        super.onResume()
        // 褰揊ragment閲嶆柊鍙鏃讹紝鍏堥噸鏂板姞杞戒粖鏃ヨ繍鍔ㄨ褰曪紙纭繚_exercises鏈夋暟鎹級
        viewModel.loadTodayExercises()
        // 閲嶆柊鍔犺浇浠婃棩楗璁板綍
        viewModel.loadTodayMeals()
        // 閲嶆柊鍔犺浇浠婃棩缁熻鏁版嵁锛堢‘淇濇暟鎹槸鏈€鏂扮殑锛?
        // 娉ㄦ剰锛歭oadTodayStats浼氫紭鍏堜娇鐢ㄦ湰鍦癬exercises鐨勬暟鎹紝鎵€浠ュ嵆浣垮苟琛屾墽琛屼篃娌￠棶棰?
        viewModel.loadTodayStats()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




