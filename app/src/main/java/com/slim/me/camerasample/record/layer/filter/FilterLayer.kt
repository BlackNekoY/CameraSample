package com.slim.me.camerasample.record.layer.filter

import android.graphics.BitmapFactory
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.slim.me.camerasample.R
import com.slim.me.camerasample.record.layer.BaseLayer
import com.slim.me.camerasample.record.layer.LayerManager
import com.slim.me.camerasample.record.render.filter.*

class FilterLayer(layerManager: LayerManager, rootView: View) : BaseLayer(layerManager), View.OnClickListener, FilterListAdapter.FilterChooseCallback {
    private val mRoot: View = rootView
    private val mFilterView: View = rootView.findViewById(R.id.filter)
    private val mFilterListView: RecyclerView = rootView.findViewById(R.id.filter_list)

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
    }

    private fun createFilterGroup(vararg filters: GPUImageFilter) : ImageFilterGroup {
        val list = ArrayList<GPUImageFilter>()
        list.addAll(filters)
        return ImageFilterGroup(list)
    }

    override fun onChooseFilter(filter: GPUImageFilter) {
        postLayerEvent(EVENT_CHANGE_FILTER, filter)
    }

    override fun handleLayerEvent(eventType: Int, params: Any?) {
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.filter -> {
                if (mFilterListView.isShown) {
                    mFilterListView.visibility = View.GONE
                    postLayerEvent(EVENT_FILTER_LIST_HIDE, null)
                } else {
                    mFilterListView.visibility = View.VISIBLE
                    postLayerEvent(EVENT_FILTER_LIST_SHOW, null)
                }
            }
        }
    }
}