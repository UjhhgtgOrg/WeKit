package com.lxj.easyadapter

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager


/**
 * Description:
 * Create by lxj, at 2018/12/5
 */
object WrapperUtils {
    fun onAttachedToRecyclerView(recyclerView: RecyclerView, fn: (layoutManager: GridLayoutManager, oldLookup: GridLayoutManager.SpanSizeLookup, position: Int)->Int) {
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is GridLayoutManager) {
            val spanSizeLookup = layoutManager.spanSizeLookup

            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return fn(layoutManager, spanSizeLookup, position)
                }
            }
            layoutManager.spanCount = layoutManager.spanCount
        }
    }

    fun setFullSpan(holder: RecyclerView.ViewHolder) {
        val lp = holder.itemView.layoutParams
        if (lp != null && lp is StaggeredGridLayoutManager.LayoutParams) {
            lp.isFullSpan = true
        }
    }
}