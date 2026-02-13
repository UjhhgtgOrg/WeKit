package moe.ouom.wekit.util.script

import moe.ouom.wekit.util.log.WeLogger
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 脚本日志记录器
 * 专门处理JavaScript脚本的日志记录
 */
class ScriptLogger {

    companion object {
        @Volatile
        private var instance: ScriptLogger? = null

        @JvmStatic
        fun getInstance(): ScriptLogger {
            return instance ?: synchronized(this) {
                instance ?: ScriptLogger().also { instance = it }
            }
        }
    }

    private val defaultScriptName = "未知"
    private var scriptName: String = defaultScriptName
    // 日志条目
    data class LogEntry(
        val id: String = UUID.randomUUID().toString(),
        val timestamp: Long = System.currentTimeMillis(),
        val level: String, // INFO, WARN, ERROR
        val message: String,
        val entryScriptName: String
    )

    // 配置
    data class LoggerConfig(
        var maxEntries: Int = 1000,
        var autoPrune: Boolean = true,
        var enableConsoleOutput: Boolean = true,
        var logLevels: Set<String> = setOf("INFO", "WARN", "ERROR")
    )

    private val logEntries = CopyOnWriteArrayList<LogEntry>()
    private var config = LoggerConfig()
    private var isInitialized = false

    fun isInitialized(): Boolean = isInitialized

    fun initialize() {
        if (!isInitialized) {
            isInitialized = true
            WeLogger.i("ScriptLogger", "Script logger initialized")
        }
    }

    /**
     * 设置脚本名称
     */
    fun setScriptName(scriptName: String) {
        this.scriptName = scriptName
    }

    /**
     * 获取脚本名称
     */
    fun getScriptName(): String {
        return this.scriptName
    }

    /**
     * 恢复默认脚本名称
     */
    fun resetScriptName() {
        this.scriptName = defaultScriptName
    }

    /**
     * 添加日志
     */
    private fun addLogInternal(entry: LogEntry) {
        if (!config.logLevels.contains(entry.level)) {
            return
        }

        // 添加到列表开头（最新的在前面）
        logEntries.add(0, entry)

        // 自动清理
        if (config.autoPrune && logEntries.size > config.maxEntries) {
            logEntries.removeAt(logEntries.size - 1)
        }

        // 输出到系统日志
        if (config.enableConsoleOutput) {
            when (entry.level) {
                "ERROR" -> WeLogger.e("[Script:${entry.entryScriptName}] ${entry.message}")
                "WARN" -> WeLogger.w("[Script:${entry.entryScriptName}] ${entry.message}")
                else -> WeLogger.i("[Script:${entry.entryScriptName}] ${entry.message}")
            }
        }
    }

    // 公共日志方法

    fun info(message: String) {
        if (!isInitialized) initialize()
        addLogInternal(LogEntry(level = "INFO", message = message, entryScriptName = scriptName))
    }

    fun warn(message: String) {
        if (!isInitialized) initialize()
        addLogInternal(LogEntry(level = "WARN", message = message, entryScriptName = scriptName))
    }

    fun error(message: String) {
        if (!isInitialized) initialize()
        addLogInternal(LogEntry(level = "ERROR", message = message, entryScriptName = scriptName))
    }

    // 查询方法

    fun getAllLogs(): List<LogEntry> = logEntries.toList()

    fun getLogsByScript(scriptName: String): List<LogEntry> {
        return logEntries.filter { it.entryScriptName == scriptName }
    }

    fun getLogsByLevel(level: String): List<LogEntry> {
        return logEntries.filter { it.level == level }
    }

    fun clearAll() {
        logEntries.clear()
        WeLogger.i("ScriptLogger", "All logs cleared")
    }

}