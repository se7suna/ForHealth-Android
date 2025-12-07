package com.example.forhealth.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.forhealth.R
import com.example.forhealth.databinding.ActivityCameraBinding
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCameraBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var isScanMode = true
    
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
        checkCameraPermission()
    }
    
    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            finish()
        }
        
        binding.btnScanMode.setOnClickListener {
            switchToScanMode()
        }
        
        binding.btnPhotoMode.setOnClickListener {
            switchToPhotoMode()
        }
        
        binding.btnCapture.setOnClickListener {
            capturePhoto()
        }
    }
    
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }
        
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, BarcodeAnalyzer())
            }
        
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            cameraProvider.unbindAll()
            
            val camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                if (isScanMode) imageAnalysis else null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun switchToScanMode() {
        isScanMode = true
        updateUI()
        bindCameraUseCases()
    }
    
    private fun switchToPhotoMode() {
        isScanMode = false
        updateUI()
        bindCameraUseCases()
    }
    
    private fun updateUI() {
        if (isScanMode) {
            binding.btnScanMode.setBackgroundResource(R.drawable.bg_white_20_rounded)
            binding.btnPhotoMode.background = null
            binding.layoutScanFrame.visibility = android.view.View.VISIBLE
            binding.btnCapture.visibility = android.view.View.GONE
            binding.tvHint.text = getString(R.string.scanning)
        } else {
            binding.btnScanMode.background = null
            binding.btnPhotoMode.setBackgroundResource(R.drawable.bg_white_20_rounded)
            binding.layoutScanFrame.visibility = android.view.View.GONE
            binding.btnCapture.visibility = android.view.View.VISIBLE
            binding.tvHint.text = getString(R.string.take_photo)
        }
    }
    
    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return
        
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "food_${System.currentTimeMillis()}.jpg")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
        ).build()
        
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    if (savedUri != null) {
                        // 返回结果给调用者
                        val resultIntent = Intent().apply {
                            putExtra("photo_uri", savedUri.toString())
                            putExtra("mode", "photo")
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {
        private val reader = MultiFormatReader().apply {
            val hints = mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.QR_CODE
                ),
                DecodeHintType.TRY_HARDER to true
            )
            setHints(hints)
        }
        
        override fun analyze(imageProxy: ImageProxy) {
            if (!isScanMode) {
                imageProxy.close()
                return
            }
            
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                try {
                    // 获取Y平面数据（灰度数据）
                    val yBuffer = mediaImage.planes[0].buffer
                    val ySize = yBuffer.remaining()
                    val yArray = ByteArray(ySize)
                    yBuffer.get(yArray)
                    
                    val width = mediaImage.width
                    val height = mediaImage.height
                    
                    // 创建YUV源（只使用Y平面）
                    val source = PlanarYUVLuminanceSource(
                        yArray,
                        width,
                        height,
                        0,
                        0,
                        width,
                        height,
                        false
                    )
                    
                    val bitmap = BinaryBitmap(HybridBinarizer(source))
                    val result = reader.decode(bitmap)
                    
                    runOnUiThread {
                        handleScanResult(result.text)
                    }
                } catch (e: NotFoundException) {
                    // 未找到二维码，继续扫描
                } catch (e: Exception) {
                    // 忽略其他错误，继续扫描
                }
            }
            
            imageProxy.close()
        }
    }
    
    private fun handleScanResult(result: String) {
        Toast.makeText(this, getString(R.string.scan_success), Toast.LENGTH_SHORT).show()
        
        val resultIntent = Intent().apply {
            putExtra("barcode", result)
            putExtra("mode", "scan")
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

