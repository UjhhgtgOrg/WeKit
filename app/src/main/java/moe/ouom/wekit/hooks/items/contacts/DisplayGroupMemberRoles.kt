package moe.ouom.wekit.hooks.items.contacts

import android.view.View
import de.robv.android.xposed.XC_MethodHook
import moe.ouom.wekit.core.dsl.dexClass
import moe.ouom.wekit.core.dsl.dexMethod
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.dexkit.intf.IDexFind
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.base.model.MessageInfo
import moe.ouom.wekit.hooks.sdk.ui.WeChatItemCreateViewListenerApi
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "联系人与群组/显示群成员身份", desc = "在群聊中显示群成员的身份: 群主, 管理员, 成员 (没写完)")
object DisplayGroupMemberRoles : BaseSwitchFunctionHookItem(), IDexFind,
    WeChatItemCreateViewListenerApi.ICreateViewListener {

    private val classChatroomMember by dexClass()
    private val methodGetChatroomData by dexMethod()

    override fun entry(classLoader: ClassLoader) {
        WeChatItemCreateViewListenerApi.addListener(this)
    }

    override fun unload(classLoader: ClassLoader) {
        WeChatItemCreateViewListenerApi.removeListener(this)
        super.unload(classLoader)
    }

    override fun onCreateView(
        hookParam: XC_MethodHook.MethodHookParam,
        view: View,
        chattingContext: Any,
        msgInfo: MessageInfo
    ) {
        if (!msgInfo.isInGroupChat) return

    }

    override fun dexFind(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        classChatroomMember.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.storage")
            matcher {
                usingEqStrings("MicroMsg.ChatRoomMember", "service is null")
            }
        }

        methodGetChatroomData.find(dexKit, descriptors) {
            matcher {
                usingEqStrings("MicroMsg.ChatRoomMember", "getChatroomData hashMap is null!")
            }
        }

        return descriptors
    }
}