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

    private val archivoFont = ResourcesCompat.getFont(context, R.font.archivo_black)
    private val textSize = 220f

    // Deep shadow for 3D depth
    private val deepShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = 220f
        typeface = archivoFont
        style = Paint.Style.FILL
        letterSpacing = -0.02f
    }

    // Mid shadow layer
    private val midShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = 220f
        typeface = archivoFont
        style = Paint.Style.FILL
        letterSpacing = -0.02f
    }

    // Outer glow/rim
    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = 220f
        typeface = archivoFont
        style = Paint.Style.STROKE
        strokeWidth = 6f
        letterSpacing = -0.02f
    }

    // Main glass fill
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = 220f
        typeface = archivoFont
        style = Paint.Style.FILL
        letterSpacing = -0.02f
    }

    // Inner highlight stroke
    private val innerHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = 220f
        typeface = archivoFont
        style = Paint.Style.STROKE
        strokeWidth = 3f
        letterSpacing = -0.02f
    }

    // Top shine overlay
    private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = 220f
        typeface = archivoFont
        style = Paint.Style.FILL
        letterSpacing = -0.02f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val padding = 40f
        val availableWidth = width - (padding * 2)

        // Deep shadow gradient (fades out)
        deepShadowPaint.color = Color.parseColor("#0D0D1A")

        // Mid shadow with slight blue tint
        midShadowPaint.color = Color.parseColor("#1A1A2E")

        // Outer glow - subtle cyan/white rim light
        outerGlowPaint.shader = LinearGradient(
            0f, 0f, 0f, 400f,
            intArrayOf(
                Color.parseColor("#80FFFFFF"),
                Color.parseColor("#4090B0C0")
            ),
            null,
            Shader.TileMode.CLAMP
        )

        // Glass gradient - shiny with depth
        glassPaint.shader = LinearGradient(
            0f, 0f, 0f, 380f,
            intArrayOf(
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#F0F4F8"),
                Color.parseColor("#D8E0E8"),
                Color.parseColor("#B8C8D8"),
                Color.parseColor("#A0B0C0")
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )

        // Inner highlight - top edge shine
        innerHighlightPaint.shader = LinearGradient(
            0f, 0f, 0f, 150f,
            intArrayOf(
                Color.parseColor("#FFFFFFFF"),
                Color.parseColor("#00FFFFFF")
            ),
            null,
            Shader.TileMode.CLAMP
        )

        // Top shine - glossy highlight
        shinePaint.shader = LinearGradient(
            0f, 0f, 0f, 120f,
            intArrayOf(
                Color.parseColor("#60FFFFFF"),
                Color.parseColor("#00FFFFFF")
            ),
            null,
            Shader.TileMode.CLAMP
        )

        // Row 1: ZOOT
        val row1Y = 180f
        drawRow(canvas, "ZOOT", row1Y, padding, availableWidth)

        // Row 2: BOX
        val row2Y = 350f
        drawRow(canvas, "BOX", row2Y, padding, availableWidth)
    }

    private fun drawRow(canvas: Canvas, text: String, rowY: Float, padding: Float, availableWidth: Float) {
        val letters = text.toList().map { it.toString() }
        val letterWidths = letters.map { letter ->
            val bounds = Rect()
            glassPaint.getTextBounds(letter, 0, letter.length, bounds)
            bounds.width().toFloat()
        }
        val totalLetterWidth = letterWidths.sum()
        val spacing = (availableWidth - totalLetterWidth) / (letters.size - 1)

        var currentX = padding
        letters.forEachIndexed { index, letter ->
            // Layer 1: Deep shadow (furthest back)
            for (i in 16 downTo 10) {
                deepShadowPaint.alpha = (255 * (1 - i / 20f)).toInt().coerceIn(20, 80)
                canvas.drawText(letter, currentX + i * 1.2f, rowY + i * 1.2f, deepShadowPaint)
            }

            // Layer 2: Mid shadow
            for (i in 9 downTo 4) {
                midShadowPaint.alpha = (255 * (1 - i / 12f)).toInt().coerceIn(60, 150)
                canvas.drawText(letter, currentX + i * 1.2f, rowY + i * 1.2f, midShadowPaint)
            }

            // Layer 3: Close shadow for depth
            for (i in 3 downTo 1) {
                midShadowPaint.alpha = (180 - i * 30).coerceIn(80, 180)
                canvas.drawText(letter, currentX + i * 1f, rowY + i * 1f, midShadowPaint)
            }

            // Layer 4: Outer glow/rim
            canvas.drawText(letter, currentX, rowY, outerGlowPaint)

            // Layer 5: Main glass fill
            canvas.drawText(letter, currentX, rowY, glassPaint)

            // Layer 6: Inner highlight stroke (top edge)
            canvas.drawText(letter, currentX, rowY - 2f, innerHighlightPaint)

            // Layer 7: Top shine overlay
            canvas.drawText(letter, currentX, rowY - 4f, shinePaint)

            currentX += letterWidths[index] + spacing
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = 360
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            desiredHeight
        )
    }
}
