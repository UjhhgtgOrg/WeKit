package moe.ouom.wekit.hooks._core

import moe.ouom.wekit.config.ConfigManager
import moe.ouom.wekit.constants.Constants.Companion.PrekClickableXXX
import moe.ouom.wekit.constants.Constants.Companion.PrekXXX
import moe.ouom.wekit.hooks._base.ApiHookItem
import moe.ouom.wekit.hooks._base.BaseClickableFunctionHookItem
import moe.ouom.wekit.hooks._base.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks._core.factory.HookItemFactory
import moe.ouom.wekit.util.log.Logger


class HookItemLoader {
    /**
     * 加载并判断哪些需要加载
     */
    fun loadHookItem(process: Int) {
        val allHookItems = HookItemFactory.getAllItemList()
        allHookItems.forEach { hookItem ->
            val path = hookItem.path
            if (hookItem is BaseSwitchFunctionHookItem) {
                hookItem.isEnabled = ConfigManager.getDefaultConfig().getBooleanOrFalse("$PrekXXX${hookItem.path}")
                if (hookItem.isEnabled && process == hookItem.targetProcess) {
                    Logger.i("[BaseSwitchFunctionHookItem] Initializing $path...")
                    hookItem.startLoad()
                }
            }
            else if (hookItem is BaseClickableFunctionHookItem) {
                hookItem.isEnabled = ConfigManager.getDefaultConfig().getBooleanOrFalse("$PrekClickableXXX${hookItem.path}")
                if (hookItem.isEnabled && process == hookItem.targetProcess) {
                    Logger.i("[BaseClickableFunctionHookItem] Initializing $path...")
                    hookItem.startLoad()
                }

                if (hookItem.alwaysRun) {
                    Logger.i("[BaseClickableFunctionHookItem-AlwaysRun] Initializing $path...")
                    hookItem.startLoad()
                }
            }
            else {
                if (hookItem is ApiHookItem && process == hookItem.targetProcess){
                    Logger.i("[API] Initializing $path...")
                    hookItem.startLoad()
                }
            }


        }
    }

}