package com.lxj.easyadapter

/**
 * Created by zhy on 16/6/22.
 */
interface ItemDelegate<T> {

    fun getLayoutId(): Int

    fun isThisType(item: T, position: Int): Boolean

    fun bind(holder: ViewHolder, t: T, position: Int)

    fun bindWithPayloads(holder: ViewHolder, t: T, position: Int, payloads: List<Any>) {
        bind(holder, t, position)
    }


}
