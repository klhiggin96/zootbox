package com.example.myapplication

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView

class GradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val gradientColors = intArrayOf(
        Color.parseColor("#1f80bb"), // Cerulean
        Color.parseColor("#38a1b5"), // Turquoise
        Color.parseColor("#6751a6"), // Lavender
        Color.parseColor("#b9448c"), // Fuchsia
        Color.parseColor("#e93577"), // Coral pink
        Color.parseColor("#ea5c77"), // Salmon
        Color.parseColor("#c87783"), // Dusty rose
        Color.parseColor("#1f80bb")  // Back to start for seamless loop
    )

    private var gradientOffset = 0f
    private var gradientAnimator: ValueAnimator? = null
    private var gradientShader: LinearGradient? = null

    init {
        startGradientAnimation()
    }

    private fun startGradientAnimation() {
        gradientAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 8000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()

            addUpdateListener { animation ->
                gradientOffset = animation.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient(w)
    }

    private fun updateGradient(width: Int) {
        if (width > 0) {
            gradientShader = LinearGradient(
                0f, 0f, width.toFloat() * 2, 0f,
                gradientColors,
                null,
                Shader.TileMode.REPEAT
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (gradientShader != null && width > 0) {
            // Create a matrix to animate the gradient
            val matrix = Matrix()
            matrix.setTranslate(-width * gradientOffset, 0f)
            gradientShader?.setLocalMatrix(matrix)

            // Set the gradient as the text color
            paint.shader = gradientShader
        }

        super.onDraw(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gradientAnimator?.cancel()
    }
}
