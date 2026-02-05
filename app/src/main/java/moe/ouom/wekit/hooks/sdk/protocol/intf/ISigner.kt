package moe.ouom.wekit.hooks.sdk.protocol.intf

import moe.ouom.wekit.hooks.sdk.protocol.model.SignResult
import org.json.JSONObject

interface ISigner {
    fun match(cgiId: Int): Boolean
    fun sign(loader: ClassLoader, json: JSONObject): SignResult
}