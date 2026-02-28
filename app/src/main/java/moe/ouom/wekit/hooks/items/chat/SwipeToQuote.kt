package moe.ouom.wekit.hooks.items.chat

import android.annotation.SuppressLint
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.widget.FrameLayout
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import de.robv.android.xposed.XC_MethodHook
import moe.ouom.wekit.core.dsl.dexClass
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.dexkit.intf.IDexFind
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.base.WeMessageApi
import moe.ouom.wekit.hooks.sdk.base.WeServiceApi
import moe.ouom.wekit.hooks.sdk.base.model.MessageInfo
import moe.ouom.wekit.hooks.sdk.ui.WeChatMessageViewApi
import moe.ouom.wekit.utils.findHostViewByIdStr
import org.luckypray.dexkit.DexKitBridge
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp

//@HookItem(path = "聊天/左划引用消息", desc = "在消息上左划以引用")
//object SwipeToQuote : BaseSwitchFunctionHookItem(),
//    WeChatMessageViewApi.ICreateViewListener {
//
//    override fun entry(classLoader: ClassLoader) {
//        WeChatMessageViewApi.addListener(this)
//    }
//
//    override fun unload(classLoader: ClassLoader) {
//        WeChatMessageViewApi.removeListener(this)
//        super.unload(classLoader)
//    }
//
//    override fun onCreateView(
//        param: XC_MethodHook.MethodHookParam,
//        view: View,
//        chattingContext: Any,
//        msgInfo: MessageInfo
//    ) {
//        val viewGroup = view as ViewGroup
//        val messageView = runCatching { viewGroup.findHostViewByIdStr<View>("bkj") }.getOrNull() ?: return
//        val messageViewGroup = messageView.parent as ViewGroup
//        messageViewGroup.removeView(messageView)
//        val lifecycleOwner = XposedLifecycleOwner().apply { onCreate(); onResume() }
//        // FIXME: this is soooooo fucking cursed
//        try {
//            val messageHolder = ComposeView(messageView.context).apply {
//                setViewTreeLifecycleOwner(lifecycleOwner)
//                setViewTreeViewModelStoreOwner(lifecycleOwner)
//                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
//
//                setContent {
//                    AppTheme {
//                        AndroidView(
//                            factory = {
//                                messageView
//                            }
//                        )
//                    }
//                }
//            }
//            viewGroup.addView(messageHolder)
//        }
//        catch (ex: Exception) {
//            WeLogger.e("SwipeToQuote", "exception while wrapping message view inside Compose")
//        }
//    }
//}

@HookItem(path = "聊天/左划引用消息", desc = "在消息上左划以引用")
object SwipeToQuote : BaseSwitchFunctionHookItem(), IDexFind,
    WeChatMessageViewApi.ICreateViewListener {

    override fun entry(classLoader: ClassLoader) {
        WeChatMessageViewApi.addListener(this)
    }

    override fun unload(classLoader: ClassLoader) {
        WeChatMessageViewApi.removeListener(this)
        super.unload(classLoader)
    }

    override fun onCreateView(
        param: XC_MethodHook.MethodHookParam,
        view: View,
        chattingContext: Any,
        msgInfo: MessageInfo
    ) {
        val viewGroup = view as? ViewGroup ?: return
        val messageView = runCatching { viewGroup.findHostViewByIdStr<View>("bkj") }.getOrNull() ?: return

        attachSwipeGesture(messageView, chattingContext, msgInfo)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachSwipeGesture(
        messageView: View,
        chattingContext: Any,
        msgInfo: MessageInfo
    ) {
        if (messageView !is ViewGroup) return

        var startX = 0f
        var startY = 0f
        var isDragging = false
        val triggerThreshold = dpToPx(messageView.context, 80).toFloat()
        var triggered = false

        ViewGroup::class.asResolver()
            .firstMethod {
                name = "onInterceptTouchEvent"
            }
            .hookAfter { param ->
                val v = param.thisObject as ViewGroup
                if (v !== messageView) return@hookAfter

                val event = param.args[0] as MotionEvent

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        isDragging = false
                        triggered = false
                        // Never intercept DOWN — children need it to arm their listeners
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - startX
                        val dy = event.y - startY

                        if (!isDragging && abs(dx) > abs(dy) && dx < 0) {
                            isDragging = true
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }

                        if (isDragging) {
                            // Returning true here cancels children and routes
                            // subsequent events to our onTouchEvent
                            param.result = true
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isDragging = false
                    }
                }
            }

        View::class.asResolver().
            firstMethod {
                name = "onTouchEvent"
                superclass()
            }
            .hookAfter { param ->
                val v = param.thisObject as View
                if (v !== messageView) return@hookAfter

                val event = param.args[0] as MotionEvent

                when (event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        if (isDragging) {
                            val dx = (event.x - startX).coerceIn(-triggerThreshold, 0f)
                            v.translationX = dx

                            if (!triggered && (event.x - startX) < -triggerThreshold) {
                                triggered = true
                                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }

                            param.result = true
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            v.animate()
                                .translationX(0f)
                                .setDuration(250)
                                .setInterpolator(SpringInterpolator())
                                .start()

                            if (triggered) onSwipeLeft(chattingContext, msgInfo)

                            v.parent?.requestDisallowInterceptTouchEvent(false)
                            isDragging = false
                            param.result = true
                        }
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            v.animate()
                                .translationX(0f)
                                .setDuration(150)
                                .start()

                            v.parent?.requestDisallowInterceptTouchEvent(false)
                            isDragging = false
                        }
                    }
                }
        }
    }

    // Poor man's spring: decelerates then slightly overshoots back to 0
    private class SpringInterpolator : Interpolator {
        override fun getInterpolation(t: Float): Float {
            // Damped sine approximation
            return (1f - (cos(t * PI * 2.5) * exp(-t * 5f))).toFloat()
        }
    }

    private fun onSwipeLeft(chattingContext: Any, msgInfo: MessageInfo) {
        val apiMan = chattingContext.asResolver()
            .firstField { type = WeServiceApi.methodApiManagerGetApi.method.declaringClass }
            .get()!!
        val api = WeServiceApi.getApiByClass(apiMan, classChattingUiFootComponent.clazz)
        val chatFooter = api.asResolver()
            .firstField { type = "com.tencent.mm.pluginsdk.ui.chat.ChatFooter" }
            .get()!! as FrameLayout
        val quoteMethod = chatFooter.asResolver()
            .firstMethod {
                parameters { params ->
                    params[0] == WeMessageApi.classMsgInfo.clazz
                }
                returnType = Boolean::class
            }.self
        if (quoteMethod.parameterCount == 1) {
            quoteMethod.invoke(chatFooter, msgInfo.instance)
        }
        else {
            quoteMethod.invoke(chatFooter, msgInfo.instance, null)
        }
    }

    private fun dpToPx(context: Context, dp: Int) =
        (dp * context.resources.displayMetrics.density).toInt()

    private val classChattingUiFootComponent by dexClass()

    override fun dexFind(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        classChattingUiFootComponent.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.ui.chatting.component")
            matcher {
                usingEqStrings("MicroMsg.ChattingUI.FootComponent", "onNotifyChange event %s talker %s")
            }
        }

        return descriptors
    }
}
