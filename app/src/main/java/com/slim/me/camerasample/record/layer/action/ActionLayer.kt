package com.slim.me.camerasample.record.layer.action

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import com.slim.me.camerasample.R
import com.slim.me.camerasample.edit.EditActivity
import com.slim.me.camerasample.record.layer.BaseLayer
import com.slim.me.camerasample.record.layer.LayerManager
import com.slim.me.camerasample.record.layer.event.CommonLayerEvent
import com.slim.me.camerasample.record.layer.event.ILayerEvent
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_FILTER_ICON_CLICK
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_START_RECORD
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_STOP_RECORD
import com.slim.me.camerasample.util.FileUtils

/**
 * 各种按钮的Layer
 */
class ActionLayer(layerManager: LayerManager, rootView: View) : BaseLayer(layerManager), View.OnClickListener {
    private val mFilterBtn: View = rootView.findViewById(R.id.filter)
    private val mAfterRecordArea: View = rootView.findViewById(R.id.after_record_area)
    private val mNextBtn: View = rootView.findViewById(R.id.next)
    private val mBackBtn: View = rootView.findViewById(R.id.back)

    private var mVideoPath: String? = null
    init {
        mFilterBtn.setOnClickListener(this)
        mNextBtn.setOnClickListener(this)
        mBackBtn.setOnClickListener(this)
    }

    override fun handleLayerEvent(event: ILayerEvent) {
        when (event.getType()) {
            EVENT_START_RECORD -> mAfterRecordArea.visibility = GONE
            EVENT_STOP_RECORD -> {
                val videoPath = event.getParam(String::class.java)
                if (FileUtils.isFileExist(videoPath)) {
                    mVideoPath = videoPath
                    mAfterRecordArea.visibility = VISIBLE
                }
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.filter -> postLayerEvent(CommonLayerEvent(EVENT_FILTER_ICON_CLICK))
            R.id.back -> {
                mAfterRecordArea.visibility = GONE
            }
            R.id.next -> {
                mVideoPath?.let {
                    val intent = Intent(v.context, EditActivity::class.java)
                    intent.putExtra(EditActivity.VIDEO_PATH, it)
                    v.context.startActivity(intent)
                }
            }
        }
    }
}