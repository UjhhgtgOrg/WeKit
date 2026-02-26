package moe.ouom.wekit.hooks.items.scripting_kts

import moe.ouom.wekit.constants.PackageConstants
import moe.ouom.wekit.host.HostInfo
import moe.ouom.wekit.utils.log.WeLogger
import java.io.File
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

object KtsEngine {

    private const val TAG = "KtsEngine"
    private val host = BasicJvmScriptingHost()
    private lateinit var classLoader: ClassLoader
    private lateinit var wechatApkPath: String
    private lateinit var moduleApkPath: String
    private lateinit var classPaths: List<File>

    fun init(classLoader: ClassLoader) {
        this.classLoader = classLoader
        wechatApkPath = HostInfo.getApplication().applicationInfo.sourceDir
        moduleApkPath = HostInfo.getApplication().packageManager.getPackageInfo(
            PackageConstants.PACKAGE_NAME_SELF, 0).applicationInfo!!.sourceDir
        WeLogger.d(TAG, "wechat apk path: $wechatApkPath")
        WeLogger.d(TAG, "module apk path: $moduleApkPath")
        classPaths = listOf(File(wechatApkPath), File(moduleApkPath))
    }

    fun loadScript(code: String): KtsScript? {
        System.setProperty("kotlin.script.classpath", "$moduleApkPath:$wechatApkPath")

        val compConfig = createJvmCompilationConfigurationFromTemplate<Any> {
            updateClasspath(classPaths)

            jvm {
                updateClasspath(classPaths)
                dependenciesFromCurrentContext(wholeClasspath = true)
                dependenciesFromClassloader(
                    "kotlin-stdlib",
                    "kotlin-reflect",
                    "kotlin-script-runtime",
                    classLoader = classLoader, wholeClasspath = true)
            }
        }

        val evalConf = createJvmEvaluationConfigurationFromTemplate<Any> {
            jvm {
                baseClassLoader(classLoader)
            }
        }

        val result = host.eval(code.toScriptSource(), compConfig, evalConf)
        val evalResult = result.valueOrThrow().returnValue

        if (evalResult is ResultValue.Value) {
            WeLogger.i(TAG, "successfully loaded a script")
            return evalResult.value as? KtsScript
        }

        WeLogger.e(TAG, "failed to load a script")
        return null
    }
}
