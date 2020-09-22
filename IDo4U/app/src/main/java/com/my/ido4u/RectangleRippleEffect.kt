package com.my.ido4u

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import com.takusemba.spotlight.effet.Effect
import java.util.concurrent.TimeUnit

/**
 * Draws an ripple effects in a round rectangle shape - an implementation of the Effect interface
 * of the Spotlight library.
 */
class RectangleRippleEffect @JvmOverloads constructor(
    private val height: Float,
    private val width: Float,
    private val offset: Float,
    private val radius: Float,
    @ColorInt private val color: Int,
    override val duration: Long = DEFAULT_DURATION,
    override val interpolator: TimeInterpolator = DEFAULT_INTERPOLATOR,
    override val repeatMode: Int = DEFAULT_REPEAT_MODE
) : Effect {

    init {
        require(offset < radius) { "holeRadius should be bigger than rippleRadius." }
    }

    override fun draw(canvas: Canvas, point: PointF, value: Float, paint: Paint) {

        val halfWidth = width / 2 * value
        val halfHeight = height / 2 * value
        val left = point.x - halfWidth
        val top = point.y - halfHeight
        val right = point.x + halfWidth
        val bottom = point.y + halfHeight
        val rect = RectF(left - 20, top - 20, right + 20, bottom + 20)

        val radius = offset + ((radius - offset) * value) - 10
        val alpha = (255).toInt()
        paint.color = color
        paint.alpha = alpha

        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    companion object {

        val DEFAULT_DURATION = TimeUnit.MILLISECONDS.toMillis(1000)

        val DEFAULT_INTERPOLATOR = DecelerateInterpolator(1f)

        const val DEFAULT_REPEAT_MODE = ObjectAnimator.REVERSE
    }
}