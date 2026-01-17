# EasyAdapter
An simplify and powerful version for hongyangAndroid [baseAdapter].

I remove some class, change some api, rewrite code, to be more simple, support kotlin.

# Gradle
[![](https://jitpack.io/v/li-xiaojun/EasyAdapter.svg)](https://jitpack.io/#li-xiaojun/EasyAdapter)
```groovy
implementation 'com.github.li-xiaojun:EasyAdapter:Tag'
```

Add it in your root build.gradle at the end of repositories:
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```


# Sample
普通使用：
```kotlin
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
```


多条目：
```kotlin
multiItemTypeAdapter = MultiItemTypeAdapter<User>(userList)
    .apply {
        addItemDelegate(OneDelegate())
        addItemDelegate(TwoDelegate())
        addHeaderView(createView("Multi Header view1111"))
        addHeaderView(createView("Multi Header view22222"))
        addFootView(createView("Multi Footer view"))
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

internal inner class OneDelegate : ItemDelegate<User> {
    override val layoutId: Int
        get() = android.R.layout.simple_list_item_1

    override fun isThisType(item: User, position: Int): Boolean {
        return position % 2 != 0
    }
    override fun bind(holder: ViewHolder, user: User, position: Int) {
        holder.setText(android.R.id.text1, "name: " + user.name + " - " + position)
    }
}

internal inner class TwoDelegate : ItemDelegate<User> {
    override val layoutId: Int
        get() = android.R.layout.simple_list_item_1

    override fun isThisType(item: User, position: Int): Boolean {
        return position % 2 == 0
    }
    override fun bind(holder: ViewHolder, user: User, position: Int) {
        holder.setText(android.R.id.text1, "age: " + user.age)
        holder.getView<View>(android.R.id.text1).setBackgroundColor(Color.RED)
    }
}
```