package com.slim.me.camerasample.record.layer.event

class CommonLayerEvent : ILayerEvent {

    private val mType : Int
    private val mParams : Any?

    constructor(type: Int) : this(type, null)

    constructor(type: Int, params: Any?) {
        mType = type
        mParams = params
    }

    override fun getType(): Int {
        return mType
    }

    override fun getParams(): Any? {
        return mParams
    }

    override fun <T> getParam(clazz: Class<T>): T? {
        return mParams as? T
    }

}