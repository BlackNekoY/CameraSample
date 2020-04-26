package com.slim.me.camerasample.record.layer.record

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.RelativeLayout
import com.slim.me.camerasample.R
import com.slim.me.camerasample.camera.CameraHelper
import com.slim.me.camerasample.record.layer.BaseLayer
import com.slim.me.camerasample.record.layer.LayerManager
import com.slim.me.camerasample.record.render.filter.GPUImageFilter

class RecordLayer(layerManager: LayerManager, rootView: View) : BaseLayer(layerManager),
        RecorderButton.OnRecorderButtonListener {

    private var mRecordView: CameraRecordView = rootView.findViewById(R.id.record_view)
    private var mRecorderButton: RecorderButton = rootView.findViewById(R.id.record_btn)
    private var mFocusArea: ImageView = rootView.findViewById(R.id.focus_area)
    private val mFocusRunnable = Runnable {
        mFocusArea.visibility = RelativeLayout.GONE
    }

    init {
        mRecorderButton.setListener(this)
        mRecorderButton.setCanPause(false)
    }

    override fun handleLayerEvent(eventType: Int, params: Any?) {
        when (eventType) {
            EVENT_CHANGE_FILTER -> {
                val filter = params as? GPUImageFilter ?: return
                mRecordView.changeFilter(filter)
            }
            EVENT_FILTER_LIST_SHOW -> {
                mRecorderButton.visibility = GONE
            }
            EVENT_FILTER_LIST_HIDE -> {
                mRecorderButton.visibility = VISIBLE
            }
            EVENT_DESTROY -> {
                mRecordView.onDestroy()
            }
        }
    }

    override fun onStartRecorder(): Boolean {
        mRecordView.startRecord()
        return true
    }

    override fun onStopRecorder(isLongClick: Boolean) {
        mRecordView.stopRecord()
    }

    override fun onHoldRecorder(): Boolean {
        mRecordView.startRecord()
        return true
    }

    override fun onCountDownStart() {
    }

    override fun onFinish(isLongClick: Boolean) {
        mRecordView.stopRecord()
    }

    fun focus(x: Float, y: Float, size: Int, width: Int, height: Int) {
        CameraHelper.getInstance().focus(x, y, size, width, height)
        mFocusArea.run {
            val params = layoutParams as RelativeLayout.LayoutParams
            params.leftMargin = (x - size / 2).toInt()
            params.topMargin = (y - size / 2).toInt()
            layoutParams = params
            visibility = RelativeLayout.VISIBLE
            handler.removeCallbacks(mFocusRunnable)
            handler.postDelayed(mFocusRunnable, 2000)
        }
    }

}