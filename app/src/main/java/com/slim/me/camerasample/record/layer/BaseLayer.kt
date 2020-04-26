package com.slim.me.camerasample.record.layer

abstract class BaseLayer(manager: LayerManager) {
    private val layerManager = manager

    companion object {
        const val EVENT_CHANGE_FILTER = 1
        const val EVENT_FILTER_LIST_SHOW = EVENT_CHANGE_FILTER + 1
        const val EVENT_FILTER_LIST_HIDE = EVENT_FILTER_LIST_SHOW + 1
        const val EVENT_DESTROY = EVENT_FILTER_LIST_HIDE + 1
    }

    protected fun postLayerEvent(eventType: Int, params: Any?) {
        layerManager.postLayerEvent(eventType, params)
    }

    abstract fun handleLayerEvent(eventType: Int, params: Any?)

}