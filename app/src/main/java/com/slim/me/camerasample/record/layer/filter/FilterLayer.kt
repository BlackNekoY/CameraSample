package com.slim.me.camerasample.record.layer.filter

import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.slim.me.camerasample.R
import com.slim.me.camerasample.record.layer.BaseLayer
import com.slim.me.camerasample.record.layer.LayerManager
import com.slim.me.camerasample.record.layer.event.CommonLayerEvent
import com.slim.me.camerasample.record.layer.event.ILayerEvent
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_CHANGE_FILTER
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_FILTER_LIST_HIDE
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_FILTER_LIST_SHOW
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_FOCUS_PRESS
import com.slim.me.camerasample.record.render.filter.*
import com.slim.me.camerasample.util.UIUtil
import kotlin.math.abs

class FilterLayer(layerManager: LayerManager, rootView: View) : BaseLayer(layerManager), View.OnClickListener,
        FilterListAdapter.FilterChooseCallback, View.OnTouchListener {
    private val mRoot: View = rootView
    private val mFilterView: View = rootView.findViewById(R.id.filter)
    private val mFilterListView: RecyclerView = rootView.findViewById(R.id.filter_list)

    private var mPressX = 0f
    private var mPressY = 0f

    private val mAdapter: FilterListAdapter = FilterListAdapter()

    init {
        initFilters()
        initLayer()
    }

    private fun initFilters() {
        val filterList = ArrayList<GPUImageFilter>()
        filterList.run {
            add(OESFilter())
            add(createFilterGroup(OESFilter(), BlackWhiteFilter()))
            add(createFilterGroup(OESFilter(), BeautyFilter()))
            add(createFilterGroup(OESFilter(), SunsetFilter()))
            add(createFilterGroup(OESFilter(), SweetsFilter()))
            add(createFilterGroup(OESFilter(), TenderFilter()))
            add(createFilterGroup(OESFilter(), WatermarkFilter(BitmapFactory.decodeResource(mRoot.resources, R.drawable.awesomeface))))
        }
        val filterNameList = ArrayList<String>()
        filterNameList.run {
            add("无")
            add("黑白")
            add("美颜")
            add("日落")
            add("甜美")
            add("温柔")
            add("水印")
        }
        mAdapter.setFilters(filterList)
        mAdapter.setFilterName(filterNameList)
    }

    private fun initLayer() {
        mFilterView.setOnClickListener(this)
        val layoutManager = LinearLayoutManager(mRoot.context)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        mFilterListView.layoutManager = layoutManager
        mAdapter.setCallback(this)
        mFilterListView.adapter = mAdapter

        mRoot.setOnTouchListener(this)
    }

    private fun createFilterGroup(vararg filters: GPUImageFilter) : ImageFilterGroup {
        val list = ArrayList<GPUImageFilter>()
        list.addAll(filters)
        return ImageFilterGroup(list)
    }

    override fun onChooseFilter(filter: GPUImageFilter) {
        postLayerEvent(CommonLayerEvent(EVENT_CHANGE_FILTER, filter))
    }

    override fun handleLayerEvent(event: ILayerEvent) {
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.filter -> {
                if (mFilterListView.isShown) {
                    mFilterListView.visibility = View.GONE
                    postLayerEvent(CommonLayerEvent(EVENT_FILTER_LIST_HIDE))
                } else {
                    mFilterListView.visibility = View.VISIBLE
                    postLayerEvent(CommonLayerEvent(EVENT_FILTER_LIST_SHOW))
                }
            }
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPressX = x
                mPressY = y
            }
            MotionEvent.ACTION_UP -> {
                val dx = x - mPressX
                val dy = y - mPressY
                val slop = ViewConfiguration.get(v.context).scaledTouchSlop
                if (abs(dx) < slop && abs(dy) < slop) {
                    val bundle = Bundle()
                    bundle.putFloat("x", x)
                    bundle.putFloat("y", y)
                    postLayerEvent(CommonLayerEvent(EVENT_FOCUS_PRESS, bundle))
                }
            }
        }
        return true
    }
}