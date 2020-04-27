package com.slim.me.camerasample.record.layer.filter

import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.slim.me.camerasample.R
import com.slim.me.camerasample.record.render.filter.GPUImageFilter

class FilterPagerAdapter : PagerAdapter() {

    private val mFilterList: ArrayList<GPUImageFilter> = ArrayList()
    private val mFilterNameList: ArrayList<String> = ArrayList()

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
        val filter = mFilterList[position]
        container.addView(itemView)
        return itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View?)
    }

    override fun isViewFromObject(view: View?, `object`: Any?): Boolean {
        return view == `object`
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
}