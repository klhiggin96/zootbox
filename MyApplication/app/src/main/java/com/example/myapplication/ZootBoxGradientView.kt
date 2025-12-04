package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat

class ZootBoxGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 220f
        typeface = ResourcesCompat.getFont(context, R.font.archivo_black)
        style = Paint.Style.FILL
        color = Color.WHITE
        letterSpacing = -0.02f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val padding = 40f // Padding from edges
        val availableWidth = width - (padding * 2)

        // Row 1: ZOOT (4 letters with space-between distribution)
        val row1Y = 180f
        val zootLetters = listOf("Z", "O", "O", "T")
        val zootWidths = zootLetters.map { letter ->
            val bounds = Rect()
            textPaint.getTextBounds(letter, 0, letter.length, bounds)
            bounds.width().toFloat()
        }
        val zootTotalLetterWidth = zootWidths.sum()
        val zootSpacing = (availableWidth - zootTotalLetterWidth) / (zootLetters.size - 1)

        var currentX = padding
        zootLetters.forEachIndexed { index, letter ->
            canvas.drawText(letter, currentX, row1Y, textPaint)
            currentX += zootWidths[index] + zootSpacing
        }

        // Row 2: BOX (3 letters with space-between distribution)
        val row2Y = 380f
        val boxLetters = listOf("B", "O", "X")
        val boxWidths = boxLetters.map { letter ->
            val bounds = Rect()
            textPaint.getTextBounds(letter, 0, letter.length, bounds)
            bounds.width().toFloat()
        }
        val boxTotalLetterWidth = boxWidths.sum()
        val boxSpacing = (availableWidth - boxTotalLetterWidth) / (boxLetters.size - 1)

        currentX = padding
        boxLetters.forEachIndexed { index, letter ->
            canvas.drawText(letter, currentX, row2Y, textPaint)
            currentX += boxWidths[index] + boxSpacing
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = 420
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            desiredHeight
        )
    }
}
