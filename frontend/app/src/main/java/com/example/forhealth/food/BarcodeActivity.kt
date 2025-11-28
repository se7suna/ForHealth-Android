package com.example.forhealth.food

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class BarcodeActivity : AppCompatActivity() {

    // 扫描回调
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            // 用户取消了扫描
            Toast.makeText(this, "扫描取消", Toast.LENGTH_SHORT).show()
            finish() // 结束当前扫描界面
        } else {
            // 扫描成功
            val barcode = result.contents
            // 将条形码结果传递给上一个界面
            val intent = Intent().apply {
                putExtra("barcode", barcode)
            }
            setResult(RESULT_OK, intent)
            finish() // 扫描完毕后返回
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startBarcodeScan() // 启动扫码界面
    }

    // 启动条形码扫描
    private fun startBarcodeScan() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES) // 支持所有条形码类型
            setPrompt("请扫描条形码")
            setCameraId(0) // 使用后置摄像头
            setBeepEnabled(true) // 扫描后播放提示音
        }
        barcodeLauncher.launch(options) // 启动扫描
    }
}
