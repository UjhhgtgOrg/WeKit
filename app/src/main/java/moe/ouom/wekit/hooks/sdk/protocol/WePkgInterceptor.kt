package moe.ouom.wekit.hooks.sdk.protocol

import java.util.concurrent.CopyOnWriteArrayList

/**
 * 协议监听接口
 */
interface WePkgInterceptor {
    fun onRequest(uri: String, cgiId: Int, reqBytes: ByteArray) {}
    fun onResponse(uri: String, cgiId: Int, respBytes: ByteArray) {}
}

/**
 * 全局协议分发中心
 */
object WePkgManager {
    private val listeners = CopyOnWriteArrayList<WePkgInterceptor>()

    fun addInterceptor(interceptor: WePkgInterceptor) {
        if (!listeners.contains(interceptor)) {
            listeners.add(interceptor)
        }
    }

    fun removeInterceptor(interceptor: WePkgInterceptor) {
        listeners.remove(interceptor)
    }

    internal fun notifyRequest(uri: String, cgiId: Int, reqBytes: ByteArray) {
        listeners.forEach { it.onRequest(uri, cgiId, reqBytes) }
    }

    internal fun notifyResponse(uri: String, cgiId: Int, respBytes: ByteArray) {
        listeners.forEach { it.onResponse(uri, cgiId, respBytes) }
    }
}