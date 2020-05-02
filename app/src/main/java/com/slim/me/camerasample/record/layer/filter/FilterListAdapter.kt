package com.slim.me.camerasample.record.layer.filter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.slim.me.camerasample.R
import com.slim.me.camerasample.record.render.filter.GPUImageFilter

class FilterListAdapter : RecyclerView.Adapter<FilterListAdapter.FilterViewHolder>() {

    private val mFilterList: ArrayList<GPUImageFilter> = ArrayList()
    private val mFilterNameList: ArrayList<String> = ArrayList()
    private var mFilterChooseCallback: FilterChooseCallback? = null
    private var mSelectedPosition: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        return FilterViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.filter_item_view, parent, false))
    }

    override fun getItemCount(): Int {
        return mFilterList.size
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bindData(mFilterList[position], mFilterNameList[position])
    }

    fun setFilters(list : ArrayList<GPUImageFilter>) {
        mFilterList.clear()
        mFilterList.addAll(list)
    }

    fun setFilterName(list : ArrayList<String>) {
        mFilterNameList.clear()
        mFilterNameList.addAll(list)
    }

    fun setCallback(callback: FilterChooseCallback) {
        mFilterChooseCallback = callback
    }

    inner class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener  {
        private val filterName: TextView = itemView.findViewById(R.id.filter_name)
        private lateinit var filter: GPUImageFilter

        fun bindData(filter: GPUImageFilter, name: String) {
            this.filter = filter
            filterName.text = name
            filterName.setOnClickListener(this)
            filterName.isSelected = mSelectedPosition == adapterPosition
        }

        override fun onClick(v: View?) {
            mFilterChooseCallback?.onChooseFilter(filter)
            mSelectedPosition = adapterPosition
            notifyDataSetChanged()
        }
    }

    interface FilterChooseCallback {
        fun onChooseFilter(filter: GPUImageFilter)
    }

}