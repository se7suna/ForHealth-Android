package com.example.forhealth.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.forhealth.R
import com.example.forhealth.databinding.ActivityCameraBinding
import com.example.forhealth.models.FoodItem
import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.ai.ProcessedFoodItem
import com.example.forhealth.network.dto.food.BarcodeScanResponse
import com.example.forhealth.network.dto.food.FoodResponse
import com.example.forhealth.network.safeApiCall
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCameraBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var isScanMode = true // true = 扫码模式, false = 拍照模式
    private var isProcessing = false // 防止重复处理
    
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "需要相机权限才能使用此功能", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
        updateUI()
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
            if (isScanMode) {
                // 扫码模式下，点击按钮可以手动触发识别（可选功能）
                Toast.makeText(this, "请将条形码对准扫描框", Toast.LENGTH_SHORT).show()
            } else {
                // 拍照模式下，点击按钮拍照
                capturePhoto()
            }
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
        
        // 只在扫码模式下启用图像分析
        if (isScanMode) {
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer())
                }
        } else {
            imageAnalysis = null
        }
        
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            cameraProvider.unbindAll()
            
            val useCases = mutableListOf<UseCase>(preview, imageCapture!!)
            if (imageAnalysis != null) {
                useCases.add(imageAnalysis!!)
            }
            
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                *useCases.toTypedArray()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "相机启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun switchToScanMode() {
        if (isProcessing) return
        isScanMode = true
        isProcessing = false
        updateUI()
        bindCameraUseCases()
    }
    
    private fun switchToPhotoMode() {
        if (isProcessing) return
        isScanMode = false
        isProcessing = false
        updateUI()
        bindCameraUseCases()
    }
    
    private fun updateUI() {
        if (isScanMode) {
            // 扫码模式
            binding.btnScanMode.setBackgroundResource(R.drawable.bg_white_20_rounded)
            binding.btnPhotoMode.background = null
            binding.layoutScanFrame.visibility = android.view.View.VISIBLE
            binding.btnCapture.visibility = android.view.View.VISIBLE
            binding.tvHint.text = "将条形码放入框内"
        } else {
            // 拍照模式
            binding.btnScanMode.background = null
            binding.btnPhotoMode.setBackgroundResource(R.drawable.bg_white_20_rounded)
            binding.layoutScanFrame.visibility = android.view.View.GONE
            binding.btnCapture.visibility = android.view.View.VISIBLE
            binding.tvHint.text = "点击按钮拍照识别食物"
        }
    }
    
    private fun capturePhoto() {
        if (isProcessing) return
        
        val imageCapture = imageCapture ?: return
        
        // 创建临时文件保存照片
        val photoFile = File(getExternalFilesDir(null), "temp_food_photo_${System.currentTimeMillis()}.jpg")
        
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // 拍照成功，调用AI识别API
                    recognizeFoodFromImage(photoFile)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    isProcessing = false
                    hideLoading()
                    Toast.makeText(this@CameraActivity, "拍照失败: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    private fun recognizeFoodFromImage(imageFile: File) {
        showLoading("识别中...")
        isProcessing = true
        
        lifecycleScope.launch {
            try {
                // 创建MultipartBody.Part
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)
                
                val result = safeApiCall {
                    RetrofitClient.apiService.recognizeFood(
                        file = body,
                        mealType = null,
                        notes = null,
                        recordedAt = null
                    )
                }
                
                when (result) {
                    is ApiResult.Success -> {
                        val response = result.data
                        if (response.success && response.processed_foods.isNotEmpty()) {
                            // 识别成功，将食物加入购物车
                            val foodItems = convertToFoodItems(response.processed_foods)
                            returnFoodItemsToParent(foodItems)
                        } else {
                            // 识别失败
                            hideLoading()
                            isProcessing = false
                            Toast.makeText(
                                this@CameraActivity,
                                response.message ?: "未识别到食物，请重试",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    is ApiResult.Error -> {
                        hideLoading()
                        isProcessing = false
                        Toast.makeText(
                            this@CameraActivity,
                            "识别失败: ${result.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is ApiResult.Loading -> {
                        // Loading state
                    }
                }
            } catch (e: Exception) {
                hideLoading()
                isProcessing = false
                Toast.makeText(
                    this@CameraActivity,
                    "识别失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // 删除临时文件
                imageFile.delete()
            }
        }
    }
    
    private fun convertToFoodItems(processedFoods: List<ProcessedFoodItem>): List<FoodItem> {
        return processedFoods.map { processed ->
            FoodItem(
                id = processed.food_id,
                name = processed.food_name,
                calories = processed.nutrition_per_serving.calories,
                protein = processed.nutrition_per_serving.protein,
                carbs = processed.nutrition_per_serving.carbohydrates,
                fat = processed.nutrition_per_serving.fat,
                unit = processed.serving_unit,
                gramsPerUnit = processed.serving_size,
                image = "" // AI识别可能没有图片
            )
        }
    }
    
    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {
        private val reader = MultiFormatReader().apply {
            val hints = mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39
                ),
                DecodeHintType.TRY_HARDER to true
            )
            setHints(hints)
        }
        
        override fun analyze(imageProxy: ImageProxy) {
            if (!isScanMode || isProcessing) {
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
                    // 未找到条形码，继续扫描
                } catch (e: Exception) {
                    // 忽略其他错误，继续扫描
                }
            }
            
            imageProxy.close()
        }
    }
    
    private fun handleScanResult(barcode: String) {
        if (isProcessing) return
        
        isProcessing = true
        showLoading("查询中...")
        
        lifecycleScope.launch {
            val result = safeApiCall {
                RetrofitClient.apiService.scanBarcode(barcode)
            }
            
            when (result) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.found && response.food_data != null) {
                        // 找到食物，加入购物车
                        val foodItem = convertFoodResponseToFoodItem(response.food_data)
                        returnFoodItemsToParent(listOf(foodItem))
                    } else {
                        // 未找到食物
                        hideLoading()
                        isProcessing = false
                        Toast.makeText(
                            this@CameraActivity,
                            response.message ?: "未识别到该条形码对应的食品",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                is ApiResult.Error -> {
                    hideLoading()
                    isProcessing = false
                    Toast.makeText(
                        this@CameraActivity,
                        "查询失败: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is ApiResult.Loading -> {
                    // Loading state
                }
            }
        }
    }
    
    private fun convertFoodResponseToFoodItem(foodResponse: FoodResponse): FoodItem {
        return FoodItem(
            id = foodResponse.id,
            name = foodResponse.name,
            calories = foodResponse.nutrition_per_serving.calories,
            protein = foodResponse.nutrition_per_serving.protein,
            carbs = foodResponse.nutrition_per_serving.carbohydrates,
            fat = foodResponse.nutrition_per_serving.fat,
            unit = foodResponse.serving_unit,
            gramsPerUnit = foodResponse.serving_size,
            image = foodResponse.image_url ?: ""
        )
    }
    
    private fun returnFoodItemsToParent(foodItems: List<FoodItem>) {
        hideLoading()
        isProcessing = false
        
        // 使用 JSON 序列化传递食物列表
        val gson = com.google.gson.Gson()
        val foodItemsJson = gson.toJson(foodItems)
        
        val resultIntent = Intent().apply {
            putExtra("mode", if (isScanMode) "scan" else "photo")
            putExtra("food_items_json", foodItemsJson)
            putExtra("food_count", foodItems.size)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    private fun showLoading(text: String) {
        binding.layoutLoading.visibility = android.view.View.VISIBLE
        binding.tvLoadingText.text = text
    }
    
    private fun hideLoading() {
        binding.layoutLoading.visibility = android.view.View.GONE
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
