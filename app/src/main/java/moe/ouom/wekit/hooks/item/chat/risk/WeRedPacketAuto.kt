package moe.ouom.wekit.hooks.item.chat.risk

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import moe.ouom.wekit.constants.Constants.Companion.CLAZZ_ICON_PREFERENCE
import moe.ouom.wekit.constants.Constants.Companion.CLAZZ_I_PREFERENCE_SCREEN
import moe.ouom.wekit.constants.Constants.Companion.CLAZZ_PREFERENCE
import moe.ouom.wekit.constants.Constants.Companion.CLAZZ_SETTINGS_UI
import moe.ouom.wekit.dexkit.TargetManager
import moe.ouom.wekit.hooks._base.BaseClickableFunctionHookItem
import moe.ouom.wekit.hooks._core.annotation.HookItem
import moe.ouom.wekit.ui.creator.dialog.item.WeRedPacketConfigDialog
import moe.ouom.wekit.util.log.Logger

@SuppressLint("DiscouragedApi")
@HookItem(path = "聊天与消息/自动抢红包", desc = "点击配置抢红包参数")
class WeRedPacketAuto : BaseClickableFunctionHookItem() {
    override fun entry(classLoader: ClassLoader) {

    }

    override fun unload(classLoader: ClassLoader) {}

    override fun onClick(context: Context?) {
        super.onClick(context)
        val dialog = context?.let { WeRedPacketConfigDialog(it) }
        dialog?.show()
    }
}