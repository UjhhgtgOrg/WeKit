package moe.ouom.wekit.hooks.items.enhance

import android.content.Context
import moe.ouom.wekit.core.model.BaseClickableFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem

@HookItem(path = "系统与隐私/清理缓存垃圾", desc = "手动或自动清理应用的缓存 (没写完)")
object AutoCleanCache : BaseClickableFunctionHookItem() {

    private const val TAG = "AutoCleanCache"

    override fun onClick(context: Context) {

    }
}