package com.lxj.easyadapter

import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ViewHolder(val convertView: View) : RecyclerView.ViewHolder(convertView) {
    private val mViews: SparseArray<View> = SparseArray()

    fun <T : View> getView(viewId: Int): T {
        var view: View? = mViews.get(viewId)
        if (view == null) {
            view = convertView.findViewById(viewId)
            mViews.put(viewId, view)
        }
        return view as T
    }

    fun <T : View> getViewOrNull(viewId: Int): T? {
        var view: View? = mViews.get(viewId)
        if (view == null) {
            view = convertView.findViewById(viewId)
            mViews.put(viewId, view)
        }
        return view as? T
    }

    fun setText(viewId: Int, text: CharSequence): ViewHolder {
        val tv = getView<TextView>(viewId)
        tv.text = text
        return this
    }

    fun setImageResource(viewId: Int, resId: Int): ViewHolder {
        val view = getView<ImageView>(viewId)
        view.setImageResource(resId)
        return this
    }

    companion object {
        fun createViewHolder(itemView: View): ViewHolder {
            return ViewHolder(itemView)
        }

        fun createViewHolder(context: Context,
                             parent: ViewGroup, layoutId: Int): ViewHolder {
            val itemView = LayoutInflater.from(context).cloneInContext(context).inflate(layoutId, parent,
                    false)
            return ViewHolder( itemView)
        }
    }


}
