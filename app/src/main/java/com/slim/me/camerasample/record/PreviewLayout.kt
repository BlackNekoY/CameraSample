package com.slim.me.camerasample.record

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.slim.me.camerasample.R
import com.slim.me.camerasample.record.layer.LayerManager
import com.slim.me.camerasample.record.layer.filter.FilterLayer
import com.slim.me.camerasample.record.layer.record.RecordLayer

class PreviewLayout : RelativeLayout {

    private lateinit var mLayerManager: LayerManager
    private lateinit var mFilterLayer: FilterLayer
    private lateinit var mRecordLayer: RecordLayer

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
}