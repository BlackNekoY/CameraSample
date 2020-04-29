package com.slim.me.camerasample.record.layer.filter

import android.os.SystemClock
import android.support.v4.view.PagerAdapter
import android.view.*
import android.widget.TextView
import com.slim.me.camerasample.R
import com.slim.me.camerasample.record.render.filter.GPUImageFilter
import kotlin.math.abs

class FilterPagerAdapter : PagerAdapter(), View.OnTouchListener {

    private val mFilterList: ArrayList<GPUImageFilter> = ArrayList()
    private val mFilterNameList: ArrayList<String> = ArrayList()
    private var mCallback: OnScreenClickCallback? = null

    override fun getCount(): Int {
        return mFilterList.size
    }

    override fun getItemPosition(`object`: Any?): Int {
        val filter = `object` as? GPUImageFilter ?: return 0
        return mFilterList.indexOf(filter)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val itemView = LayoutInflater.from(container.context).inflate(R.layout.filter_pager_item_view, container, false)
        val textView = itemView.findViewById<TextView>(R.id.name)
        textView.text = mFilterNameList[position]
        container.addView(itemView)
        itemView.setOnTouchListener(this)
        return itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View?)
    }

    override fun isViewFromObject(view: View?, `object`: Any?): Boolean {
        return view == `object`
    }

    fun setCallback(callback: OnScreenClickCallback) {
        mCallback = callback
    }

    fun setFilters(list : ArrayList<GPUImageFilter>) {
        mFilterList.clear()
        mFilterList.addAll(list)
    }

    fun setFilterName(list : ArrayList<String>) {
        mFilterNameList.clear()
        mFilterNameList.addAll(list)
    }

    fun getFilter(position: Int) : GPUImageFilter?{
        if (position >= 0 && position < mFilterList.size) {
            return mFilterList[position]
        }
        return null
    }

    private var mPressTime = 0L
    private var mPressX = 0f
    private var mPressY = 0f
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val dx = x - mPressX
        val dy = y - mPressY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPressX = x
                mPressY = y
                mPressTime = SystemClock.uptimeMillis()
            }
            MotionEvent.ACTION_MOVE -> { }
            MotionEvent.ACTION_UP -> {
                val currTime = SystemClock.uptimeMillis()
                val slop = ViewConfiguration.get(v.context).scaledTouchSlop
                if (abs(dx) < slop && abs(dy) < slop && abs(currTime - mPressTime) < 100) {
                    // 触发点击对焦
                    mCallback?.onScreenClick(x, y)
                }
                mPressTime = 0L
            }
        }
        return true
    }

    interface OnScreenClickCallback {
        fun onScreenClick(x: Float, y: Float)
    }
}