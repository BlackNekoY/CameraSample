package com.slim.me.camerasample.record

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.RelativeLayout
import com.slim.me.camerasample.R
import com.slim.me.camerasample.record.layer.LayerManager
import com.slim.me.camerasample.record.layer.filter.FilterLayer
import com.slim.me.camerasample.record.layer.record.RecordLayer
import com.slim.me.camerasample.util.UIUtil
import kotlin.math.abs

class PreviewLayout : RelativeLayout {

    private lateinit var mLayerManager: LayerManager
    private lateinit var mFilterLayer: FilterLayer
    private lateinit var mRecordLayer: RecordLayer

    private var mPressX = 0f
    private var mPressY = 0f

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        val view = LayoutInflater.from(context).inflate(R.layout.preview_layout, this)
        mLayerManager = LayerManager()
        mFilterLayer = FilterLayer(mLayerManager, view)
        mRecordLayer = RecordLayer(mLayerManager, view)
        mLayerManager.addLayer(mFilterLayer)
        mLayerManager.addLayer(mRecordLayer)
    }

    fun onDestroy() {
        mLayerManager.onDestroy()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPressX = x
                mPressY = y
            }
            MotionEvent.ACTION_UP -> {
                val dx = x - mPressX
                val dy = y - mPressY
                val slop = ViewConfiguration.get(context).scaledTouchSlop
                val size = UIUtil.dip2px(context, 80f)
                if (abs(dx) < slop && abs(dy) < slop) {
                    // 对焦
                    mRecordLayer.focus(x, y, size, width, height)
                }
            }
        }
        return true
    }
}