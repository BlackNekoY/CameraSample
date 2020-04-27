package com.slim.me.camerasample.record.layer

import com.slim.me.camerasample.record.layer.event.ILayerEvent

abstract class BaseLayer(manager: LayerManager) {
    private val layerManager = manager

    protected fun postLayerEvent(event: ILayerEvent) {
        layerManager.postLayerEvent(event)
    }

    abstract fun handleLayerEvent(event: ILayerEvent)

}