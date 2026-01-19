package moe.ouom.wekit.hooks.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import moe.ouom.wekit.constants.Constants.Companion.CLAZZ_ICON_PREFERENCE
import moe.ouom.wekit.constants.Constants.Companion.CLAZZ_SETTINGS_UI
import moe.ouom.wekit.dexkit.TargetManager
import moe.ouom.wekit.hooks._base.ApiHookItem
import moe.ouom.wekit.hooks._core.annotation.HookItem
import moe.ouom.wekit.ui.CommonContextWrapper
import moe.ouom.wekit.ui.creator.dialog.MainSettingsDialog
import moe.ouom.wekit.util.log.Logger

@SuppressLint("DiscouragedApi")
@HookItem(path = "设置模块入口")
class WeSettingInjector : ApiHookItem() {

    companion object {
        private const val KEY_WEKIT_ENTRY = "wekit_settings_entry"
        private const val TITLE_WEKIT_ENTRY = "WeKit 设置"
    }

    override fun entry(classLoader: ClassLoader) {
        val methodSetKey = TargetManager.requireMethod(TargetManager.KEY_METHOD_SET_KEY)
        val methodSetTitle = TargetManager.requireMethod(TargetManager.KEY_METHOD_SET_TITLE)
        val methodGetKey = TargetManager.requireMethod(TargetManager.KEY_METHOD_GET_KEY)
        val methodAddPref = TargetManager.requireMethod(TargetManager.KEY_METHOD_ADD_PREF)

        if (methodSetKey == null || methodSetTitle == null || methodGetKey == null || methodAddPref == null) {
            Logger.e("WeSettingInjector: 关键方法未找到，跳过 Hook。请先运行 DexKit 分析")
            return
        }

        val clsSettingsUI = XposedHelpers.findClass(CLAZZ_SETTINGS_UI, classLoader)

        val mInitView = XposedHelpers.findMethodExact(
            clsSettingsUI,
            "initView",
            *arrayOf<Class<*>>()
        )

        hookAfter(mInitView) { param: XC_MethodHook.MethodHookParam ->
            val activity = param.thisObject as Activity
            val context = activity as Context

            try {
                val clsIconPref = XposedHelpers.findClass(CLAZZ_ICON_PREFERENCE, classLoader)
                val prefInstance = XposedHelpers.newInstance(clsIconPref, context)

                methodSetKey.invoke(prefInstance, KEY_WEKIT_ENTRY)
                methodSetTitle.invoke(prefInstance, TITLE_WEKIT_ENTRY)

                val prefScreen = XposedHelpers.callMethod(activity, "getPreferenceScreen")

                methodAddPref.invoke(prefScreen, prefInstance, 0)

            } catch (e: Throwable) {
                Logger.e("WeSettingInjector: 插入选项失败", e)
            }
        }

        Logger.i("WeSettingInjector: Created WeKit setting")
        XposedBridge.hookAllMethods(
            clsSettingsUI,
            "onPreferenceTreeClick",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        if (param.args.size < 2) return
                        val preference = param.args[1] ?: return

                        val key = methodGetKey.invoke(preference) as? String
                        Logger.d("WeKit Debug: Click key = $key")

                        if (KEY_WEKIT_ENTRY == key) {
                            val activity = param.thisObject as Activity

                            val fixContext = CommonContextWrapper.createAppCompatContext(activity)
                            val dialog = MainSettingsDialog(fixContext)
                            dialog.show()
                            param.result = true
                        }
                    } catch (t: Throwable) {
                        Logger.e(t)
                        Logger.e("WeSettingInjector: Click handle error", t)
                    }
                }
            }
        )

        Logger.i("WeSettingInjector: Hooked onPreferenceTreeClick")
    }

    override fun unload(classLoader: ClassLoader) {}
}