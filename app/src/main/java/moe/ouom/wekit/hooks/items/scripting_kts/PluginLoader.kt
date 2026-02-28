package moe.ouom.wekit.hooks.items.scripting_kts

import moe.ouom.wekit.utils.log.WeLogger
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClassloader
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

class PluginLoader(private val classLoader: ClassLoader) {

    private val host = BasicJvmScriptingHost()

    fun loadPlugins(pluginDir: Path) {
        if (!pluginDir.exists()) pluginDir.createDirectories()

        pluginDir.listDirectoryEntries("*.wekit.kts")
            .forEach { file ->
                runCatching { loadScript(file) }
                    .onFailure { WeLogger.e("PluginLoader", "Failed to load ${file.name}", it) }
            }
    }

    private fun loadScript(file: Path) {
        WeLogger.i("PluginLoader", "loading ${file.name}")

        val currentThread = Thread.currentThread()
        val originalClassLoader = currentThread.contextClassLoader

        try {
            currentThread.contextClassLoader = classLoader

            val compilationConfig = createJvmCompilationConfigurationFromTemplate<WeKitScript> {
                updateClasspath(getAndroidClasspath())
                updateClasspath(classpathFromClassloader(classLoader))
            }

            val result = host.eval(file.toFile().toScriptSource(), compilationConfig, null)

            result.valueOrThrow().returnValue.scriptInstance
                ?.let { instance ->
                    val plugin = instance as WeKitPlugin
                    plugin.classLoader = classLoader
                    plugin.onLoad()
                    WeLogger.i("PluginLoader", "Loaded plugin: ${file.name}")
                }

        } finally {
            currentThread.contextClassLoader = originalClassLoader
        }
    }
}