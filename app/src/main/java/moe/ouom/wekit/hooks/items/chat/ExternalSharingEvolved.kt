package moe.ouom.wekit.hooks.items.chat

import android.app.Person
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.host.HostInfo

@HookItem(path = "聊天/分享进化", desc = "让应用的系统分享菜单更易用 (没写完)")
object ExternalSharingEvolved : BaseSwitchFunctionHookItem() {

    override fun entry(classLoader: ClassLoader) {

        val context = HostInfo.getApplication()

        val shortcutManager = context.getSystemService(ShortcutManager::class.java) as ShortcutManager

        val contact = Person.Builder()
            .setName("John Doe")
            .setImportant(true)
            .build()

        val shortcut = ShortcutInfo.Builder(context, "contact_id_123")
            .setShortLabel("John Doe")
            .setPerson(contact)
            .setCategories(setOf("android.intent.category.DEFAULT"))
            .setIntent(
                Intent(Intent.ACTION_SEND)
                    .setComponent(
                        ComponentName(
                            "com.tencent.mm",
                            // although this activity is called 'ShareImg',
                            // it is actually used to handle all types
                            "com.tencent.mm.ui.tools.ShareImgUI"
                        )
                    )
                    .setTextData("hi")
            )
            .setLongLived(true)
            .build()

        shortcutManager.addDynamicShortcuts(listOf(shortcut))
    }

    private fun Intent.setTextData(text: String): Intent {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        return this
    }
}