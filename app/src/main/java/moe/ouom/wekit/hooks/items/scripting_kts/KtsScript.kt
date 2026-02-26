package moe.ouom.wekit.hooks.items.scripting_kts

import android.content.ContentValues

interface KtsScript {
    fun onMessage(msgInfo: ContentValues)
}
