package moe.ouom.wekit.loader.startup

import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import moe.ouom.wekit.BuildConfig
import moe.ouom.wekit.constants.PackageConstants

@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {

    lateinit var packageParam: PackageParam

    override fun onInit() = configs {
        isDebug = BuildConfig.DEBUG
    }

    override fun onHook() = YukiHookAPI.encase {
        loadApp(isExcludeSelf = true) {
            packageParam = this
            val packageName = this.packageName
            if (packageName.startsWith(PackageConstants.PACKAGE_NAME_WECHAT)) {
                if (this.isFirstApplication) {
                    val modulePath = this.appInfo.sourceDir
                    StartupInfo.setModulePath(modulePath)
                    UnifiedEntryPoint.entry(modulePath, this.appInfo.dataDir,this.appClassLoader!!)
                }
            }
        }
    }
}