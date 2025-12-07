package com.example.forhealth.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.forhealth.R

class ProgressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.slate_100)
        style = Paint.Style.FILL
    }
    
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private var current: Double = 0.0
    private var target: Double = 100.0
    private var progressColor: Int = ContextCompat.getColor(context, R.color.emerald_500)
    
    private val rect = RectF()
    
    fun setProgress(current: Double, target: Double, colorRes: Int) {
        this.current = current
        this.target = target
        this.progressColor = ContextCompat.getColor(context, colorRes)
        progressPaint.color = progressColor
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val height = height.toFloat()
        val width = width.toFloat()
        
        // Draw background
        rect.set(0f, 0f, width, height)
        canvas.drawRoundRect(rect, height / 2, height / 2, backgroundPaint)
        
        // Draw progress
        val percentage = (current / target).coerceIn(0.0, 1.0)
        val progressWidth = (width * percentage).toFloat()
        
        if (progressWidth > 0) {
            rect.set(0f, 0f, progressWidth, height)
            canvas.drawRoundRect(rect, height / 2, height / 2, progressPaint)
        }
    }
}

