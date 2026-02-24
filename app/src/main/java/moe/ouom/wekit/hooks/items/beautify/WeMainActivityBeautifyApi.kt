package moe.ouom.wekit.hooks.items.beautify

import moe.ouom.wekit.core.dsl.dexMethod
import moe.ouom.wekit.core.model.ApiHookItem
import moe.ouom.wekit.dexkit.intf.IDexFind
import moe.ouom.wekit.hooks.core.annotation.HookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "API/微信主屏幕美化服务", desc = "为其他功能提供美化微信主屏幕的能力")
object WeMainActivityBeautifyApi : ApiHookItem(), IDexFind {

    val methodDoOnCreate by dexMethod()

    override fun dexFind(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        methodDoOnCreate.find(dexKit, descriptors) {
            matcher {
                declaredClass = "com.tencent.mm.ui.MainTabUI"
                usingEqStrings("MicroMsg.LauncherUI.MainTabUI", "doOnCreate")
            }
        }

        return descriptors
    }
}