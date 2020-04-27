package com.slim.me.camerasample.record.layer.filter

import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import com.slim.me.camerasample.R
import com.slim.me.camerasample.record.layer.BaseLayer
import com.slim.me.camerasample.record.layer.LayerManager
import com.slim.me.camerasample.record.layer.event.ChangeFilterEvent
import com.slim.me.camerasample.record.layer.event.CommonLayerEvent
import com.slim.me.camerasample.record.layer.event.ILayerEvent
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_FILTER_LIST_HIDE
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_FILTER_LIST_SHOW
import com.slim.me.camerasample.record.layer.event.ILayerEvent.Companion.EVENT_FILTER_ON_SCROLL
import com.slim.me.camerasample.record.render.filter.*
import com.slim.me.camerasample.util.UIUtil
import kotlin.math.abs

class FilterLayer(layerManager: LayerManager, rootView: View) : BaseLayer(layerManager), View.OnClickListener,
        FilterListAdapter.FilterChooseCallback {
    private val mRoot: View = rootView
    private val mFilterView: View = rootView.findViewById(R.id.filter)
    private val mFilterListView: RecyclerView = rootView.findViewById(R.id.filter_list)
    private val mFilterPager: ViewPager = rootView.findViewById(R.id.filter_pager)

    private val mAdapter: FilterListAdapter = FilterListAdapter()
    private val mPagerAdapter: FilterPagerAdapter = FilterPagerAdapter()
    private val mFilterList = ArrayList<GPUImageFilter>()
    private val mFilterListName = ArrayList<String>()

    init {
        initFilters()
        initLayer()
    }

    private fun initFilters() {
        mFilterList.run {
            add(OESFilter())
            add(createFilterGroup(OESFilter(), BlackWhiteFilter()))
            add(createFilterGroup(OESFilter(), BeautyFilter()))
            add(createFilterGroup(OESFilter(), SunsetFilter()))
            add(createFilterGroup(OESFilter(), SweetsFilter()))
            add(createFilterGroup(OESFilter(), TenderFilter()))
            add(createFilterGroup(OESFilter(), WatermarkFilter(BitmapFactory.decodeResource(mRoot.resources, R.drawable.awesomeface))))
        }
        mFilterListName.run {
            add("无")
            add("黑白")
            add("美颜")
            add("日落")
            add("甜美")
            add("温柔")
            add("水印")
        }
        mAdapter.setFilters(mFilterList)
        mAdapter.setFilterName(mFilterListName)

        mPagerAdapter.setFilters(mFilterList)
        mPagerAdapter.setFilterName(mFilterListName)
    }

    private fun initLayer() {
        mFilterView.setOnClickListener(this)
        val layoutManager = LinearLayoutManager(mRoot.context)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        mFilterListView.layoutManager = layoutManager
        mAdapter.setCallback(this)
        mFilterListView.adapter = mAdapter

        mFilterPager.adapter = mPagerAdapter
        mFilterPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
            private var selectPos = 0
            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager.SCROLL_STATE_IDLE -> {
                        // ViewPager处于静止
                        postLayerEvent(ChangeFilterEvent(mFilterList[selectPos], null))
                        postLayerEvent(CommonLayerEvent(EVENT_FILTER_ON_SCROLL, 1f))
                    }
                    ViewPager.SCROLL_STATE_DRAGGING -> {}
                    ViewPager.SCROLL_STATE_SETTLING -> {}
                }
                Log.d("slim", "onPageScrollStateChanged: $state")
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                val leftFilter = mFilterList[position]
                val rightFilter : GPUImageFilter? = if (position + 1 >= 1 && position + 1 < mFilterList.size) {
                    mFilterList[position + 1]
                } else {
                    null
                }
                postLayerEvent(ChangeFilterEvent(leftFilter, rightFilter))
                postLayerEvent(CommonLayerEvent(EVENT_FILTER_ON_SCROLL, 1 - positionOffset))
                Log.d("slim", "onPageScrolled: position = $position, positionOffset = $positionOffset, positionOffsetPixels = $positionOffsetPixels")
            }

            override fun onPageSelected(position: Int) {
                selectPos = position
                Log.d("slim", "onPageSelected: $position")
            }
        })
    }

    private fun createFilterGroup(vararg filters: GPUImageFilter) : ImageFilterGroup {
        val list = ArrayList<GPUImageFilter>()
        list.addAll(filters)
        return ImageFilterGroup(list)
    }

    override fun onChooseFilter(filter: GPUImageFilter) {
        postLayerEvent(ChangeFilterEvent(filter, null))
    }

    override fun handleLayerEvent(event: ILayerEvent) {}

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
}