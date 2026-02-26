package moe.ouom.wekit.utils.common

import android.content.Context
import android.system.Os
import moe.ouom.wekit.config.WeConfig
import moe.ouom.wekit.utils.hookstatus.AbiUtils.archStringToArchInt
import moe.ouom.wekit.utils.hookstatus.AbiUtils.archStringToLibDirName
import moe.ouom.wekit.utils.hookstatus.AbiUtils.getApplicationActiveAbi
import moe.ouom.wekit.utils.hookstatus.AbiUtils.getSuggestedAbiVariant
import moe.ouom.wekit.utils.hookstatus.AbiUtils.queryModuleAbiList

object CheckAbiVariantModel {
    val HOST_PACKAGES = setOf(
        "com.tencent.mm",
    )

    fun collectAbiInfo(context: Context): AbiInfo {
        val abiInfo = AbiInfo()
        val uts = Os.uname()
        val sysAbi = uts.machine
        abiInfo.sysArchName = sysAbi
        abiInfo.sysArch = archStringToArchInt(sysAbi)

        val requestAbis = HashSet<String>()
        requestAbis.add(archStringToLibDirName(sysAbi))
        for (pkg in HOST_PACKAGES) {
            val activeAbi = getApplicationActiveAbi(pkg) ?: continue
            val abi = archStringToLibDirName(activeAbi)
            if (!isPackageIgnored(pkg)) {
                requestAbis.add(abi)
            }
            val pi = AbiInfo.Package()
            pi.abi = archStringToArchInt(activeAbi)
            pi.ignored = isPackageIgnored(pkg)
            pi.packageName = pkg
            abiInfo.packages[pkg] = pi
        }
        val modulesAbis = queryModuleAbiList()
        val missingAbis = HashSet<String?>()
        // check if modulesAbis contains all requestAbis
        for (abi in requestAbis) {
            if (!modulesAbis.contains(abi)) {
                missingAbis.add(abi)
            }
        }
        abiInfo.isAbiMatch = missingAbis.isEmpty()
        var abi = 0
        for (name in requestAbis) {
            abi = abi or archStringToArchInt(name)
        }
        abiInfo.suggestedApkAbiVariant = getSuggestedAbiVariant(abi)
        return abiInfo
    }

    fun setPackageIgnored(packageName: String, ignored: Boolean) {
        val cfg = WeConfig.getDefaultConfig()
        cfg.putBoolean("native_lib_abi_ignore.$packageName", ignored)
    }

    fun isPackageIgnored(packageName: String): Boolean {
        val cfg = WeConfig.getDefaultConfig()
        return cfg.getBoolean("native_lib_abi_ignore.$packageName", false)
    }

    class AbiInfo {
        class Package {
            var packageName: String? = null
            var abi: Int = 0
            var ignored: Boolean = false
        }

        var packages: MutableMap<String?, Package?> = HashMap<String?, Package?>()
        var sysArchName: String? = null
        var sysArch: Int = 0
        var isAbiMatch: Boolean = false
        var suggestedApkAbiVariant: String? = null
    }
}
