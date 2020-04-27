package com.slim.me.camerasample.record.layer

import com.slim.me.camerasample.record.layer.event.CommonLayerEvent
import com.slim.me.camerasample.record.layer.event.ILayerEvent
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_DESTROY

class LayerManager {
    private val mLayerList: ArrayList<BaseLayer> = ArrayList()

    fun postLayerEvent(event: ILayerEvent) {
        for (layer in mLayerList) {
            layer.handleLayerEvent(event)
        }
    }

    fun addLayer(layer: BaseLayer) {
        mLayerList.add(layer)
    }

    fun onDestroy() {
        postLayerEvent(CommonLayerEvent(EVENT_DESTROY))
    }
}