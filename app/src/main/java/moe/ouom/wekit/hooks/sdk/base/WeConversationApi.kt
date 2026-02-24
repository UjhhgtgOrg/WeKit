package moe.ouom.wekit.hooks.sdk.base

import android.database.Cursor
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import moe.ouom.wekit.core.dsl.dexClass
import moe.ouom.wekit.core.dsl.dexMethod
import moe.ouom.wekit.core.model.ApiHookItem
import moe.ouom.wekit.dexkit.intf.IDexFind
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.utils.log.WeLogger
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "API/对话服务", desc = "为其他功能提供对话管理能力")
object WeConversationApi : ApiHookItem(), IDexFind {

    private const val TAG = "WeConversationApi"
    val classConversationStorage by dexClass()
    val methodUpdateUnreadByTalker by dexMethod()
    val methodHiddenConvParent by dexMethod()
    val methodGetConvByName by dexMethod()

    fun getConversationStorage(): Any {
        return WeServiceApi.storageFeatureService.asResolver()
            .firstMethod {
                returnType = classConversationStorage.clazz
            }
            .invoke()!!
    }

    fun clearUnreadCounts() {
        val cursor = WeDatabaseApi.execQueryMethod!!.invoke(WeDatabaseApi.dbInstance,
            "SELECT username FROM rconversation WHERE unReadCount>0 OR unReadMuteCount>0", arrayOf<String>()) as Cursor
        while (cursor.moveToNext()) {
            val talker = cursor.getString(0)
            try {
                methodUpdateUnreadByTalker.method.invoke(getConversationStorage(), talker)
            }
            catch (ex: Exception) {
                WeLogger.w(TAG, "exception while updating unread count for $talker", ex)
            }
        }
        cursor.close()
    }

    override fun dexFind(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        classConversationStorage.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.storage")
            matcher {
                usingEqStrings("rconversation", "PRAGMA table_info( rconversation)")
            }
        }

        methodUpdateUnreadByTalker.find(dexKit, descriptors) {
            matcher {
                declaredClass(classConversationStorage.clazz)
                usingEqStrings("MicroMsg.ConversationStorage", "updateUnreadByTalker %s")
            }
        }

        methodHiddenConvParent.find(dexKit, descriptors) {
            matcher {
                declaredClass(classConversationStorage.clazz)
                usingEqStrings("Update rconversation set parentRef = '", "' where 1 != 1 ")
            }
        }

        methodGetConvByName.find(dexKit, descriptors) {
            matcher {
                declaredClass(classConversationStorage.clazz)
                usingEqStrings("MicroMsg.ConversationStorage", "get null with username:")
            }
        }

        return descriptors
    }
}