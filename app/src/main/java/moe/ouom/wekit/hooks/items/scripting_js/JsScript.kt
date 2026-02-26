package moe.ouom.wekit.hooks.items.scripting_js

data class JsScript(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val script: String,
    val enabled: Boolean = true,
)