package moe.ouom.wekit.util.script

import android.content.Context
import moe.ouom.wekit.util.log.WeLogger
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * 脚本文件管理器
 */
class ScriptFileManager private constructor() {

    companion object {
        private const val SCRIPT_DIR = "wekit_scripts"
        private const val SCRIPT_SUFFIX = ".json"

        @Volatile
        private var INSTANCE: ScriptFileManager? = null

        @JvmStatic
        fun getInstance(): ScriptFileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScriptFileManager().also { INSTANCE = it }
            }
        }
    }

    data class ScriptConfig(
        val id: String,
        var name: String,
        var content: String,
        var enabled: Boolean = true,
        var order: Int = 0,
        var createdTime: Long = System.currentTimeMillis(),
        var modifiedTime: Long = System.currentTimeMillis(),
        var description: String = ""
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", id)
                put("name", name)
                put("content", content)
                put("enabled", enabled)
                put("order", order)
                put("createdTime", createdTime)
                put("modifiedTime", modifiedTime)
                put("description", description)
            }
        }

        companion object {
            fun fromJson(json: JSONObject): ScriptConfig {
                return ScriptConfig(
                    id = json.optString("id", ""),
                    name = json.optString("name", "未命名脚本"),
                    content = json.optString("content", ""),
                    enabled = json.optBoolean("enabled", true),
                    order = json.optInt("order", 0),
                    createdTime = json.optLong("createdTime", System.currentTimeMillis()),
                    modifiedTime = json.optLong("modifiedTime", System.currentTimeMillis()),
                    description = json.optString("description", "")
                )
            }
        }
    }

    private lateinit var scriptDir: File
    private var isInitialized = false


    /**
     * 初始化脚本文件管理器
     * @param applicationContext Application级别的Context
     */
    fun initialize(applicationContext: Context) {
        if (isInitialized) {
            WeLogger.w("[ScriptFileManager] 已经初始化过")
            return
        }

        try {
            scriptDir = File(applicationContext.getFilesDir().parentFile, SCRIPT_DIR)
            ensureScriptDirExists()
            isInitialized = true
            WeLogger.i("[ScriptFileManager] 初始化成功: ${scriptDir.absolutePath}")
        } catch (e: Exception) {
            WeLogger.e("[ScriptFileManager] 初始化失败", e)
            throw IllegalStateException("ScriptFileManager 初始化失败", e)
        }
    }


    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean {
        return isInitialized
    }

    private fun ensureScriptDirExists() {
        if (!scriptDir.exists()) {
            if (scriptDir.mkdirs()) {
                WeLogger.i("[ScriptFileManager] 脚本目录创建成功: ${scriptDir.absolutePath}")
            }
        }
    }

    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("ScriptFileManager 未初始化，请先调用 initialize() 方法")
        }
    }

    /**
     * 保存脚本
     */
    fun saveScript(script: ScriptConfig): Boolean {
        checkInitialized()

        return try {
            ensureScriptDirExists()

            val scriptFile = File(scriptDir, "${script.id}$SCRIPT_SUFFIX")
            val json = script.toJson()

            FileWriter(scriptFile).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }

            WeLogger.d("[ScriptFileManager] 脚本已保存: ${script.name}")
            true
        } catch (e: IOException) {
            WeLogger.e("[ScriptFileManager] 保存脚本失败", e)
            false
        }
    }

    /**
     * 获取所有脚本
     */
    fun getAllScripts(): List<ScriptConfig> {
        checkInitialized()

        ensureScriptDirExists()

        val scripts = mutableListOf<ScriptConfig>()

        scriptDir.listFiles { _, name ->
            name.endsWith(SCRIPT_SUFFIX)
        }?.forEach { file ->
            try {
                val jsonString = file.readText()
                val json = JSONObject(jsonString)
                scripts.add(ScriptConfig.fromJson(json))
            } catch (e: Exception) {
                WeLogger.e("[ScriptFileManager] 读取脚本文件失败: ${file.name}", e)
            }
        }

        return scripts.sortedBy { it.order }
    }

    /**
     * 根据ID获取脚本
     */
    fun getScriptById(id: String): ScriptConfig? {
        checkInitialized()

        val scriptFile = File(scriptDir, "$id$SCRIPT_SUFFIX")
        if (!scriptFile.exists()) return null

        return try {
            val jsonString = scriptFile.readText()
            val json = JSONObject(jsonString)
            ScriptConfig.fromJson(json)
        } catch (e: Exception) {
            WeLogger.e("[ScriptFileManager] 读取脚本失败: $id", e)
            null
        }
    }

    /**
     * 删除脚本
     */
    fun deleteScript(id: String): Boolean {
        checkInitialized()

        val scriptFile = File(scriptDir, "$id$SCRIPT_SUFFIX")
        return if (scriptFile.exists()) {
            val success = scriptFile.delete()
            if (success) {
                WeLogger.i("[ScriptFileManager] 脚本已删除: $id")
            }
            success
        } else {
            false
        }
    }

    /**
     * 删除所有脚本
     */
    fun deleteAllScripts(): Int {
        checkInitialized()

        var count = 0
        scriptDir.listFiles { _, name ->
            name.endsWith(SCRIPT_SUFFIX)
        }?.forEach { file ->
            if (file.delete()) {
                count++
            }
        }

        WeLogger.i("[ScriptFileManager] 已删除 $count 个脚本")
        return count
    }

    /**
     * 获取启用状态的脚本
     */
    fun getEnabledScripts(): List<ScriptConfig> {
        checkInitialized()

        return getAllScripts().filter { it.enabled }
    }

    /**
     * 获取脚本数量
     */
    fun getScriptCount(): Int {
        checkInitialized()

        return getAllScripts().size
    }

    /**
     * 获取启用脚本数量
     */
    fun getEnabledScriptCount(): Int {
        checkInitialized()

        return getEnabledScripts().size
    }
}