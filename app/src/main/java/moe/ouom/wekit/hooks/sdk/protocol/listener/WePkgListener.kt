package moe.ouom.wekit.hooks.sdk.protocol.listener

import de.robv.android.xposed.XposedHelpers
import moe.ouom.wekit.core.model.ApiHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.protocol.WePkgManager
import moe.ouom.wekit.util.log.WeLogger

@HookItem(path = "protocol/统一数据包分发器", desc = "全量 PB 对象监控")
class WePkgDispatcher : ApiHookItem() {

    override fun entry(classLoader: ClassLoader) {
        val netSceneBaseClass = XposedHelpers.findClass("com.tencent.mm.modelbase.m1", classLoader)
        val callbackClass = XposedHelpers.findClass("com.tencent.mm.network.l0", classLoader)

        hookBefore(netSceneBaseClass, "dispatch") { param ->
            val rrObj = param.args[1] ?: return@hookBefore
            val callback = param.args[2] ?: return@hookBefore

            val uri = XposedHelpers.callMethod(rrObj, "getUri") as? String ?: return@hookBefore
            val cgiId = XposedHelpers.callMethod(rrObj, "getType") as? Int ?: 0

            try {
                val reqObj = extractPbObject(rrObj, isRequest = true)
                if (reqObj != null) {
                    WePkgManager.notifyRequest(uri, cgiId, reqObj as ByteArray)
                }
            } catch (e: Exception) {
                WeLogger.e("PkgDispatcher", "Request interception failed for $uri", e)
            }

            param.args[2] = createCallbackProxy(callback, callbackClass, rrObj, uri, cgiId)
        }
    }

    /**
     * 创建回调代理，用于拦截服务器回包
     */
    private fun createCallbackProxy(
        originalCallback: Any,
        callbackClass: Class<*>,
        rrObj: Any,
        uri: String,
        cgiId: Int
    ): Any {
        return java.lang.reflect.Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass)
        ) { _, method, args ->
            if (method.name == "onGYNetEnd") {
                try {
                    val v0Var = args[4]
                    if (v0Var != null) {
                        val respWrapper = XposedHelpers.getObjectField(v0Var, "b")
                        val respObj = XposedHelpers.getObjectField(respWrapper, "a")
                        val bytes = XposedHelpers.callMethod(respObj, "toByteArray")

                        if (respObj != null) {
                            WePkgManager.notifyResponse(uri, cgiId, bytes as ByteArray)
                        }
                    }
                } catch (e: Exception) {
                    WeLogger.e("PkgDispatcher", "提取响应对象失败: ${e.message}")
                }
            }
            method.invoke(originalCallback, *args)
        }
    }

    /**
     * 从 IReqResp 对象中提取真正的 BaseProtoBuf 结构体
     */
    private fun extractPbObject(rrObj: Any, isRequest: Boolean): Any? {
        return try {
            if (isRequest) {
                try {
                    val r = XposedHelpers.callMethod(rrObj, "getReqObj")
                    XposedHelpers.callMethod(r, "toProtoBuf")
                } catch (_: NoSuchMethodError) {
                    null
                }
            } else {
                try {
                    val r = XposedHelpers.callMethod(rrObj, "getRespObj")
                    XposedHelpers.callMethod(r, "toProtoBuf")
                } catch (_: NoSuchMethodError) {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}