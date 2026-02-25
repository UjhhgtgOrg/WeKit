package moe.ouom.wekit.hooks.items.enhance

import android.view.WindowManager
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.toClass
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem

@HookItem(path = "系统与隐私/禁止屏幕高亮度", desc = "禁止应用将屏幕亮度设置得过高")
object DisableHighBrightness : BaseSwitchFunctionHookItem() {

    override fun entry(classLoader: ClassLoader) {
        "com.android.internal.policy.PhoneWindow".toClass(classLoader).asResolver()
            .firstMethod {
                name = "setAttributes"
                parameters(WindowManager.LayoutParams::class)
            }
            .hookBefore { param ->
                val lp = param.args[0] as WindowManager.LayoutParams
                if (lp.screenBrightness >= 0.5f) {
                    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
    }
}