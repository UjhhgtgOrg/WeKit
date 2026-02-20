package moe.ouom.wekit.hooks.sdk.api

import android.util.SparseArray
import androidx.core.util.size
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import moe.ouom.wekit.core.dsl.dexMethod
import moe.ouom.wekit.core.model.ApiHookItem
import moe.ouom.wekit.dexkit.intf.IDexFind
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.utils.log.WeLogger
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "API/首页弹窗菜单服务", desc = "提供向首页弹窗菜单添加菜单项的能力")
class WeHomePagePopupMenuApi : ApiHookItem(), IDexFind {
    companion object {
        private const val TAG = "WeHomePagePopupMenuApi"
    }

    private val methodAddItem by dexMethod()
    private val methodHandleItemClick by dexMethod()

    override fun entry(classLoader: ClassLoader) {
        methodAddItem.toDexMethod {
            hook {
                beforeIfEnabled { param ->
                    val thisObj = param.thisObject
                    val sparseArray = thisObj.asResolver()
                        .firstField {
                            type = SparseArray::class
                        }
                        .get() as SparseArray<*>?
                    WeLogger.d(TAG, "sparseArray size: ${sparseArray?.size ?: "null"}")
                }
            }
        }

        methodHandleItemClick.toDexMethod {
            hook {
                beforeIfEnabled { param ->
                    val thisObj = param.thisObject
                    val sparseArray = thisObj.asResolver()
                        .firstField {
                            type = SparseArray::class
                        }
                        .get() as SparseArray<*>?
                    WeLogger.d(TAG, "sparseArray size: ${sparseArray?.size ?: "null"}")
                }
            }
        }
    }

    override fun dexFind(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        methodAddItem.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.ui")
            matcher {
                usingEqStrings("MicroMsg.PlusSubMenuHelper", "dyna plus config is null, we use default one")
            }
        }

        methodHandleItemClick.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.ui")
            matcher {
                usingEqStrings("MicroMsg.PlusSubMenuHelper", "processOnItemClick")
            }
        }

        return descriptors
    }
}