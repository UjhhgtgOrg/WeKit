package moe.ouom.wekit.hooks.sdk.protocol.listener

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import moe.ouom.wekit.core.model.ApiHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.util.FunProtoData
import moe.ouom.wekit.util.log.WeLogger
import java.lang.reflect.Modifier

@HookItem(path = "protocol/微信数据包监听", desc = "NetScene 监控与配置生成")
class WePkgListener : ApiHookItem() {

    companion object {
        private var DEBUG = false

    }
    override fun entry(classLoader: ClassLoader) {
        if (DEBUG) {
            hookBuilder(classLoader) // debug use only
        }
        hookDispatch(classLoader)
    }

    private fun hookBuilder(classLoader: ClassLoader) {
        val builderClass = "com.tencent.mm.modelbase.l"

        try {
            XposedHelpers.findAndHookMethod(builderClass, classLoader, "a", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val builder = param.thisObject

                    val cgiId = try { XposedHelpers.getIntField(builder, "d") } catch (e: Throwable) { 0 }
                    val funcId = try { XposedHelpers.getIntField(builder, "e") } catch (e: Throwable) { 0 }
                    val routeId = try { XposedHelpers.getIntField(builder, "f") } catch (e: Throwable) { 0 }
                    val uri = try { XposedHelpers.getObjectField(builder, "c") as? String ?: "" } catch (e: Throwable) { "" }

                    // 获取 Request 对象的类名
                    var reqClassName = "Unknown"
                    try {
                        val reqObj = XposedHelpers.getObjectField(builder, "a")
                        if (reqObj != null) {
                            reqClassName = reqObj.javaClass.name
                        }
                    } catch (_: Throwable) { }

                    val configLog = "$cgiId to Triple(\"$reqClassName\", $funcId, $routeId), // $uri"
                    WeLogger.e("WePkgListener-gen", configLog)

//                    WeLogger.printStackTrace()
                }
            })
        } catch (e: Throwable) {
            WeLogger.e("WePkgListener-gen", "Builder Hook 失败: ${e.message}")
        }
    }

    private fun hookDispatch(classLoader: ClassLoader) {
        val netSceneBaseClass = "com.tencent.mm.modelbase.m1"
        val pbBaseClass = "com.tencent.mm.protobuf.f"

        XposedHelpers.findAndHookMethod(
            netSceneBaseClass,
            classLoader,
            "dispatch",
            "com.tencent.mm.network.s",
            "com.tencent.mm.network.v0",
            "com.tencent.mm.network.l0",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val rrObj = param.args[1] ?: return
                        val uri = XposedHelpers.callMethod(rrObj, "getUri") as? String ?: "null"
                        val cgiId = XposedHelpers.callMethod(rrObj, "getType") as? Int ?: 0

                        if (isIgnoredCgi(uri, cgiId)) return

                        val realReqPb = findPbObjectSafe(rrObj, pbBaseClass, 0, 3)

                        if (realReqPb != null) {
                            if (DEBUG) WeLogger.i("WePkgListener", ">>> [捕获包体] $uri ($cgiId)")
                            try {
                                val pbBytes = XposedHelpers.callMethod(realReqPb, "toByteArray") as? ByteArray
                                if (pbBytes != null && pbBytes.isNotEmpty()) {
                                    val data = FunProtoData()
                                    data.fromBytes(pbBytes)
                                    if (DEBUG) WeLogger.d("WePkgListener", "JSON: ${data.toJSON()}")
                                }
                            } catch (_: Exception) { }
                            if (DEBUG) WeLogger.printStackTrace()
                        }
                    } catch (t: Throwable) {
                        if (DEBUG) WeLogger.e("WePkgListener", "Dispatch 扫描异常: ${t.message}")
                    }
                }
            }
        )
    }

    private fun findPbObjectSafe(instance: Any?, targetClassStr: String, currentDepth: Int, maxDepth: Int): Any? {
        if (instance == null || currentDepth > maxDepth) return null
        val clazz = instance.javaClass
        if (clazz.name.startsWith("java.") || clazz.name.startsWith("android.")) return null
        if (isInstanceOf(clazz, targetClassStr)) return instance

        try {
            var currentClass: Class<*>? = clazz
            while (currentClass != null && currentClass.name != "java.lang.Object") {
                for (field in currentClass.declaredFields) {
                    if (Modifier.isStatic(field.modifiers)) continue
                    field.isAccessible = true
                    val value = field.get(instance) ?: continue
                    val found = findPbObjectSafe(value, targetClassStr, currentDepth + 1, maxDepth)
                    if (found != null) return found
                }
                currentClass = currentClass.superclass
            }
        } catch (e: Exception) { }
        return null
    }

    private fun isInstanceOf(clazz: Class<*>, targetName: String): Boolean {
        var current: Class<*>? = clazz
        while (current != null) {
            if (current.name == targetName) return true
            current = current.superclass
        }
        return false
    }

    private fun isIgnoredCgi(uri: String, id: Int): Boolean {
        return false
//        return uri.contains("report") || uri.contains("log") || id == 381 || id == 988 || id == 2723
    }
}