package com.slim.me.camerasample.record.layer.record

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.RelativeLayout
import com.slim.me.camerasample.R
import com.slim.me.camerasample.camera.CameraHelper
import com.slim.me.camerasample.record.encoder.EncodeConfig
import com.slim.me.camerasample.record.layer.BaseLayer
import com.slim.me.camerasample.record.layer.LayerManager
import com.slim.me.camerasample.record.layer.event.ChangeFilterEvent
import com.slim.me.camerasample.record.layer.event.CommonLayerEvent
import com.slim.me.camerasample.record.layer.event.ILayerEvent
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_CHANGE_FILTER
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_DESTROY
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_FILTER_ICON_CLICK
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_FILTER_ON_SCROLL
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_FOCUS_PRESS
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_START_RECORD
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_STOP_RECORD
import com.slim.me.camerasample.util.UIUtil

class RecordLayer(layerManager: LayerManager, rootView: View) : BaseLayer(layerManager),
        RecorderButton.OnRecorderButtonListener, CameraRecordView.OnRecordCallback {

    private var mRecordView: CameraRecordView = rootView.findViewById(R.id.record_view)
    private var mRecorderButton: RecorderButton = rootView.findViewById(R.id.record_btn)
    private var mFocusArea: ImageView = rootView.findViewById(R.id.focus_area)
    private val mFocusRunnable = Runnable {
        mFocusArea.visibility = RelativeLayout.GONE
    }

    init {
        mRecorderButton.setListener(this)
        mRecorderButton.setCanPause(false)
        mRecordView.setRecordCallback(this)
    }

    override fun handleLayerEvent(event: ILayerEvent) {
        when (event.getType()) {
            EVENT_CHANGE_FILTER -> {
                val changeEvent = event as? ChangeFilterEvent ?: return
                mRecordView.changeFilter(changeEvent.leftFilter, changeEvent.rightFilter)
            }
            EVENT_FILTER_ON_SCROLL -> {
                val x = event.getParam(Float::class.java) ?: return
                mRecordView.changeScrollX(x)
            }
            EVENT_FILTER_ICON_CLICK -> mRecorderButton.visibility = if (mRecorderButton.isShown) GONE else VISIBLE
            EVENT_FOCUS_PRESS -> {
                if (mRecorderButton.visibility != VISIBLE) {
                    mRecorderButton.visibility = VISIBLE
                }
                val bundle = event.getParam(Bundle::class.java) ?: return
                val x = bundle.getFloat("x")
                val y = bundle.getFloat("y")
                val size = UIUtil.dip2px(mRecordView.context, 80f)
                focus(x, y, size, mRecordView.width, mRecordView.height)
            }
            EVENT_DESTROY -> {
                mRecordView.onDestroy()
            }
        }
    }

    override fun onStartRecorder(): Boolean {
        startRecord()
        return true
    }

    override fun onStopRecorder(isLongClick: Boolean) {
        stopRecord()
    }

    override fun onHoldRecorder(): Boolean {
        startRecord()
        return true
    }

    override fun onCountDownStart() {
    }

    override fun onFinish(isLongClick: Boolean) {
        stopRecord()
    }

    override fun onStartRecord() {
        mRecordView.post {
            postLayerEvent(CommonLayerEvent(EVENT_START_RECORD))
        }
    }

    override fun onStopRecord(encodeConfig: EncodeConfig) {
        mRecordView.post {
            postLayerEvent(CommonLayerEvent(EVENT_STOP_RECORD, encodeConfig.outputPath))
        }
    }

    private fun startRecord() {
        mRecordView.startRecord()
    }

    private fun stopRecord() {
        mRecordView.stopRecord()
    }

    private fun focus(x: Float, y: Float, size: Int, width: Int, height: Int) {
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