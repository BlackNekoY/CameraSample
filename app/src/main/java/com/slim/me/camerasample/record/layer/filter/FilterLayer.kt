package com.slim.me.camerasample.record.layer.filter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator.REVERSE
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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

class FilterLayer(layerManager: LayerManager, rootView: View) : BaseLayer(layerManager), View.OnClickListener,
        FilterListAdapter.FilterChooseCallback, FilterPagerAdapter.OnScreenClickCallback {
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
        }
        mFilterListName.run {
            add("无")
            add("黑白")
            add("美颜")
            add("日落")
            add("甜美")
            add("温柔")
        }
        mAdapter.setFilters(mFilterList)
        mAdapter.setFilterName(mFilterListName)

        mPagerAdapter.setFilters(mFilterList)
        mPagerAdapter.setFilterName(mFilterListName)
        mPagerAdapter.setCallback(this)
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
                        val view = mFilterPager.findViewWithTag<View>(selectPos)
                        view?.let {
                            doAnim(it)
                        }
                    }
                    ViewPager.SCROLL_STATE_DRAGGING -> {}
                    ViewPager.SCROLL_STATE_SETTLING -> {}
                }
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
            }

            override fun onPageSelected(position: Int) {
                selectPos = position
            }
        })
    }

    private fun doAnim(view: View) {
        view.visibility = View.GONE
        val animator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        animator.addListener(object : AnimatorListenerAdapter(){
            override fun onAnimationStart(animation: Animator?) {
                view.alpha = 0f
                view.visibility = View.VISIBLE
            }
        })
        animator.repeatCount = 1
        animator.repeatMode = REVERSE
        animator.duration = 500
        animator.startDelay = 200
        animator.start()
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

    override fun onScreenClick(x: Float, y: Float) {
        val bundle = Bundle()
        bundle.putFloat("x", x)
        bundle.putFloat("y", y)
        postLayerEvent(CommonLayerEvent(ILayerEvent.EVENT_FOCUS_PRESS, bundle))
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
}