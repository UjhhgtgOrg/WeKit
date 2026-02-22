@file:Suppress("unused")

package moe.ouom.wekit.hooks.item.script

import moe.ouom.wekit.utils.WeProtoData
import moe.ouom.wekit.utils.log.WeLogger
import org.json.JSONObject
import java.util.regex.Pattern

// TODO: move to JsApiExposer
object WeProtoUtils {
    fun replaceUtf8ContainsInJson(json: JSONObject, needle: String, replacement: String): JSONObject {
        return try {
            val protoData = WeProtoData()
            protoData.fromJSON(json)
            protoData.replaceUtf8Contains(needle, replacement)
            protoData.toJSON()
        } catch (e: Exception) {
            WeLogger.e("Failed to replace in JSON: ${e.message}")
            json
        }
    }

    fun replaceUtf8RegexInJson(json: JSONObject, pattern: String, replacement: String): JSONObject {
        return try {
            val protoData = WeProtoData()
            protoData.fromJSON(json)
            val regex = Pattern.compile(pattern)
            protoData.replaceUtf8Regex(regex, replacement)
            protoData.toJSON()
        } catch (e: Exception) {
            WeLogger.e("Failed to regex replace in JSON: ${e.message}")
            json
        }
    }
}