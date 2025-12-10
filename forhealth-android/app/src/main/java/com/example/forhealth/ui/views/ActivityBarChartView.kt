package com.example.forhealth.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.forhealth.R

class ActivityBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var intakeValue: Double = 0.0
    private var burnedValue: Double = 0.0
    
    private val intakePaint = Paint().apply {
        color = resources.getColor(R.color.emerald_600, null) // 使用饮食记录的绿色
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val burnedPaint = Paint().apply {
        color = resources.getColor(R.color.orange_500, null)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val gridPaint = Paint().apply {
        color = resources.getColor(R.color.slate_200, null)
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    
    private val labelPaint = Paint().apply {
        color = resources.getColor(R.color.slate_400, null)
        textSize = resources.displayMetrics.scaledDensity * 10f // 10sp
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    private val valueLabelPaint = Paint().apply {
        color = resources.getColor(R.color.emerald_600, null) // Intake使用绿色
        textSize = resources.displayMetrics.scaledDensity * 12f // 12sp
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    private val burnedValueLabelPaint = Paint().apply {
        color = resources.getColor(R.color.orange_600, null) // Burned使用橙色
        textSize = resources.displayMetrics.scaledDensity * 12f // 12sp
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    fun setData(chartData: List<ChartDataPoint>) {
        // 日视图：使用所有数据点的总和
        intakeValue = chartData.sumOf { it.intake }
        burnedValue = chartData.sumOf { it.burned }
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val leftPadding = 20f
        val rightPadding = 20f
        val topPadding = 20f
        val bottomPadding = 50f // 底部留出空间给标签
        val chartHeight = height - topPadding - bottomPadding
        
        val maxValue = maxOf(intakeValue, burnedValue, 100.0) * 1.2
        
        // 绘制底部基线
        val bottomY = topPadding + chartHeight
        canvas.drawLine(leftPadding, bottomY, width - rightPadding, bottomY, gridPaint)
        
        // 绘制柱状图 - 固定宽度，居中显示
        val fixedBarWidth = 80f // 固定柱宽
        val centerX = width / 2
        val gap = 20f // 两个柱子之间的间距
        
        // Intake柱（左侧）
        val intakeHeight = (intakeValue / maxValue * chartHeight).toFloat()
        val intakeLeft = centerX - fixedBarWidth - gap / 2
        val intakeRight = centerX - gap / 2
        val intakeTop = bottomY - intakeHeight
        val intakeBottom = bottomY
        
        canvas.drawRect(intakeLeft, intakeTop, intakeRight, intakeBottom, intakePaint)
        
        // 绘制Intake数值（不绘制标签）
        canvas.drawText(
            Math.round(intakeValue).toString(),
            (intakeLeft + intakeRight) / 2,
            intakeTop - 5f,
            valueLabelPaint
        )
        
        // Burned柱（右侧）
        val burnedHeight = (burnedValue / maxValue * chartHeight).toFloat()
        val burnedLeft = centerX + gap / 2
        val burnedRight = centerX + fixedBarWidth + gap / 2
        val burnedTop = bottomY - burnedHeight
        val burnedBottom = bottomY
        
        canvas.drawRect(burnedLeft, burnedTop, burnedRight, burnedBottom, burnedPaint)
        
        // 绘制Burned数值（不绘制标签）
        canvas.drawText(
            Math.round(burnedValue).toString(),
            (burnedLeft + burnedRight) / 2,
            burnedTop - 5f,
            burnedValueLabelPaint
        )
    }
}

