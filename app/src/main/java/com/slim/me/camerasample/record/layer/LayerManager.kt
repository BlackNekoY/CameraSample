package com.slim.me.camerasample.record.layer

class LayerManager {
    private val mLayerList: ArrayList<BaseLayer> = ArrayList()

    fun postLayerEvent(eventType: Int, params: Any?) {
        for (layer in mLayerList) {
            layer.handleLayerEvent(eventType, params)
        }
    }

    fun addLayer(layer: BaseLayer) {
        mLayerList.add(layer)
    }

    fun onDestroy() {
        postLayerEvent(BaseLayer.EVENT_DESTROY, null)
    }
}