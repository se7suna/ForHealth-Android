package com.example.forhealth.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import android.content.res.ColorStateList
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
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
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
        Log.d("CameraActivity", "Permission request result: $isGranted")
        if (isGranted) {
            Log.d("CameraActivity", "Camera permission granted by user, starting camera")
            Toast.makeText(this, "相机权限已获得", Toast.LENGTH_SHORT).show()
            startCamera()
        } else {
            Log.w("CameraActivity", "Camera permission denied by user")

            // 检查是否是永久拒绝
            if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Log.w("CameraActivity", "Permission permanently denied")
                showPermanentlyDeniedDialog()
            } else {
                Log.w("CameraActivity", "Permission denied, show retry option")
                showPermissionDeniedUI()
            }
        }
    }

    private fun showPermanentlyDeniedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("需要相机权限")
            .setMessage("相机权限被永久拒绝。请前往应用设置中手动开启相机权限。")
            .setPositiveButton("前往设置") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                showPermissionDeniedUI()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = android.net.Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("CameraActivity", "Failed to open app settings", e)
            Toast.makeText(this, "无法打开设置页面", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("CameraActivity", "onCreate called")

        // 检查设备是否有摄像头
        if (!checkCameraHardware()) {
            Toast.makeText(this, "设备没有摄像头", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 确保 PreviewView 可见
        binding.previewView.visibility = android.view.View.VISIBLE
        Log.d("CameraActivity", "PreviewView visibility set to VISIBLE")

        setupClickListeners()
        applyDarkModeButtons()
        updateUI()

        // 立即检查并请求权限
        checkCameraPermission()
    }

    private fun checkCameraHardware(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
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
        Log.d("CameraActivity", "Checking camera permission...")

        val permission = Manifest.permission.CAMERA
        val permissionStatus = ContextCompat.checkSelfPermission(this, permission)

        Log.d("CameraActivity", "Current permission status: $permissionStatus")
        Log.d("CameraActivity", "PERMISSION_GRANTED = ${PackageManager.PERMISSION_GRANTED}")

        when (permissionStatus) {
            PackageManager.PERMISSION_GRANTED -> {
                Log.d("CameraActivity", "Camera permission already granted")
                startCamera()
            }
            else -> {
                Log.d("CameraActivity", "Camera permission not granted, requesting permission...")

                if (shouldShowRequestPermissionRationale(permission)) {
                    Log.d("CameraActivity", "Should show permission rationale")
                    // 显示解释为什么需要权限
                    showPermissionRationaleDialog()
                } else {
                    Log.d("CameraActivity", "Directly requesting camera permission")
                    requestCameraPermission()
                }
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("需要相机权限")
            .setMessage("本应用需要相机权限来扫描条形码和拍照识别食物。请允许应用使用相机。")
            .setPositiveButton("允许") { _, _ ->
                requestCameraPermission()
            }
            .setNegativeButton("拒绝") { dialog, _ ->
                dialog.dismiss()
                showPermissionDeniedUI()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestCameraPermission() {
        Log.d("CameraActivity", "Launching permission request")
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun showPermissionDeniedUI() {
        binding.tvHint.text = "需要相机权限才能使用此功能，请点击下方按钮重新申请"
        binding.btnCapture.setOnClickListener {
            checkCameraPermission()
        }
        Toast.makeText(this, "没有相机权限无法使用扫码和拍照功能", Toast.LENGTH_LONG).show()
    }

    private fun startCamera() {
        Log.d("CameraActivity", "Starting camera with simplified approach...")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                this.cameraProvider = cameraProvider

                // 解绑之前的用例
                cameraProvider.unbindAll()

                // 创建Preview用例
                val preview = Preview.Builder().build()

                // 创建ImageCapture用例
                imageCapture = ImageCapture.Builder().build()

                // 选择摄像头
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // 绑定预览到PreviewView
                    preview.setSurfaceProvider(binding.previewView.surfaceProvider)

                    // 准备用例列表
                    val useCases = mutableListOf<UseCase>(preview, imageCapture!!)

                    // 在扫码模式下添加图像分析
                    if (isScanMode) {
                        imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        imageAnalysis?.setAnalyzer(cameraExecutor, BarcodeAnalyzer())
                        useCases.add(imageAnalysis!!)
                    }

                    // 绑定用例到生命周期
                    val camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, *useCases.toTypedArray()
                    )

                    Log.d("CameraActivity", "Camera bound successfully")

                } catch (exc: Exception) {
                    Log.e("CameraActivity", "Use case binding failed", exc)
                    Toast.makeText(this, "摄像头绑定失败: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

            } catch (exc: Exception) {
                Log.e("CameraActivity", "Camera provider failed", exc)
                Toast.makeText(this, "摄像头初始化失败: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchToScanMode() {
        if (isProcessing) return
        isScanMode = true
        isProcessing = false
        updateUI()
        // 重新绑定摄像头用例
        startCamera()
    }

    private fun switchToPhotoMode() {
        if (isProcessing) return
        isScanMode = false
        isProcessing = false
        updateUI()
        // 重新绑定摄像头用例
        startCamera()
    }

    private fun updateUI() {
        applyDarkModeButtons()
        if (isScanMode) {
            // 扫码模式
            binding.layoutScanFrame.visibility = android.view.View.VISIBLE
            binding.btnCapture.visibility = android.view.View.VISIBLE
            binding.tvHint.text = "将条形码放入框内"
        } else {
            // 拍照模式
            binding.layoutScanFrame.visibility = android.view.View.GONE
            binding.btnCapture.visibility = android.view.View.VISIBLE
            binding.tvHint.text = "点击按钮拍照识别食物"
        }
    }

    private fun applyDarkModeButtons() {
        val dark = ContextCompat.getColor(this, android.R.color.black)
        val white = ContextCompat.getColor(this, android.R.color.white)
        listOf(binding.btnScanMode, binding.btnPhotoMode).forEach { btn ->
            btn.backgroundTintList = ColorStateList.valueOf(dark)
            btn.setTextColor(white)
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
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.QR_CODE
                ),
                DecodeHintType.TRY_HARDER to true
            )
            setHints(hints)
        }

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            try {
                if (!isScanMode || isProcessing) return

                val mediaImage = imageProxy.image ?: return
                val width = imageProxy.width
                val height = imageProxy.height

                // Y 平面并不一定是紧凑的 width*height，需要考虑 rowStride/pixelStride
                val yPlane = mediaImage.planes[0]
                val yBuffer = yPlane.buffer.duplicate()
                val rowStride = yPlane.rowStride
                val pixelStride = yPlane.pixelStride

                val yData = ByteArray(width * height)
                if (pixelStride == 1 && rowStride == width) {
                    yBuffer.rewind()
                    yBuffer.get(yData, 0, yData.size)
                } else {
                    var outputIndex = 0
                    for (row in 0 until height) {
                        var inputIndex = row * rowStride
                        for (col in 0 until width) {
                            yData[outputIndex++] = yBuffer.get(inputIndex)
                            inputIndex += pixelStride
                        }
                    }
                }

                var source: LuminanceSource = PlanarYUVLuminanceSource(
                    yData,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false
                )

                // rotationDegrees: 顺时针旋转多少度才是正向；PlanarYUVLuminanceSource 支持逆时针旋转
                val rotation = imageProxy.imageInfo.rotationDegrees
                if (rotation != 0 && source.isRotateSupported) {
                    source = when (rotation) {
                        90 -> source.rotateCounterClockwise()
                            .rotateCounterClockwise()
                            .rotateCounterClockwise()
                        180 -> source.rotateCounterClockwise().rotateCounterClockwise()
                        270 -> source.rotateCounterClockwise()
                        else -> source
                    }
                }

                val bitmap = BinaryBitmap(HybridBinarizer(source))
                val result = reader.decodeWithState(bitmap)

                runOnUiThread { handleScanResult(result.text) }
            } catch (e: NotFoundException) {
                // 未找到条形码/二维码，继续扫描
            } catch (e: Exception) {
                Log.w("CameraActivity", "Decode failed", e)
            } finally {
                reader.reset()
                imageProxy.close()
            }
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

    override fun onResume() {
        super.onResume()
        Log.d("CameraActivity", "onResume called")

        // 检查权限状态并相应地启动摄像头或显示权限请求UI
        val permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        Log.d("CameraActivity", "onResume - permission status: $permissionStatus")

        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            if (cameraProvider == null) {
                Log.d("CameraActivity", "Permission granted but camera provider is null, restarting camera")
                startCamera()
            }
        } else {
            Log.d("CameraActivity", "Permission not granted in onResume")
            showPermissionDeniedUI()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("CameraActivity", "onPause called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CameraActivity", "onDestroy called")
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}
