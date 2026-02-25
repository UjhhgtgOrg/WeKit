package moe.ouom.wekit.hooks.items.chat

import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem

@HookItem(path = "聊天/分享进化", desc = "让应用的系统分享菜单更易用 (没写完)")
object ExternalSharingEvolved : BaseSwitchFunctionHookItem() {

    override fun entry(classLoader: ClassLoader) {

//        val context = HostInfo.getApplication()
//
//        val shortcutManager: ShortcutManager = context.getSystemService(ShortcutManager::class.java)
//
//        val contact = Person.Builder()
//            .setName("John Doe")
//            .setImportant(true)
//            .build()
//
//        val shortcut = ShortcutInfo.Builder(context, "contact_id_123")
//            .setShortLabel("John Doe")
//            .setPerson(contact)
//            .setCategories(mutableSetOf("android.intent.category.DEFAULT"))
//            .setIntent(
//                Intent(Intent.ACTION_SEND)
//                    .setComponent(
//                        ComponentName(
//                            "com.tencent.mm",
//                            // although this activity is called 'ShareImg',
//                            // it is actually used to handle all types
//                            "com.tencent.mm.ui.tools.ShareImgUI"
//                        )
//                    )
////                    .putExtra("contact_user_name", "wxid_v7xxx")
//            )
//            .setLongLived(true)
//            .build()
//
//        shortcutManager.addDynamicShortcuts(listOf(shortcut))
    }
}