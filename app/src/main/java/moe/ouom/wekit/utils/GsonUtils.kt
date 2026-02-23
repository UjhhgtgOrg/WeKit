package moe.ouom.wekit.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject

fun JsonElement.getByPath(path: String): JsonElement? {
    var current: JsonElement? = this
    val keys = path.split(".")

    for (key in keys) {
        if (current is JsonObject) {
            current = current.get(key)
        } else {
            return null
        }
    }
    return current
}