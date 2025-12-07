package com.example.forhealth.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.forhealth.R

class MacroDonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var proteinCal: Double = 0.0
    private var carbsCal: Double = 0.0
    private var fatCal: Double = 0.0
    private var totalIntake: Double = 0.0
    
    private val proteinPaint = Paint().apply {
        color = resources.getColor(R.color.blue_500, null)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val carbsPaint = Paint().apply {
        color = resources.getColor(R.color.amber_400, null)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val fatPaint = Paint().apply {
        color = resources.getColor(R.color.rose_400, null)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val centerTextPaint = Paint().apply {
        color = resources.getColor(R.color.slate_400, null)
        textSize = resources.displayMetrics.scaledDensity * 10f // 10sp
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    private val centerValuePaint = Paint().apply {
        color = resources.getColor(R.color.slate_700, null)
        textSize = resources.displayMetrics.scaledDensity * 24f // 24sp，增大字体
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }
    
    fun setMacros(protein: Double, carbs: Double, fat: Double, total: Double = 0.0) {
        proteinCal = protein
        carbsCal = carbs
        fatCal = fat
        totalIntake = total
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2
        val centerY = height / 2
        val radius = minOf(width, height) / 2 - 10f // 增大圆环半径
        val innerRadius = radius - 35f // 减小内圆半径，使圆环变粗
        
        val total = proteinCal + carbsCal + fatCal
        
        // 如果没有数据，绘制灰色圆环
        if (total <= 0) {
            val grayPaint = Paint().apply {
                color = resources.getColor(R.color.slate_300, null)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(centerX, centerY, radius, grayPaint)
            
            // 绘制中心圆（白色背景）
            val centerPaint = Paint().apply {
                color = resources.getColor(R.color.white, null)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(centerX, centerY, innerRadius, centerPaint)
            
            // 绘制中心文字 - 显示0
            val textY = centerY + (centerValuePaint.textSize / 3)
            canvas.drawText("0", centerX, textY, centerValuePaint)
            return
        }
        
        val proteinPct = (proteinCal / total * 360f).toFloat()
        val carbsPct = (carbsCal / total * 360f).toFloat()
        val fatPct = (fatCal / total * 360f).toFloat()
        
        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        
        var startAngle = -90f
        
        // 绘制蛋白质
        if (proteinPct > 0) {
            canvas.drawArc(rect, startAngle, proteinPct, true, proteinPaint)
            startAngle += proteinPct
        }
        
        // 绘制碳水
        if (carbsPct > 0) {
            canvas.drawArc(rect, startAngle, carbsPct, true, carbsPaint)
            startAngle += carbsPct
        }
        
        // 绘制脂肪
        if (fatPct > 0) {
            canvas.drawArc(rect, startAngle, fatPct, true, fatPaint)
        }
        
        // 绘制中心圆（白色背景）
        val centerPaint = Paint().apply {
            color = resources.getColor(R.color.white, null)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, centerY, innerRadius, centerPaint)
        
        // 绘制中心文字 - 只显示总摄入量数字，居中显示
        val displayValue = if (totalIntake > 0) totalIntake else total
        val textY = centerY + (centerValuePaint.textSize / 3) // 调整垂直居中
        canvas.drawText(Math.round(displayValue).toString(), centerX, textY, centerValuePaint)
    }
}

