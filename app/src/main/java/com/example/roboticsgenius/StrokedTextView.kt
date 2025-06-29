package com.example.roboticsgenius

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class StrokedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var strokeColorValue: Int? = null
    private var strokeWidthValue: Float = 0f

    fun setStroke(width: Float, color: Int) {
        strokeWidthValue = width
        strokeColorValue = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val color = strokeColorValue
        if (strokeWidthValue > 0 && color != null) {
            val originalColor = this.currentTextColor
            this.paint.style = Paint.Style.STROKE
            this.paint.strokeWidth = strokeWidthValue
            this.setTextColor(color)
            super.onDraw(canvas)

            this.paint.style = Paint.Style.FILL
            this.setTextColor(originalColor)
            super.onDraw(canvas)
        } else {
            super.onDraw(canvas)
        }
    }
}