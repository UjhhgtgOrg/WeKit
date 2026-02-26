package moe.ouom.wekit.hooks.items.scripting_kts

import android.content.ContentValues
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.base.WeDatabaseListenerApi
import moe.ouom.wekit.utils.io.PathUtils
import moe.ouom.wekit.utils.log.WeLogger
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

@HookItem(path = "脚本/脚本引擎 (测试)", desc = "提供运行 .kts 的能力 (没写完)")
object KtsScriptingHook : BaseSwitchFunctionHookItem(),
    WeDatabaseListenerApi.IInsertListener {

    private const val TAG = "ScriptingHook"
    val scripts: MutableList<KtsScript> = mutableListOf()
    private val scriptsDir = PathUtils.moduleDataPath!! / "scripts"

    override fun entry(classLoader: ClassLoader) {
        KtsEngine.init(classLoader)

        if (!scriptsDir.isDirectory())
            scriptsDir.createDirectories()

        for (scriptFile in scriptsDir.listDirectoryEntries("*.kts")) {
            WeLogger.i(TAG, "loading script from $scriptFile")
            val script = KtsEngine.loadScript(scriptFile.readText()) ?: continue
            scripts.add(script)
        }

        WeLogger.i(TAG, "loaded ${scripts.size} scripts in total")
    }

    override fun unload(classLoader: ClassLoader) {
        WeLogger.i(TAG, "unloading ${scripts.size} scripts")
        scripts.clear()
        super.unload(classLoader)
    }

    override fun onInsert(table: String, values: ContentValues) {
        if (!isEnabled) return
        if (table != "message") return

        for (script in scripts) {
            try {
                script.onMessage(values)
            }
            catch (ex: Exception) {
                WeLogger.e(TAG, "a script threw while executing onMessage", ex)
            }
        }
    }
}