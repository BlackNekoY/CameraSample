package com.slim.me.camerasample.record.layer.event

import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_CHANGE_FILTER
import com.slim.me.camerasample.record.render.filter.GPUImageFilter

class ChangeFilterEvent(leftFilter: GPUImageFilter, rightFilter: GPUImageFilter?) : CommonLayerEvent(EVENT_CHANGE_FILTER) {
    val leftFilter = leftFilter
    val rightFilter = rightFilter
}