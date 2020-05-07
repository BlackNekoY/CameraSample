package com.slim.me.camerasample.record.layer.event

interface ILayerEvent {

    companion object {
        const val EVENT_CHANGE_FILTER = 1
        const val EVENT_FILTER_ICON_CLICK = EVENT_CHANGE_FILTER + 1
        const val EVENT_FOCUS_PRESS = EVENT_FILTER_ICON_CLICK + 1
        const val EVENT_FILTER_ON_SCROLL = EVENT_FOCUS_PRESS + 1
        const val EVENT_DESTROY = EVENT_FILTER_ON_SCROLL + 1
        const val EVENT_START_RECORD = EVENT_DESTROY + 1
        const val EVENT_STOP_RECORD = EVENT_START_RECORD + 1
    }

    fun getType() : Int

    fun getParams(): Any?

    fun <T> getParam(clazz: Class<T>): T?
}