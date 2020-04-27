package com.slim.me.camerasample.record.layer.gesture

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.slim.me.camerasample.record.layer.BaseLayer
import com.slim.me.camerasample.record.layer.LayerManager
import com.slim.me.camerasample.record.layer.event.CommonLayerEvent
import com.slim.me.camerasample.record.layer.event.ILayerEvent
import kotlin.math.abs

class GestureLayer(layerManager: LayerManager, rootView: View) : BaseLayer(layerManager), View.OnTouchListener {

    private var mPressTime = 0L
    private var mPressX = 0f
    private var mPressY = 0f
    private var mPressOffset = 0f
    private var mDivisionOffset = 0f

    init {
        rootView.setOnTouchListener(this)
    }

    override fun handleLayerEvent(event: ILayerEvent) {}

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val dx = x - mPressX
        val dy = y - mPressY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPressX = x
                mPressY = y
                mPressTime = SystemClock.uptimeMillis()
                mPressOffset = mDivisionOffset
            }
            MotionEvent.ACTION_MOVE -> {
                mDivisionOffset = mPressOffset + dx / v.width.toFloat()
                Log.d("slim", "move:" + mDivisionOffset)
            }
            MotionEvent.ACTION_UP -> {
                val currTime = SystemClock.uptimeMillis()
                val slop = ViewConfiguration.get(v.context).scaledTouchSlop
                if (abs(dx) < slop && abs(dy) < slop && abs(currTime - mPressTime) < 100) {
                    // 触发点击对焦
                    val bundle = Bundle()
                    bundle.putFloat("x", x)
                    bundle.putFloat("y", y)
                    postLayerEvent(CommonLayerEvent(ILayerEvent.EVENT_FOCUS_PRESS, bundle))
                }

                mPressTime = 0L
            }
        }
        return true
    }
}