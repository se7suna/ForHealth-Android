package com.example.forhealth.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.forhealth.R

class RingProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.slate_300)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    private val rect = RectF()
    
    private var current: Double = 0.0
    private var target: Double = 100.0
    
    private val size: Float = resources.getDimension(R.dimen.progress_ring_size)
    private val backgroundStrokeWidth: Float = resources.getDimension(R.dimen.progress_ring_stroke_background)
    private val progressStrokeWidth: Float = resources.getDimension(R.dimen.progress_ring_stroke)
    
    // Center radius: the radius of the center line of the ring
    // This ensures both rings share the same center line
    // Increase the center radius to make the inner radius larger (thinner ring)
    // Use a larger offset to push the ring outward
    private val centerRadius: Float = size / 2f - maxOf(backgroundStrokeWidth, progressStrokeWidth) / 2f + resources.getDimension(R.dimen.spacing_24)
    
    init {
        backgroundPaint.strokeWidth = backgroundStrokeWidth
        progressPaint.strokeWidth = progressStrokeWidth
    }
    
    fun setProgress(current: Double, target: Double) {
        this.current = current
        this.target = target
        invalidate()
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Calculate the actual outer radius of the ring
        // Outer radius = centerRadius + max(strokeWidth) / 2
        val maxStrokeWidth = maxOf(backgroundStrokeWidth, progressStrokeWidth)
        val outerRadius = centerRadius + maxStrokeWidth / 2f
        val measuredSize = (outerRadius * 2).toInt()
        setMeasuredDimension(
            resolveSize(measuredSize, widthMeasureSpec),
            resolveSize(measuredSize, heightMeasureSpec)
        )
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Calculate progress
        val isNegative = current < 0
        val absValue = Math.abs(current)
        val percentage = (absValue / target).coerceIn(0.0, 1.0)
        
        // Set progress color
        val progressColor = if (isNegative) {
            ContextCompat.getColor(context, R.color.orange_500)
        } else {
            ContextCompat.getColor(context, R.color.emerald_500)
        }
        progressPaint.color = progressColor
        
        // Set rect for drawing arcs
        // The rect is centered and uses centerRadius
        rect.set(
            centerX - centerRadius,
            centerY - centerRadius,
            centerX + centerRadius,
            centerY + centerRadius
        )
        
        // Draw background circle (full 360 degrees)
        canvas.drawArc(rect, -90f, 360f, false, backgroundPaint)
        
        // Draw progress arc
        val sweepAngle = (percentage * 360f).toFloat()
        
        // Only draw if sweepAngle > 0 to avoid rendering issues
        if (sweepAngle > 0f) {
            if (isNegative) {
                // For negative values (orange), draw counter-clockwise from -90 degrees
                // Use negative sweepAngle to draw counter-clockwise (leftward from top)
                canvas.drawArc(rect, -90f, -sweepAngle, false, progressPaint)
            } else {
                // For positive values (green), draw clockwise from -90 degrees
                canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
            }
        }
    }
}

