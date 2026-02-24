package moe.ouom.wekit.hooks.items.enhance

import android.os.Process
import de.robv.android.xposed.XC_MethodHook
import moe.ouom.wekit.R
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.ui.WeHomeScreenPopupMenuApi

@HookItem(path = "优化与修复/快捷强制停止", desc = "向主屏幕右上角菜单添加 '强制停止' 菜单项")
object QuickForceStop : BaseSwitchFunctionHookItem(), WeHomeScreenPopupMenuApi.IMenuItemsProvider {

    override fun entry(classLoader: ClassLoader) {
        WeHomeScreenPopupMenuApi.addProvider(this)
    }

    override fun unload(classLoader: ClassLoader) {
        WeHomeScreenPopupMenuApi.removeProvider(this)
        super.unload(classLoader)
    }

    override fun getMenuItems(hookParam: XC_MethodHook.MethodHookParam): List<WeHomeScreenPopupMenuApi.MenuItem> {
        return listOf(WeHomeScreenPopupMenuApi.MenuItem(777001, "强制停止", R.drawable.block_24px, {
            Process.killProcess(Process.myPid())
        }))
    }
}