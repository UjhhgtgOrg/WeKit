package moe.ouom.wekit.hooks.items.scripting_kts

abstract class WeKitPlugin {
    lateinit var classLoader: ClassLoader

    abstract fun onLoad()
}