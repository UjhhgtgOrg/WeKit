package moe.ouom.wekit.hooks.sdk.protocol

import moe.ouom.wekit.config.WePrefs
import moe.ouom.wekit.constants.PreferenceKeys
import moe.ouom.wekit.hooks.sdk.protocol.intf.IWePkgInterceptor
import moe.ouom.wekit.utils.WeProtoData
import moe.ouom.wekit.utils.log.WeLogger
import java.util.concurrent.CopyOnWriteArrayList

object WePkgManager {
    private val listeners = CopyOnWriteArrayList<IWePkgInterceptor>()

    fun addInterceptor(interceptor: IWePkgInterceptor) = listeners.addIfAbsent(interceptor)

    fun removeInterceptor(interceptor: IWePkgInterceptor) = listeners.remove(interceptor)

    internal fun handleRequestTamper(uri: String, cgiId: Int, reqBytes: ByteArray): ByteArray? {
        if (WePrefs.getBoolOrFalse(PreferenceKeys.VERBOSE_LOG)) {
            val data = WeProtoData()
            data.fromBytes(reqBytes)
            WeLogger.logChunkedI(
                "WePkgInterceptor-Request",
                "Request: $uri, CGI=$cgiId, LEN=${reqBytes.size}, Data=${data.toJsonObject()}, Stack=${WeLogger.getStackTraceString()}"
            )
        }

        for (listener in listeners) {
            val tampered = listener.onRequest(uri, cgiId, reqBytes)
            if (tampered != null) return tampered
        }
        return null
    }

    internal fun handleResponseTamper(uri: String, cgiId: Int, respBytes: ByteArray): ByteArray? {
        if (WePrefs.getBoolOrFalse(PreferenceKeys.VERBOSE_LOG)) {
            val data = WeProtoData()
            data.fromBytes(respBytes)
            WeLogger.logChunkedI(
                "WePkgInterceptor-Response",
                "Received: $uri, CGI=$cgiId, LEN=${respBytes.size}, Data=${data.toJsonObject()}"
            )
        }
        for (listener in listeners) {
            val tampered = listener.onResponse(uri, cgiId, respBytes)
            if (tampered != null) return tampered
        }
        return null
    }
}