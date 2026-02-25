package moe.ouom.wekit.hooks.items.chat

import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import de.robv.android.xposed.XC_MethodHook
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.base.WeMessageApi
import moe.ouom.wekit.hooks.sdk.base.WeServiceApi
import moe.ouom.wekit.hooks.sdk.base.model.MessageInfo
import moe.ouom.wekit.hooks.sdk.ui.WeChatItemCreateViewListenerApi
import moe.ouom.wekit.utils.common.SimpleLruCache

@HookItem(path = "聊天增强/自动语音转文字", desc = "自动将语音消息转为文字")
object AutoSpeechToText : BaseSwitchFunctionHookItem(), WeChatItemCreateViewListenerApi.ICreateViewListener {

    private val cache = SimpleLruCache<Long, Boolean>(100)

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
        val id = msgInfo.id
        if (cache[id] == true) {
            return
        }

        val apiManager = chattingContext.asResolver()
            .firstField {
                type = WeServiceApi.methodApiManagerGetApi.method.declaringClass
            }
            .get()!!
        val api = WeServiceApi.methodApiManagerGetApi.method.invoke(apiManager, WeMessageApi.classTransformChattingComponent.clazz.interfaces[0])
        val chatViewItem = api.asResolver()
            .firstMethod {
                parameters(Long::class)
                returnType { clazz ->
                    clazz.name.startsWith("com.tencent.mm.ui.chatting.viewitems")
                }
            }
            .invoke(id)

        if (chatViewItem.toString() == "NoTransform") {
            cache[id] = true
            api.asResolver()
                .firstMethod {
                    parameters(WeMessageApi.classMsgInfo.clazz, Boolean::class, Int::class, Int::class)
                    returnType = Unit::class
                }
                .invoke(msgInfo.instance, false, -1, 0)
        }
    }
}