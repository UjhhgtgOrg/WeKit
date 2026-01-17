package com.lxj.easyadapter.sample

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.CloneUtils
import com.blankj.utilcode.util.GsonUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.lxj.easyadapter.*

import kotlinx.android.synthetic.main.activity_main.*
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory




class MainActivity : AppCompatActivity() {

    internal var userList: MutableList<User> = ArrayList()
    private var adapter: EasyAdapter<User>? = null
    private var multiItemTypeAdapter: MultiItemTypeAdapter<User>? = null
    val url = "https://img1.baidu.com/it/u=1925715390,133119052&fm=253&fmt=auto&app=138&f=JPEG?w=400&h=400"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        userList.add(User(name = "盛大"))
        userList.add(User(name = "都是"))
        userList.add(User(name = "CEAD"))
        userList.add(User(name = "大图"))
        add.setOnClickListener {
            val old = deepCopy()
            val range = (0 until userList.size)
            userList.add(if(range.isEmpty()) 0 else range.random(), User(id = UUID.randomUUID().toString(), name = "随机添加 - ${(0..1000).random()}" ))
            DiffUtil.calculateDiff(UserDiffCallback(old, userList)).dispatchUpdatesTo(recyclerView.adapter!!)
        }
        del.setOnClickListener {
            if(userList.isNullOrEmpty()) return@setOnClickListener
            val range = (0 until userList.size)
            val old = deepCopy()
            userList.removeAt(range.random())
            DiffUtil.calculateDiff(UserDiffCallback(old, userList)).dispatchUpdatesTo(recyclerView.adapter!!)
        }
        update.setOnClickListener {
            if(userList.isNullOrEmpty()) return@setOnClickListener
            val index = (0 until userList.size).random()
            val old = deepCopy()
            userList[index].name += " - 随机更新"
            DiffUtil.calculateDiff(UserDiffCallback(old, userList)).dispatchUpdatesTo(recyclerView.adapter!!)
        }
        replace.setOnClickListener {
            if(userList.isNullOrEmpty()) return@setOnClickListener
            val index = (0 until userList.size).random()
            val old = deepCopy()
            userList[index] = User(name = "我是随机替换的")
            DiffUtil.calculateDiff(UserDiffCallback(old, userList)).dispatchUpdatesTo(recyclerView.adapter!!)
        }
        move.setOnClickListener {
            if(userList.isNullOrEmpty()) return@setOnClickListener
            val old = deepCopy()
            userList.reverse()
            DiffUtil.calculateDiff(UserDiffCallback(old, userList)).dispatchUpdatesTo(recyclerView.adapter!!)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)

        //prepare data
//        for (i in 0..6) {
//            userList.add(User("本杰明 - $i", i * 2, i))
//        }

//        testHeader()
        testMultiItem()
    }

    fun deepCopy(): List<User>{
        val json = GsonBuilder().create().toJson(userList)
        return GsonBuilder().create().fromJson<List<User>>(json, object : TypeToken<List<User>>() {}.type)
    }

    private fun testHeader() {
        adapter = object : EasyAdapter<User>(userList, R.layout.item) {
            override fun bind(holder: ViewHolder, user: User, position: Int) {
                with(holder) {
                    setText(R.id.tv_name, "name: " + user.name )
                    setText(R.id.tv_age, "age: " + user.age)
                }
            }

        }.apply {
            setOnItemClickListener(object : MultiItemTypeAdapter.SimpleOnItemClickListener() {
                override fun onItemClick(view: View, holder: RecyclerView.ViewHolder, position: Int) {
                    super.onItemClick(view, holder, position)
                    Toast.makeText(this@MainActivity, "position - $position", Toast.LENGTH_SHORT).show()
                    userList.removeAt(position)
//                    notifyDataSetChanged()
                    notifyItemRemoved(position + headersCount)
                }
            })
            addHeaderView(createView("Header - 1，点我在头部添加一条数据"))
            addHeaderView(createView("Header - 2，点我在头部添加一条数据"))
            addFootView(createView("Footer - 1，点我在末尾添加一条数据", true))
            addFootView(createView("Footer - 2，点我在末尾添加一条数据", true))
            recyclerView.adapter = this
        }
    }

    private fun createView(text: String, isFooter: Boolean = false): TextView {
        val textView = TextView(this)
        textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        textView.setPadding(80, 80, 80, 80)
        textView.text = text
        textView.setOnClickListener {
            Toast.makeText(this@MainActivity, textView.text.toString(), Toast.LENGTH_SHORT).show()
            val user = User()
            user.name = "随机添加的名字 - ${System.currentTimeMillis()}"
            user.age = System.currentTimeMillis().toInt()
            if(isFooter){
                //尾部添加
                userList.add(user)
            }else{
                userList.add(0, user)
            }
            multiItemTypeAdapter?.apply { notifyItemInserted(if (isFooter) (userList.size + headersCount) else headersCount) }
            adapter?.apply { notifyItemInserted(if (isFooter) (userList.size + headersCount) else headersCount) }
        }
        return textView
    }

    internal fun testMultiItem() {
        multiItemTypeAdapter = MultiItemTypeAdapter<User>(userList)
                .apply {
                    addItemDelegate(OneDelegate())
                    addItemDelegate(TwoDelegate())
//                    addHeaderView(createView("Multi Header view1111"))
//                    addHeaderView(createView("Multi Header view22222"))
//                    addFootView(createView("Multi Footer view"))
                    setOnItemClickListener(object : MultiItemTypeAdapter.OnItemClickListener {
                        override fun onItemClick(view: View, holder: RecyclerView.ViewHolder, position: Int) {
                            Toast.makeText(this@MainActivity, "position: $position", Toast.LENGTH_SHORT).show()
                        }

                        override fun onItemLongClick(view: View, holder: RecyclerView.ViewHolder, position: Int): Boolean {
                            return false
                        }
                    })
                    recyclerView.adapter = this
                }
    }

    internal inner class OneDelegate : ItemDelegate<User> {

        override fun isThisType(item: User, position: Int): Boolean {
            return position < 2
        }
        override fun bind(holder: ViewHolder, user: User, position: Int) {
            holder.setText(R.id.name, user.name)
            Glide.with(this@MainActivity)
                .load(url).transition(DrawableTransitionOptions.withCrossFade(1000))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.mipmap.ic_launcher_round)
                .into(holder.getView<ImageView>(R.id.avatar))
        }

        //布局更新
        override fun bindWithPayloads(holder: ViewHolder, t: User,
            position: Int, payloads: List<Any>) {
            if(payloads.isNullOrEmpty()) return
            val bundle = payloads[0] as Bundle
            val name = bundle.getString("name") ?: ""
            if(!name.isNullOrEmpty()){
                holder.setText(R.id.name, name)
            }
        }

        override fun getLayoutId(): Int = R.layout.adapter_one
    }

    internal inner class TwoDelegate : ItemDelegate<User> {
        override fun getLayoutId(): Int  = android.R.layout.simple_list_item_1

        override fun isThisType(item: User, position: Int): Boolean {
            return position >=2
        }
        override fun bind(holder: ViewHolder, user: User, position: Int) {
            holder.setText(android.R.id.text1, "age: " + user.age)
            holder.getView<View>(android.R.id.text1).setBackgroundColor(Color.RED)
        }
    }
}
