package com.example.forhealth.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.forhealth.R

data class ChartDataPoint(
    val label: String,
    val intake: Double,
    val burned: Double
)

class ActivityTrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var data: List<ChartDataPoint> = emptyList()
    private var selectedIndex: Int? = null // 选中的节点索引
    private var selectedType: String? = null // 选中的类型："intake" 或 "burned"
    private val leftPadding = 20f
    private val rightPadding = 20f
    private val topPadding = 20f
    private val bottomPadding = 40f // 底部留出空间给标签
    
    private val gridPaint = Paint().apply {
        color = resources.getColor(R.color.slate_200, null)
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    
    private val dashedGridPaint = Paint().apply {
        color = resources.getColor(R.color.slate_100, null)
        strokeWidth = 1f
        style = Paint.Style.STROKE
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(4f, 4f), 0f)
    }
    
    private val intakeLinePaint = Paint().apply {
        color = resources.getColor(R.color.emerald_600, null) // 使用饮食记录的绿色
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val burnedLinePaint = Paint().apply {
        color = resources.getColor(R.color.orange_500, null)
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val valueTextPaint = Paint().apply {
        color = resources.getColor(R.color.emerald_600, null) // Intake数值使用绿色
        textSize = resources.displayMetrics.scaledDensity * 12f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    private val burnedValueTextPaint = Paint().apply {
        color = resources.getColor(R.color.orange_600, null) // Burned数值使用橙色
        textSize = resources.displayMetrics.scaledDensity * 12f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    private val tooltipBackgroundPaint = Paint().apply {
        color = resources.getColor(R.color.white, null)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val tooltipBorderPaint = Paint().apply {
        color = resources.getColor(R.color.slate_200, null)
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private val tooltipTextPaint = Paint().apply {
        color = resources.getColor(R.color.slate_800, null)
        textSize = resources.displayMetrics.scaledDensity * 12f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }
    
    private val intakePointPaint = Paint().apply {
        color = resources.getColor(R.color.white, null)
        style = Paint.Style.FILL
    }
    
    private val intakePointStrokePaint = Paint().apply {
        color = resources.getColor(R.color.emerald_600, null) // 使用饮食记录的绿色
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    
    private val burnedPointPaint = Paint().apply {
        color = resources.getColor(R.color.white, null)
        style = Paint.Style.FILL
    }
    
    private val burnedPointStrokePaint = Paint().apply {
        color = resources.getColor(R.color.orange_500, null)
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    
    private val labelPaint = Paint().apply {
        color = resources.getColor(R.color.slate_400, null)
        textSize = resources.displayMetrics.scaledDensity * 10f // 10sp
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    private val axisLabelPaint = Paint().apply {
        color = resources.getColor(R.color.slate_400, null)
        textSize = resources.displayMetrics.scaledDensity * 10f // 10sp
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
    }
    
    fun setData(chartData: List<ChartDataPoint>) {
        data = chartData
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (data.isEmpty()) return
        
        val width = width.toFloat()
        val height = height.toFloat()
        val chartWidth = width - leftPadding - rightPadding
        val chartHeight = height - topPadding - bottomPadding
        
        // 计算最大值
        val maxValue = data.maxOfOrNull { maxOf(it.intake, it.burned) }?.let { 
            if (it > 0) it * 1.1 else 100.0 
        } ?: 100.0
        
        // 绘制底部基线
        val bottomY = topPadding + chartHeight
        canvas.drawLine(leftPadding, bottomY, width - rightPadding, bottomY, gridPaint)
        
        if (data.size < 2) return
        
        // 绘制摄入折线
        val intakePath = Path()
        data.forEachIndexed { index, point ->
            val x = leftPadding + (index.toFloat() / (data.size - 1)) * chartWidth
            val y = bottomY - (point.intake / maxValue).toFloat() * chartHeight
            
            if (index == 0) {
                intakePath.moveTo(x, y)
            } else {
                intakePath.lineTo(x, y)
            }
        }
        canvas.drawPath(intakePath, intakeLinePaint)
        
        // 绘制消耗折线
        val burnedPath = Path()
        data.forEachIndexed { index, point ->
            val x = leftPadding + (index.toFloat() / (data.size - 1)) * chartWidth
            val y = bottomY - (point.burned / maxValue).toFloat() * chartHeight
            
            if (index == 0) {
                burnedPath.moveTo(x, y)
            } else {
                burnedPath.lineTo(x, y)
            }
        }
        canvas.drawPath(burnedPath, burnedLinePaint)
        
        // 绘制点和标签
        data.forEachIndexed { index, point ->
            val x = leftPadding + (index.toFloat() / (data.size - 1)) * chartWidth
            val intakeY = bottomY - (point.intake / maxValue).toFloat() * chartHeight
            val burnedY = bottomY - (point.burned / maxValue).toFloat() * chartHeight
            
            // 摄入点
            canvas.drawCircle(x, intakeY, 3f, intakePointPaint)
            canvas.drawCircle(x, intakeY, 3f, intakePointStrokePaint)
            
            // 消耗点
            canvas.drawCircle(x, burnedY, 3f, burnedPointPaint)
            canvas.drawCircle(x, burnedY, 3f, burnedPointStrokePaint)
            
            // X轴标签
            canvas.drawText(point.label, x, height - 10f, labelPaint)
        }
        
        // 绘制选中节点的数值提示
        selectedIndex?.let { index ->
            selectedType?.let { type ->
                if (index >= 0 && index < data.size) {
                    val point = data[index]
                    val x = leftPadding + (index.toFloat() / (data.size - 1)) * chartWidth
                    val intakeY = bottomY - (point.intake / maxValue).toFloat() * chartHeight
                    val burnedY = bottomY - (point.burned / maxValue).toFloat() * chartHeight
                    
                    // 根据选中的类型确定显示的值和位置
                    val (value, y, label) = when (type) {
                        "intake" -> Triple(point.intake, intakeY, "Intake")
                        "burned" -> Triple(point.burned, burnedY, "Burned")
                        else -> return@let
                    }
                    
                    // 计算提示框位置，确保不超出边界
                    val tooltipText = "${Math.round(value)} kcal"
                    val textWidth = tooltipTextPaint.measureText(tooltipText)
                    val tooltipPadding = 16f
                    val tooltipWidth = textWidth + tooltipPadding * 2
                    val tooltipHeight = 40f
                    val tooltipSpacing = 10f // 提示框与点的间距
                    
                    // tooltipY是提示框底部的Y坐标
                    // 优先尝试放在点的上方
                    var tooltipY = y - tooltipSpacing
                    var tooltipTop = tooltipY - tooltipHeight
                    
                    // 如果上方超出顶部边界，尝试放在点的下方
                    if (tooltipTop < topPadding) {
                        tooltipY = y + tooltipSpacing
                        tooltipTop = tooltipY - tooltipHeight
                        
                        // 如果下方也超出底部边界，则强制放在图表顶部安全位置
                        if (tooltipY > bottomY) {
                            tooltipY = topPadding + tooltipHeight
                            tooltipTop = topPadding
                        }
                    }
                    
                    // 最终确保提示框完全在图表区域内（topPadding到bottomY之间）
                    // tooltipTop必须 >= topPadding，tooltipY必须 <= bottomY
                    if (tooltipTop < topPadding) {
                        tooltipY = topPadding + tooltipHeight
                    }
                    if (tooltipY > bottomY) {
                        tooltipY = bottomY
                    }
                    
                    // 计算提示框的X位置，确保不超出左右边界
                    var tooltipX = x - tooltipWidth / 2
                    if (tooltipX < leftPadding) {
                        tooltipX = leftPadding + 5f
                    } else if (tooltipX + tooltipWidth > width - rightPadding) {
                        tooltipX = width - rightPadding - tooltipWidth - 5f
                    }
                    
                    val tooltipRect = android.graphics.RectF(
                        tooltipX,
                        tooltipY - tooltipHeight,
                        tooltipX + tooltipWidth,
                        tooltipY
                    )
                    
                    // 绘制提示框背景和边框
                    canvas.drawRoundRect(tooltipRect, 8f, 8f, tooltipBackgroundPaint)
                    canvas.drawRoundRect(tooltipRect, 8f, 8f, tooltipBorderPaint)
                    
                    // 绘制数值文本
                    canvas.drawText(
                        tooltipText,
                        tooltipX + tooltipWidth / 2,
                        tooltipY - tooltipHeight / 2 + tooltipTextPaint.textSize / 3,
                        tooltipTextPaint
                    )
                }
            }
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (data.size < 2) return false
                
                val width = width.toFloat()
                val height = height.toFloat()
                val chartWidth = width - leftPadding - rightPadding
                val chartHeight = height - topPadding - bottomPadding
                val bottomY = topPadding + chartHeight
                val maxValue = data.maxOfOrNull { maxOf(it.intake, it.burned) }?.let { 
                    if (it > 0) it * 1.1 else 100.0 
                } ?: 100.0
                
                val x = event.x
                val y = event.y
                
                // 找到最近的数据点
                var minDistance = Float.MAX_VALUE
                var closestIndex = -1
                var closestType: String? = null
                
                data.forEachIndexed { index, point ->
                    val pointX = leftPadding + (index.toFloat() / (data.size - 1)) * chartWidth
                    val intakeY = bottomY - (point.intake / maxValue).toFloat() * chartHeight
                    val burnedY = bottomY - (point.burned / maxValue).toFloat() * chartHeight
                    
                    val distanceX = Math.abs(x - pointX)
                    if (distanceX < 50f) { // 50f为点击容差
                        // 检查点击的是intake点还是burned点
                        val distanceToIntake = kotlin.math.sqrt(
                            (x - pointX) * (x - pointX) + (y - intakeY) * (y - intakeY)
                        )
                        val distanceToBurned = kotlin.math.sqrt(
                            (x - pointX) * (x - pointX) + (y - burnedY) * (y - burnedY)
                        )
                        
                        val minDist = minOf(distanceToIntake, distanceToBurned)
                        if (minDist < minDistance && minDist < 50f) {
                            minDistance = minDist
                            closestIndex = index
                            closestType = if (distanceToIntake < distanceToBurned) "intake" else "burned"
                        }
                    }
                }
                
                if (closestIndex >= 0 && closestType != null) {
                    selectedIndex = closestIndex
                    selectedType = closestType
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                selectedIndex = null
                selectedType = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

