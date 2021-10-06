package com.example.yandexmaps.ui.custom

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.yandexmaps.R


class DividerView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val mPaint: Paint
    private var orientation = 0

    constructor(context: Context) : this(context, null) {}

    override fun onDraw(canvas: Canvas) {
        if (orientation == ORIENTATION_HORIZONTAL) {
            val center = height * .5f
            canvas.drawLine(0f, center, width.toFloat(), center, mPaint)
        } else {
            val center = width * .5f
            canvas.drawLine(center, 0f, center, height.toFloat(), mPaint)
        }
    }

    companion object {
        var ORIENTATION_HORIZONTAL = 0
        var ORIENTATION_VERTICAL = 1
    }

    init {
        val dashGap: Int
        val dashLength: Int
        val dashThickness: Int
        val color: Int
        val a: TypedArray =
            context.getTheme().obtainStyledAttributes(attrs, R.styleable.DividerView, 0, 0)
        try {
            dashGap = a.getDimensionPixelSize(R.styleable.DividerView_dashGap, 5)
            dashLength = a.getDimensionPixelSize(R.styleable.DividerView_dashLength, 5)
            dashThickness = a.getDimensionPixelSize(R.styleable.DividerView_dashThickness, 3)
            color = a.getColor(R.styleable.DividerView_color, -0x1000000)
            orientation = a.getInt(R.styleable.DividerView_orientation, ORIENTATION_HORIZONTAL)
        } finally {
            a.recycle()
        }
        mPaint = Paint()
        mPaint.setAntiAlias(true)
        mPaint.setColor(color)
        mPaint.setStyle(Paint.Style.STROKE)
        mPaint.setStrokeWidth(dashThickness.toFloat())
        mPaint.setPathEffect(
            DashPathEffect(
                floatArrayOf(dashLength.toFloat(), dashGap.toFloat()),
                0f
            )
        )
    }
}