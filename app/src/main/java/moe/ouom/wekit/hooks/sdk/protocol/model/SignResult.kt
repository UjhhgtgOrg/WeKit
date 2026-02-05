package moe.ouom.wekit.hooks.sdk.protocol.model

import org.json.JSONObject

data class SignResult(
    val json: JSONObject,
    val nativeNetScene: Any? = null,
    val onSendSuccess: (() -> Unit)? = null
)