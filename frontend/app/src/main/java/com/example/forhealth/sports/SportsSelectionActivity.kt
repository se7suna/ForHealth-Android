package com.example.forhealth.sports

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.example.forhealth.model.LogSportsRequest
import com.example.forhealth.model.SearchSportsResponse
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.utils.PrefsHelper
import com.example.forhealth.auth.LoginActivity
import android.content.Intent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SportsSelectionActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var rvSports: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private lateinit var sportsAdapter: SportsAdapter
    private var allSports = listOf<SearchSportsResponse>()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_sports_selection)
            
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "选择运动类型"
            
            initViews()
            setupRecyclerView()
            setupSearch()
            loadSportsTypes()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "页面加载失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        rvSports = findViewById(R.id.rvSports)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
    }

    private fun setupRecyclerView() {
        sportsAdapter = SportsAdapter { sport ->
            showDurationDialog(sport)
        }
        rvSports.apply {
            layoutManager = LinearLayoutManager(this@SportsSelectionActivity)
            adapter = sportsAdapter
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    filterSports(s?.toString() ?: "")
                }
            }
        })
    }

    private fun loadSportsTypes() {
        val token = PrefsHelper.getToken(this)
        if (token == null) {
            redirectToLogin()
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getAvailableSportsTypes("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    allSports = response.body()!!
                    if (allSports.isEmpty()) {
                        showEmpty(true)
                    } else {
                        showEmpty(false)
                        sportsAdapter.submitList(allSports)
                    }
                } else {
                    Toast.makeText(this@SportsSelectionActivity, 
                        "加载失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                    showEmpty(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@SportsSelectionActivity, 
                    "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmpty(true)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun filterSports(keyword: String) {
        if (keyword.isBlank()) {
            sportsAdapter.submitList(allSports)
            showEmpty(allSports.isEmpty())
        } else {
            val filtered = allSports.filter { 
                it.sportType?.contains(keyword, ignoreCase = true) == true ||
                it.describe?.contains(keyword, ignoreCase = true) == true
            }
            sportsAdapter.submitList(filtered)
            showEmpty(filtered.isEmpty())
        }
    }

    private fun showDurationDialog(sport: SearchSportsResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_sports_duration, null)
        val tvSportType = dialogView.findViewById<TextView>(R.id.tvDialogSportType)
        val tvSportInfo = dialogView.findViewById<TextView>(R.id.tvSportInfo)
        val etDuration = dialogView.findViewById<EditText>(R.id.etDuration)

        tvSportType.text = sport.sportType ?: "未知运动"
        tvSportInfo.text = "METs: ${sport.mets ?: 0.0} | ${sport.describe ?: ""}"

        AlertDialog.Builder(this)
            .setTitle("记录运动")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val durationStr = etDuration.text.toString()
                if (durationStr.isBlank()) {
                    Toast.makeText(this, "请输入运动时长", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val duration = durationStr.toIntOrNull()
                if (duration == null || duration <= 0) {
                    Toast.makeText(this, "请输入有效的运动时长", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                logSports(sport, duration)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun logSports(sport: SearchSportsResponse, duration: Int) {
        val token = PrefsHelper.getToken(this) ?: return

        lifecycleScope.launch {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val now = sdf.format(Date())

                val request = LogSportsRequest(
                    sportType = sport.sportType ?: "",
                    createdAt = now,
                    durationTime = duration
                )

                val response = RetrofitClient.api.logSports("Bearer $token", request)
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@SportsSelectionActivity, 
                        "运动记录成功！", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@SportsSelectionActivity, 
                        "记录失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@SportsSelectionActivity, 
                    "记录失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        rvSports.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmpty(show: Boolean) {
        tvEmpty.visibility = if (show) View.VISIBLE else View.GONE
        rvSports.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun redirectToLogin() {
        Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

