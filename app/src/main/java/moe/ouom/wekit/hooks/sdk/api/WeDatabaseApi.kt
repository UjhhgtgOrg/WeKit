package moe.ouom.wekit.hooks.sdk.api

import android.annotation.SuppressLint
import android.content.ContentValues
import de.robv.android.xposed.XposedHelpers
import moe.ouom.wekit.config.ConfigManager
import moe.ouom.wekit.constants.Constants
import moe.ouom.wekit.core.model.ApiHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.util.log.WeLogger
import java.util.concurrent.CopyOnWriteArrayList

@SuppressLint("DiscouragedApi")
@HookItem(path = "API/数据库监听服务", desc = "为其他功能提供数据库写入监听能力")
class WeDatabaseApi : ApiHookItem() {

    // 定义监听器接口
    interface DatabaseInsertListener {
        fun onInsert(table: String, values: ContentValues)
    }

    companion object {
        private val listeners = CopyOnWriteArrayList<DatabaseInsertListener>()

        // 供其他模块注册监听
        fun addListener(listener: DatabaseInsertListener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
                WeLogger.i("WeDatabaseApi: 监听器已添加，当前监听器数量: ${listeners.size}")
            } else {
                WeLogger.w("WeDatabaseApi: 监听器已存在，跳过添加")
            }
        }

        fun removeListener(listener: DatabaseInsertListener) {
            val removed = listeners.remove(listener)
            WeLogger.i("WeDatabaseApi: 监听器移除${if (removed) "成功" else "失败"}，当前监听器数量: ${listeners.size}")
        }
    }

    override fun entry(classLoader: ClassLoader) {
        hookDatabaseInsert(classLoader)
    }

    private fun hookDatabaseInsert(classLoader: ClassLoader) {
        try {
            val clsSQLite = XposedHelpers.findClass(Constants.CLAZZ_SQLITE_DATABASE, classLoader)

            val mInsertWithOnConflict = XposedHelpers.findMethodExact(
                clsSQLite,
                "insertWithOnConflict",
                String::class.java,
                String::class.java,
                ContentValues::class.java,
                Int::class.javaPrimitiveType
            )

            hookAfter(mInsertWithOnConflict) { param ->
                try {
                    val table = param.args[0] as String
                    val values = param.args[2] as ContentValues

                    val config = ConfigManager.getDefaultConfig()
                    val verboseLog = config.getBooleanOrFalse(Constants.PrekVerboseLog)
                    val dbVerboseLog = config.getBooleanOrFalse(Constants.PrekDatabaseVerboseLog)

                    // 分发事件给所有监听者
                    if (listeners.isNotEmpty()) {
                        if (verboseLog) {
                            if (dbVerboseLog) {
                                // 输出完整的 ContentValues
                                WeLogger.d("WeDatabaseApi: 分发数据库插入事件 table=$table 给 ${listeners.size} 个监听器, values=$values")
                            } else {
                                WeLogger.d("WeDatabaseApi: 分发数据库插入事件 table=$table 给 ${listeners.size} 个监听器")
                            }
                        }
                        listeners.forEach { it.onInsert(table, values) }
                    }
                } catch (e: Throwable) {
                    WeLogger.e("WeDatabaseApi: Dispatch failed", e)
                }
            }
            WeLogger.i("WeDatabaseApi: Hook success")
        } catch (e: Throwable) {
            WeLogger.e("WeDatabaseApi: Hook database failed", e)
        }
    }

    override fun unload(classLoader: ClassLoader) {
        listeners.clear()
    }
}