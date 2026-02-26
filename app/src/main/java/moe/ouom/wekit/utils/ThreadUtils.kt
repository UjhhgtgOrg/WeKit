package moe.ouom.wekit.utils

import android.os.Build

// this is what we call 'technical debt'
fun Thread.getThreadId(): Long {
    return this.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            it.threadId()
        } else {
            @Suppress("DEPRECATION")
            it.id
        }
    }
}