package com.example.forhealth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.forhealth.user.ProfileActivity
import com.example.forhealth.auth.LoginActivity
import com.example.forhealth.utils.PrefsHelper
import com.example.forhealth.user.MainActivity
class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val btnGoToMain = findViewById<Button>(R.id.btnGoToMain)

        // 设置按钮点击事件
        btnGoToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)  // 创建跳转到 MainActivity 的 Intent
            startActivity(intent)  // 启动活动
        }
        // 设置ActionBar标题
        supportActionBar?.title = "For Health"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                // 跳转到个人信息页面
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_logout -> {
                // 退出登录
                PrefsHelper.clearToken(this)
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}


