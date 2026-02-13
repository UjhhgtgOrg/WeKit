package moe.ouom.wekit.util.script

import moe.ouom.wekit.util.log.WeLogger
import org.json.JSONObject

/**
 * JavaScript脚本执行管理器
 * 用于执行脚本的onRequest和onResponse方法
 */
class ScriptEvalManager private constructor() {

    companion object {
        @Volatile
        private var instance: ScriptEvalManager? = null

        @JvmStatic
        fun getInstance(): ScriptEvalManager {
            return instance ?: synchronized(this) {
                instance ?: ScriptEvalManager().also { instance = it }
            }
        }
    }

    private lateinit var jsExecutor: JsExecutor
    private lateinit var scriptFileManager: ScriptFileManager
    private var isInitialized = false

    /**
     * 初始化脚本执行管理器
     */
    fun initialize(scriptFileManager: ScriptFileManager) {
        if (isInitialized) {
            WeLogger.w("[ScriptEvalManager] 已经初始化过")
            return
        }

        try {
            // 初始化JsExecutor
            jsExecutor = JsExecutor.getInstance()
            // 设置ScriptFileManager
            this@ScriptEvalManager.scriptFileManager = scriptFileManager
            isInitialized = true
            WeLogger.i("[ScriptEvalManager] 初始化成功")
        } catch (e: Exception) {
            WeLogger.e("[ScriptEvalManager] 初始化失败", e)
            throw IllegalStateException("ScriptEvalManager 初始化失败", e)
        }
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean {
        return isInitialized
    }

    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("ScriptEvalManager 未初始化，请先调用 initialize() 方法")
        }
    }

    /**
     * 检查脚本是否定义了指定方法
     */
    fun hasMethod(scriptContent: String, methodName: String): Boolean {
        checkInitialized()

        if (!jsExecutor.isInitialized()) {
            WeLogger.w("[ScriptEvalManager] JsExecutor 未就绪")
            return false
        }

        val jsCode = """
            (function() {
                try {
                    // 定义脚本内容
                    $scriptContent
                    
                    // 检查方法是否存在且是函数类型
                    return typeof $methodName === 'function';
                } catch (error) {
                    return false;
                }
            })();
        """.trimIndent()

        return try {
            val result = jsExecutor.executeJs(jsCode)
            result?.toBooleanStrictOrNull() ?: false
        } catch (e: Exception) {
            WeLogger.e("[ScriptEvalManager] 检查方法 $methodName 失败", e)
            false
        }
    }

    /**
     * 测试脚本方法定义
     */
    fun testScriptMethods(scriptContent: String): ScriptMethodTestResult {
        checkInitialized()

        val hasOnRequest = hasMethod(scriptContent, "onRequest")
        val hasOnResponse = hasMethod(scriptContent, "onResponse")

        // 额外检查语法错误
        val syntaxErrors = checkSyntaxError(scriptContent)

        return ScriptMethodTestResult(
            hasOnRequest = hasOnRequest,
            hasOnResponse = hasOnResponse,
            errorMessages = syntaxErrors
        )
    }

    /**
     * 检查脚本语法错误
     */
    private fun checkSyntaxError(scriptContent: String): List<String> {
        checkInitialized()

        val jsCode = """
        (function() {
            try {
                // 尝试解析脚本内容
                eval($scriptContent);
                return [];
            } catch (error) {
                return [error.message];
            }
        })();
    """.trimIndent()

        return try {
            val result = jsExecutor.executeJs(jsCode)
            if (result.isNullOrEmpty() || result == "[]") {
                emptyList()
            } else {
                listOf("语法错误: $result")
            }
        } catch (e: Exception) {
            WeLogger.e("[ScriptEvalManager] 语法检查失败", e)
            listOf("语法检查异常: ${e.message}")
        }
    }

    /**
     * 执行所有启用脚本的onRequest方法
     */
    fun executeOnRequest(uri: String, cgiId: Int, requestJson: JSONObject): JSONObject? {
        checkInitialized()
        return executeAllScripts("onRequest", uri, cgiId, requestJson)
    }

    /**
     * 执行所有启用脚本的onResponse方法
     */
    fun executeOnResponse(uri: String, cgiId: Int, responseJson: JSONObject): JSONObject? {
        checkInitialized()
        return executeAllScripts("onResponse", uri, cgiId, responseJson)
    }

    /**
     * 执行所有脚本的指定方法
     */
    private fun executeAllScripts(methodName: String, uri: String, cgiId: Int, jsonData: JSONObject): JSONObject? {
        checkInitialized()

        val enabledScripts = scriptFileManager.getEnabledScripts()
        if (enabledScripts.isEmpty()) return null

        var currentData = jsonData
        var modified = false

        enabledScripts.sortedBy { it.order }.forEach { script ->
            if (hasMethod(script.content, methodName)) {
                val result = executeScriptMethod(script, methodName, uri, cgiId, currentData)
                if (result != null) {
                    try {
                        currentData = JSONObject(result)
                        modified = true
                        WeLogger.d("[ScriptEvalManager] 脚本 ${script.name}.$methodName 执行成功")
                    } catch (e: Exception) {
                        WeLogger.e("[ScriptEvalManager] 解析脚本 ${script.name}.$methodName 结果失败", e)
                    }
                }
            }
        }

        return if (modified) currentData else null
    }

    /**
     * 执行单个脚本的方法
     */
    private fun executeScriptMethod(
        script: ScriptFileManager.ScriptConfig,
        methodName: String,
        uri: String,
        cgiId: Int,
        jsonData: JSONObject
    ): String? {
        val scriptName = script.name
        val scriptContent = script.content

        val jsCode = """
            (function() {
                try {
                    // 执行脚本内容
                    $scriptContent
                    
                    // 创建包含所有参数的对象
                    const data = {
                        uri: '$uri',
                        cgiId: $cgiId,
                        jsonData: $jsonData
                    };
                    
                    // 调用指定方法
                    const result = $methodName(data);
                    
                    // 返回结果
                    if (result === undefined || result === null) {
                        return null;
                    }
                    
                    if (typeof result === 'object') {
                        return JSON.stringify(result);
                    } else if (typeof result === 'string') {
                        try {
                            JSON.parse(result);
                            return result;
                        } catch(e) {
                            return JSON.stringify({value: result});
                        }
                    } else {
                        return JSON.stringify({value: result});
                    }
                } catch(error) {
                    wekit.log('[Script:${scriptName} Error] ' + error.message);
                    return null;
                }
            })();
        """.trimIndent()

        return try {
            ScriptLogger.getInstance().setScriptName(scriptName)
            jsExecutor.executeJs(jsCode)
        } catch (e: Exception) {
            WeLogger.e("[ScriptEvalManager] 执行脚本 ${scriptName}.$methodName 失败", e)
            null
        } finally {
            ScriptLogger.getInstance().resetScriptName()
        }
    }

    /**
     * 测试执行指定的JavaScript代码片段
     */
    fun testExecuteCode(scriptContent: String, codeSnippet: String, scriptName: String = "测试脚本"): String? {
        checkInitialized()

        val jsCode = """
        (function() {
            try {
                // 执行脚本内容
                $scriptContent
                // 执行用户输入的代码
                const result = eval('$codeSnippet');
                // 返回结果
                return JSON.stringify(result);
            } catch(error) {
                return JSON.stringify({error: error.message});
            }
        })();
    """.trimIndent()

        return try {
            ScriptLogger.getInstance().setScriptName(scriptName)
            val result = jsExecutor.executeJs(jsCode)
            result
        } catch (e: Exception) {
            WeLogger.e("[ScriptEvalManager] 测试执行失败", e)
            null
        } finally {
            ScriptLogger.getInstance().resetScriptName()
        }
    }

    data class ScriptMethodTestResult(
        val hasOnRequest: Boolean,
        val hasOnResponse: Boolean,
        val errorMessages: List<String>
    ) {
        fun isPassed(): Boolean {
            return hasOnRequest || hasOnResponse
        }

        fun getSummary(): String {
            return buildString {
                append("方法检查结果:\n")
                append("• onRequest: ${if (hasOnRequest) "✓ 存在" else "✗ 不存在"}\n")
                append("• onResponse: ${if (hasOnResponse) "✓ 存在" else "✗ 不存在"}")

                if (errorMessages.isNotEmpty()) {
                    append("\n\n错误:\n")
                    errorMessages.forEach { append("• $it\n") }
                }
            }
        }
    }

}