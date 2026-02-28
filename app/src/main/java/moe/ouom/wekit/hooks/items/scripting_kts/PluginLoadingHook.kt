package moe.ouom.wekit.hooks.items.scripting_kts

import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.utils.io.PathUtils
import kotlin.io.path.div

@HookItem(path = "脚本/脚本引擎 (Kotlin Scripting)", desc = "测试")
object PluginLoadingHook : BaseSwitchFunctionHookItem() {

    private lateinit var pluginLoader: PluginLoader

    override fun entry(classLoader: ClassLoader) {
        pluginLoader = PluginLoader(this::class.java.classLoader!!)
        pluginLoader.loadPlugins(PathUtils.moduleDataPath!!/"scripts")
    }
}