//package moe.ouom.wekit.hooks.items.contacts
//
//import android.app.Activity
//import android.widget.BaseAdapter
//import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
//import moe.ouom.wekit.hooks.core.annotation.HookItem
//import moe.ouom.wekit.hooks.sdk.ui.WePreferenceScreenApi
//import moe.ouom.wekit.utils.log.WeLogger
//
//@HookItem(
//    path = "联系人与群组/显示微信 ID (新)",
//    desc = "在联系人详情页面显示微信 ID"
//)
//object ShowWxIdInContactDetailsNew : BaseSwitchFunctionHookItem(),
//    WePreferenceScreenApi.IPrefItemsProvider {
//
//    private const val TAG = "ShowWxIdInContactDetailsNew"
//
//    override fun entry(classLoader: ClassLoader) {
//        WePreferenceScreenApi.addProvider(this)
//    }
//
//    override fun unload(classLoader: ClassLoader) {
//        WePreferenceScreenApi.removeProvider(this)
//        super.unload(classLoader)
//    }
//
//    override fun getMenuItems(
//        activity: Activity,
//        baseAdapter: BaseAdapter
//    ): List<WePreferenceScreenApi.PrefItem> {
//        WeLogger.i(TAG, activity.intent.getStringExtra("Contact_User") ?: "null")
//        return emptyList()
//    }
//}