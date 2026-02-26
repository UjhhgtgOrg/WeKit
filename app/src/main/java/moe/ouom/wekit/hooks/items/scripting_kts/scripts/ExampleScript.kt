package moe.ouom.wekit.hooks.items.scripting_kts.scripts

import android.content.ContentValues
import moe.ouom.wekit.hooks.items.scripting_kts.KtsScript
import moe.ouom.wekit.hooks.sdk.base.WeMessageApi

// my ide refuses to auto-import when i name this .kts

object : KtsScript {
    override fun onMessage(msgInfo: ContentValues) {
        val content = msgInfo.getAsString("content") ?: return
        val talker = msgInfo.getAsString("talker") ?: return
        if (content == "echo") {
            WeMessageApi.sendText(talker, content)
        }
    }
}
