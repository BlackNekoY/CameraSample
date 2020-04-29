package com.slim.me.camerasample.record.layer.filter

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

class FilterViewPager : ViewPager {

    private var mPressX = 0f
    private var mPressY = 0f
    private var mFlag = false

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        super.onInterceptTouchEvent(event)
        val x = event.x
        val y = event.y
        val dx = x - mPressX
        val dy = y - mPressY
        val slop = ViewConfiguration.get(context).scaledTouchSlop

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPressX = x
                mPressY = y
                mFlag = false
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (mFlag) {
                    return true
                }
                if (abs(dx) > slop || abs(dy) > slop) {
                    mFlag = true
                    return true
                }
                return false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mFlag = false
                return false
            }
        }
        return false
    }
}