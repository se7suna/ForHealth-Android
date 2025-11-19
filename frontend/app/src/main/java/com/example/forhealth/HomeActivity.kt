package com.example.forhealth

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.food.FoodSelectionActivity
import com.example.forhealth.food.DailyRecordsAdapter
import com.example.forhealth.sports.SportsSelectionActivity
import com.example.forhealth.model.DailyNutritionSummary
import com.example.forhealth.model.DailyRecordItem
import com.example.forhealth.model.FoodRecord
import com.example.forhealth.model.User
import com.example.forhealth.model.SearchSportRecordsRequest
import com.example.forhealth.model.SearchSportRecordsResponse
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.user.ProfileActivity
import com.example.forhealth.auth.LoginActivity
import com.example.forhealth.utils.PrefsHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var tvDate: TextView
    private lateinit var tvCaloriesIn: TextView
    private lateinit var tvCaloriesOut: TextView
    private lateinit var tvCaloriesRemaining: TextView
    private lateinit var tvCaloriesGoal: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvProtein: TextView
    private lateinit var tvFat: TextView
    private lateinit var rvFoodRecords: RecyclerView
    private lateinit var tvEmptyHint: TextView
    
    private lateinit var btnBreakfast: Button
    private lateinit var btnLunch: Button
    private lateinit var btnDinner: Button
    private lateinit var btnSnack: Button
    private lateinit var btnSports: Button

    private lateinit var dailyRecordsAdapter: DailyRecordsAdapter
    private var dailyCalorieGoal: Double = 2000.0
    private var currentNutritionSummary: DailyNutritionSummary? = null
    private var todaySportsCalories: Double = 0.0
    private var todaySportsRecords: List<SearchSportRecordsResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        supportActionBar?.title = "For Health"
        
        initViews()
        setupRecyclerView()
        setupButtons()
        loadUserProfile()
        loadTodayData()
    }

    override fun onResume() {
        super.onResume()
        // 每次返回页面时刷新数据
        loadTodayData()
    }

    private fun initViews() {
        tvDate = findViewById(R.id.tvDate)
        tvCaloriesIn = findViewById(R.id.tvCaloriesIn)
        tvCaloriesOut = findViewById(R.id.tvCaloriesOut)
        tvCaloriesRemaining = findViewById(R.id.tvCaloriesRemaining)
        tvCaloriesGoal = findViewById(R.id.tvCaloriesGoal)
        tvCarbs = findViewById(R.id.tvCarbs)
        tvProtein = findViewById(R.id.tvProtein)
        tvFat = findViewById(R.id.tvFat)
        rvFoodRecords = findViewById(R.id.rvFoodRecords)
        tvEmptyHint = findViewById(R.id.tvEmptyHint)
        
        btnBreakfast = findViewById(R.id.btnBreakfast)
        btnLunch = findViewById(R.id.btnLunch)
        btnDinner = findViewById(R.id.btnDinner)
        btnSnack = findViewById(R.id.btnSnack)
        btnSports = findViewById(R.id.btnSports)
        
        // 设置日期显示
        val sdf = SimpleDateFormat("M月d日", Locale.CHINA)
        tvDate.text = sdf.format(Date())
    }

    private fun setupRecyclerView() {
        dailyRecordsAdapter = DailyRecordsAdapter(
            onFoodLongClick = { record -> deleteFoodRecord(record) },
            onSportsLongClick = { recordId -> deleteSportsRecord(recordId) }
        )
        rvFoodRecords.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = dailyRecordsAdapter
        }
    }

    private fun setupButtons() {
        btnBreakfast.setOnClickListener { openFoodSelection("早餐") }
        btnLunch.setOnClickListener { openFoodSelection("午餐") }
        btnDinner.setOnClickListener { openFoodSelection("晚餐") }
        btnSnack.setOnClickListener { openFoodSelection("加餐") }
        btnSports.setOnClickListener { openSportsSelection() }
    }

    private fun openFoodSelection(mealType: String) {
        val intent = Intent(this, FoodSelectionActivity::class.java)
        intent.putExtra("meal_type", mealType)
        startActivity(intent)
    }

    private fun openSportsSelection() {
        val intent = Intent(this, SportsSelectionActivity::class.java)
        startActivity(intent)
    }

    private fun loadUserProfile() {
        val token = PrefsHelper.getToken(this)
        if (token == null) {
            redirectToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getProfile("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    dailyCalorieGoal = user.daily_calorie_goal ?: user.tdee ?: 2000.0
                    updateCalorieDisplay()
                }
            } catch (e: Exception) {
                // 如果获取失败，使用默认值
            }
        }
    }

    private fun loadTodayData() {
        val token = PrefsHelper.getToken(this)
        if (token == null) {
            redirectToLogin()
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())

        lifecycleScope.launch {
            try {
                // 先加载运动数据
                loadTodaySportsData()
                
                // 再加载饮食数据
                val response = RetrofitClient.api.getDailyNutrition("Bearer $token", today)
                if (response.isSuccessful && response.body() != null) {
                    currentNutritionSummary = response.body()!!
                    updateUI(currentNutritionSummary!!)
                } else {
                    // 没有数据，显示空状态
                    showEmptyState()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "加载数据失败: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
        }
    }

    private suspend fun loadTodaySportsData() {
        val token = PrefsHelper.getToken(this) ?: return

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val today = sdf.format(Date())

            val request = SearchSportRecordsRequest(
                sportType = null,
                startDate = today,
                endDate = today
            )

            val response = RetrofitClient.api.searchSportsRecords("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                todaySportsRecords = response.body()!!
                todaySportsCalories = todaySportsRecords.sumOf { it.caloriesBurned ?: 0.0 }
            } else {
                todaySportsRecords = emptyList()
                todaySportsCalories = 0.0
            }
        } catch (e: Exception) {
            todaySportsRecords = emptyList()
            todaySportsCalories = 0.0
        }
    }

    private fun updateUI(summary: DailyNutritionSummary) {
        // 更新卡路里数据
        val caloriesIn = summary.totalCalories.toInt()
        val caloriesOut = todaySportsCalories.toInt()
        val remaining = (dailyCalorieGoal - caloriesIn + caloriesOut).toInt()

        tvCaloriesIn.text = caloriesIn.toString()
        tvCaloriesOut.text = caloriesOut.toString()
        tvCaloriesRemaining.text = remaining.toString()
        tvCaloriesGoal.text = "推荐预算 ${dailyCalorieGoal.toInt()}"

        // 更新营养素数据
        val carbsGoal = (dailyCalorieGoal * 0.55 / 4).toInt() // 55%能量来自碳水
        val proteinGoal = (dailyCalorieGoal * 0.15 / 4).toInt() // 15%能量来自蛋白质
        val fatGoal = (dailyCalorieGoal * 0.30 / 9).toInt() // 30%能量来自脂肪

        tvCarbs.text = "${summary.totalCarbohydrates.toInt()} / ${carbsGoal}克"
        tvProtein.text = "${summary.totalProtein.toInt()} / ${proteinGoal}克"
        tvFat.text = "${summary.totalFat.toInt()} / ${fatGoal}克"

        // 合并食物记录和运动记录
        val allRecords = mutableListOf<DailyRecordItem>()
        summary.records.forEach { allRecords.add(DailyRecordItem.FoodItem(it)) }
        todaySportsRecords.forEach { allRecords.add(DailyRecordItem.SportsItem(it)) }
        
        // 更新记录列表
        if (allRecords.isEmpty()) {
            tvEmptyHint.visibility = View.VISIBLE
            rvFoodRecords.visibility = View.GONE
        } else {
            tvEmptyHint.visibility = View.GONE
            rvFoodRecords.visibility = View.VISIBLE
            dailyRecordsAdapter.submitList(allRecords)
        }
    }

    private fun showEmptyState() {
        // 更新卡路里数据（即使没有饮食数据，也要显示运动消耗）
        val caloriesIn = 0
        val caloriesOut = todaySportsCalories.toInt()
        val remaining = (dailyCalorieGoal - caloriesIn + caloriesOut).toInt()
        
        tvCaloriesIn.text = caloriesIn.toString()
        tvCaloriesOut.text = caloriesOut.toString()
        tvCaloriesRemaining.text = remaining.toString()
        tvCaloriesGoal.text = "推荐预算 ${dailyCalorieGoal.toInt()}"

        val carbsGoal = (dailyCalorieGoal * 0.55 / 4).toInt()
        val proteinGoal = (dailyCalorieGoal * 0.15 / 4).toInt()
        val fatGoal = (dailyCalorieGoal * 0.30 / 9).toInt()

        tvCarbs.text = "0 / ${carbsGoal}克"
        tvProtein.text = "0 / ${proteinGoal}克"
        tvFat.text = "0 / ${fatGoal}克"

        // 即使没有食物记录，也可能有运动记录
        val allRecords = todaySportsRecords.map { DailyRecordItem.SportsItem(it) }
        
        if (allRecords.isEmpty()) {
            tvEmptyHint.visibility = View.VISIBLE
            rvFoodRecords.visibility = View.GONE
        } else {
            tvEmptyHint.visibility = View.GONE
            rvFoodRecords.visibility = View.VISIBLE
            dailyRecordsAdapter.submitList(allRecords)
        }
    }

    private fun updateCalorieDisplay() {
        tvCaloriesRemaining.text = dailyCalorieGoal.toInt().toString()
        tvCaloriesGoal.text = "推荐预算 ${dailyCalorieGoal.toInt()}"
    }

    private fun deleteFoodRecord(record: FoodRecord) {
        val token = PrefsHelper.getToken(this) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.deleteFoodRecord("Bearer $token", record.id)
                if (response.isSuccessful) {
                    Toast.makeText(this@HomeActivity, "删除成功", Toast.LENGTH_SHORT).show()
                    loadTodayData() // 重新加载数据
                } else {
                    Toast.makeText(this@HomeActivity, "删除失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteSportsRecord(recordId: String) {
        val token = PrefsHelper.getToken(this) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.deleteSportsRecord("Bearer $token", recordId)
                if (response.isSuccessful) {
                    Toast.makeText(this@HomeActivity, "运动记录删除成功", Toast.LENGTH_SHORT).show()
                    loadTodayData() // 重新加载数据
                } else {
                    Toast.makeText(this@HomeActivity, "删除失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_logout -> {
                PrefsHelper.clearToken(this)
                redirectToLogin()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}


