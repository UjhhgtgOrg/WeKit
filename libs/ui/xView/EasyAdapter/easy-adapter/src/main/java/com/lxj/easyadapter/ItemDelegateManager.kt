package com.lxj.easyadapter

import android.util.SparseArray


/**
 * Created by zhy on 16/6/22.
 */
class ItemDelegateManager<T> {
    private var delegates: SparseArray<ItemDelegate<T>> = SparseArray()

    val itemViewDelegateCount: Int
        get() = delegates.size()

    fun addDelegate(delegate: ItemDelegate<T>): ItemDelegateManager<T> {
        var viewType = delegates.size()
        delegates.put(viewType, delegate)
        viewType++
        return this
    }

    fun addDelegate(viewType: Int, delegate: ItemDelegate<T>): ItemDelegateManager<T> {
        if (delegates.get(viewType) != null) {
            throw IllegalArgumentException(
                    "An ItemDelegate is already registered for the viewType = "
                            + viewType
                            + ". Already registered ItemDelegate is "
                            + delegates.get(viewType))
        }
        delegates.put(viewType, delegate)
        return this
    }

    fun removeDelegate(delegate: ItemDelegate<T>): ItemDelegateManager<T> {
        val indexToRemove = delegates.indexOfValue(delegate)

        if (indexToRemove >= 0) {
            delegates.removeAt(indexToRemove)
        }
        return this
    }

    fun removeDelegate(itemType: Int): ItemDelegateManager<T> {
        val indexToRemove = delegates.indexOfKey(itemType)

        if (indexToRemove >= 0) {
            delegates.removeAt(indexToRemove)
        }
        return this
    }

    fun getItemViewType(item: T, position: Int): Int {
        val delegatesCount = delegates.size()
        for (i in delegatesCount - 1 downTo 0) {
            val delegate = delegates.valueAt(i)
            if (delegate.isThisType(item, position)) {
                return delegates.keyAt(i)
            }
        }
        return 0
//        throw IllegalArgumentException(
//                "No ItemDelegate added that matches position=$position in data source")
    }

    fun convert(holder: ViewHolder, item: T, position: Int, payloads: List<Any>? = null) {
        val delegatesCount = delegates.size()
        for (i in 0 until delegatesCount) {
            val delegate = delegates.valueAt(i)

            if (delegate.isThisType(item, position)) {
                if (payloads.isNullOrEmpty()){
                    delegate.bind(holder, item, position)
                }else{
                    delegate.bindWithPayloads(holder, item, position, payloads)
                }
                return
            }
        }
//        throw IllegalArgumentException(
//                "No ItemDelegateManager added that matches position=$position in data source")
    }


    fun getItemViewDelegate(viewType: Int): ItemDelegate<T> {
        return delegates.get(viewType)!!
    }

    fun getItemLayoutId(viewType: Int): Int {
        return getItemViewDelegate(viewType).getLayoutId()
    }

    fun getItemViewType(itemViewDelegate: ItemDelegate<T>): Int {
        return delegates.indexOfValue(itemViewDelegate)
    }
}
