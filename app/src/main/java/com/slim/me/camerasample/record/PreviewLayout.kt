package com.slim.me.camerasample.record

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.RelativeLayout
import com.slim.me.camerasample.R
import com.slim.me.camerasample.camera.CameraHelper
import com.slim.me.camerasample.record.widget.RecorderButton
import com.slim.me.camerasample.util.UIUtil
import kotlin.math.abs

class PreviewLayout : RelativeLayout, RecorderButton.OnRecorderButtonListener {
    private var mRecordView: CameraRecordView? = null
    private var mRecorderButton: RecorderButton? = null
    private var mFocusArea: ImageView? = null

    private var mPressX = 0f
    private var mPressY = 0f

    private val mFocusRunnable = Runnable {
        mFocusArea?.visibility = GONE
    }

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView()
    }

    private fun initView() {
        val view = LayoutInflater.from(context).inflate(R.layout.preview_layout, this)
        mRecordView = view.findViewById(R.id.record_view)
        mRecorderButton = view.findViewById(R.id.record_btn)
        mFocusArea = view.findViewById(R.id.focus_area)
        mRecorderButton?.setListener(this)
        mRecorderButton?.setCanPause(false)
    }

    fun onDestroy() {
        mRecordView?.onDestroy()
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
                    CameraHelper.getInstance().focus(x, y, size, width, height)
                    mFocusArea?.run {
                        val params = layoutParams as LayoutParams
                        params.leftMargin = (x - size / 2).toInt()
                        params.topMargin = (y - size / 2).toInt()
                        layoutParams = params
                        visibility = VISIBLE
                        removeCallbacks(mFocusRunnable)
                        postDelayed(mFocusRunnable, 2000)
                    }
                }
            }
        }
        return true
    }

    override fun onStartRecorder(): Boolean {
        mRecordView?.startRecord()
        return true
    }

    override fun onStopRecorder(isLongClick: Boolean) {
        mRecordView?.stopRecord()
    }

    override fun onHoldRecorder(): Boolean {
        mRecordView?.startRecord()
        return true
    }

    override fun onCountDownStart() {
    }

    override fun onFinish(isLongClick: Boolean) {
        mRecordView?.stopRecord()
    }
}