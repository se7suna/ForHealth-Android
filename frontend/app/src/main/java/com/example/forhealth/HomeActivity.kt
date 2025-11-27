package com.example.forhealth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.food.DailyRecordsAdapter
import com.example.forhealth.food.FoodSelectionActivity
import com.example.forhealth.model.DailyNutritionSummary
import com.example.forhealth.model.DailyRecordItem
import com.example.forhealth.model.FoodRecord
import com.example.forhealth.model.SearchSportRecordsResponse
import com.example.forhealth.sports.SportsSelectionActivity
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
    private var todaySportsRecords: MutableList<SearchSportRecordsResponse> = mutableListOf()
    private var todayFoodRecords: MutableList<FoodRecord> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        supportActionBar?.title = "For Health"

        initViews()
        setupRecyclerView()
        setupButtons()
        loadFakeData()
        updateUIWithFakeData()
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

        // 显示今天日期
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

    private fun loadFakeData() {
        // 这里手动构造假数据


        todaySportsCalories = todaySportsRecords.sumOf { it.caloriesBurned ?: 0.0 }

        // 计算总摄入卡路里，蛋白质，脂肪，碳水
    }

    private fun updateUIWithFakeData() {
        currentNutritionSummary?.let { summary ->
            val caloriesIn = summary.totalCalories.toInt()
            val caloriesOut = todaySportsCalories.toInt()
            val remaining = (dailyCalorieGoal - caloriesIn + caloriesOut).toInt()

            tvCaloriesIn.text = caloriesIn.toString()
            tvCaloriesOut.text = caloriesOut.toString()
            tvCaloriesRemaining.text = remaining.toString()
            tvCaloriesGoal.text = "推荐预算 ${dailyCalorieGoal.toInt()}"

            val carbsGoal = (dailyCalorieGoal * 0.55 / 4).toInt()
            val proteinGoal = (dailyCalorieGoal * 0.15 / 4).toInt()
            val fatGoal = (dailyCalorieGoal * 0.30 / 9).toInt()

            tvCarbs.text = "${summary.totalCarbohydrates.toInt()} / ${carbsGoal}克"
            tvProtein.text = "${summary.totalProtein.toInt()} / ${proteinGoal}克"
            tvFat.text = "${summary.totalFat.toInt()} / ${fatGoal}克"

            // 合并食物和运动记录显示
            val allRecords = mutableListOf<DailyRecordItem>()
            summary.records.forEach { allRecords.add(DailyRecordItem.FoodItem(it)) }
            todaySportsRecords.forEach { allRecords.add(DailyRecordItem.SportsItem(it)) }

            if (allRecords.isEmpty()) {
                tvEmptyHint.visibility = View.VISIBLE
                rvFoodRecords.visibility = View.GONE
            } else {
                tvEmptyHint.visibility = View.GONE
                rvFoodRecords.visibility = View.VISIBLE
                dailyRecordsAdapter.submitList(allRecords)
            }
        }
    }

    private fun deleteFoodRecord(record: FoodRecord) {
        todayFoodRecords.remove(record)
        loadFakeData()
        updateUIWithFakeData()
        Toast.makeText(this, "删除食物记录", Toast.LENGTH_SHORT).show()
    }

    private fun deleteSportsRecord(recordId: String) {
        todaySportsRecords.removeAll { it.recordId == recordId }
        todaySportsCalories = todaySportsRecords.sumOf { it.caloriesBurned ?: 0.0 }
        loadFakeData()
        updateUIWithFakeData()
        Toast.makeText(this, "删除运动记录", Toast.LENGTH_SHORT).show()
    }
}



