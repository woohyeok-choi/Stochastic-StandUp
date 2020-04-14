package kaist.iclab.standup.smi.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.core.content.ContextCompat
import kaist.iclab.standup.smi.R
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin


class AnimatedCircle(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {
    private val mStrokeWidth: Float
    private val mFillWidth: Float
    private val mBackgroundColor: Int
    private val mIsAnimated: Boolean
    private val mShowPoint: Boolean
    private val mPointRadius: Float

    private val strokePaint: Paint
    private val fillPaint: Paint
    private val pointPaint: Paint


    private val mStartAngle: Float
    private var mCurrentAngle: Float

    var angle: Float
        get() = mCurrentAngle
        set(value) {
            if (mIsAnimated) {
                val animation = object : Animation() {
                    override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                        mCurrentAngle = value * interpolatedTime
                        requestLayout()
                    }
                }.apply {
                    duration = 750
                    interpolator = AccelerateDecelerateInterpolator()
                }
                startAnimation(animation)
            } else {
                mCurrentAngle = value
                invalidate()
            }
        }

    init {
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.AnimatedCircle, 0, 0).apply {
            try {
                mBackgroundColor = getColor(
                    R.styleable.AnimatedCircle_backgroundColor,
                    ContextCompat.getColor(context, R.color.gmm_white)
                )
                mStrokeWidth = getDimension(R.styleable.AnimatedCircle_strokeWidth, 5.0F)
                mFillWidth = getDimension(R.styleable.AnimatedCircle_fillWidth, 5.0F)
                mIsAnimated = getBoolean(R.styleable.AnimatedCircle_isAnimated, false)
                mPointRadius = getDimension(R.styleable.AnimatedCircle_pointRadius, mFillWidth / 2)
                mShowPoint = getBoolean(R.styleable.AnimatedCircle_showPoint, false)
                mStartAngle = getFloat(R.styleable.AnimatedCircle_startAngle, 270.0F)
                mCurrentAngle = getFloat(R.styleable.AnimatedCircle_angle, 0.0F)

                val mStrokeDotSize = getDimension(R.styleable.AnimatedCircle_strokeDotSize, 3.0F)
                val mStrokeGapSize = getDimension(R.styleable.AnimatedCircle_strokeGapSize, 3.0F)
                val mStrokeStyle = getInt(R.styleable.AnimatedCircle_strokeStyle, 0)

                val mStrokeColor = getColor(
                    R.styleable.AnimatedCircle_strokeColor,
                    ContextCompat.getColor(context, R.color.extreme_light_blue)
                )
                val mFillColor = getColor(
                    R.styleable.AnimatedCircle_fillColor,
                    ContextCompat.getColor(context, R.color.blue)
                )
                val mPointColor = getColor(R.styleable.AnimatedCircle_pointColor, mFillColor)

                strokePaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    color = mStrokeColor
                    strokeWidth = mStrokeWidth
                    if (mStrokeStyle == 1) {
                        pathEffect =
                            DashPathEffect(floatArrayOf(mStrokeDotSize, mStrokeGapSize), 0.0F)
                    }
                }

                fillPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    color = mFillColor
                    strokeWidth = mFillWidth
                }

                pointPaint = Paint().apply {
                    isAntiAlias = true
                    color = mPointColor
                }
            } finally {
                recycle()
            }
        }
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        val maxWidth = max(mStrokeWidth, mFillWidth)

        canvas.drawColor(mBackgroundColor)

        canvas.drawArc(
            maxWidth,
            maxWidth,
            width - maxWidth,
            height - maxWidth,
            0.0F,
            360.0F,
            false,
            strokePaint
        )

        canvas.drawArc(
            maxWidth,
            maxWidth,
            width - maxWidth,
            width - maxWidth,
            mStartAngle,
            mCurrentAngle,
            false,
            fillPaint
        )

        if (mShowPoint) {
            val destAngle = (mStartAngle + mCurrentAngle).toDouble()
            val cX = width.toFloat() / 2
            val cY = height.toFloat() / 2
            val radius = width / 2 - maxWidth

            val pX = cos(Math.toRadians(destAngle)).toFloat() * radius
            val pY = sin(Math.toRadians(destAngle)).toFloat() * radius

            canvas.drawCircle(cX + pX, cY + pY, mPointRadius, pointPaint)
            canvas.drawCircle(cX, cY - radius, mPointRadius, pointPaint)
        }
    }
}