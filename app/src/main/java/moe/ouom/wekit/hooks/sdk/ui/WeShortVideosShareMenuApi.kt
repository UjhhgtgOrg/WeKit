package moe.ouom.wekit.hooks.sdk.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import de.robv.android.xposed.XC_MethodHook
import moe.ouom.wekit.core.dsl.dexMethod
import moe.ouom.wekit.core.model.ApiHookItem
import moe.ouom.wekit.dexkit.intf.IDexFind
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.utils.log.WeLogger
import org.json.JSONObject
import org.luckypray.dexkit.DexKitBridge
import java.util.LinkedList
import java.util.concurrent.CopyOnWriteArrayList

@SuppressLint("StaticFieldLeak")
@HookItem(path = "API/视频号分享菜单扩展", desc = "为视频号分享菜单提供添加菜单项功能")
object WeShortVideosShareMenuApi : ApiHookItem(), IDexFind {

    interface IMenuItemsProvider {
        fun getMenuItems(param: XC_MethodHook.MethodHookParam, context: Context): List<MenuItem>
    }
    data class MenuItem(val id: Int,
                        val text: String, val drawable: Drawable,
                        val onClick: (XC_MethodHook.MethodHookParam, Int, List<JSONObject>) -> Unit)

    private const val TAG: String = "WeShortVideosShareMenuApi"

    private val providers = CopyOnWriteArrayList<IMenuItemsProvider>()

    fun addProvider(provider: IMenuItemsProvider) {
        if (!providers.contains(provider)) {
            providers.add(provider)
            WeLogger.i(TAG, "provider added, current provider count: ${providers.size}")
        } else {
            WeLogger.w(TAG, "provider already exists, ignored")
        }
    }

    fun removeProvider(provider: IMenuItemsProvider) {
        val removed = providers.remove(provider)
        WeLogger.i(TAG, "provider remove ${if (removed) "succeeded" else "failed"}, current provider count: ${providers.size}")
    }

    private val methodCreateMenu by dexMethod()
    private val methodSelectMenuItem by dexMethod()

    override fun entry(classLoader: ClassLoader) {
        methodCreateMenu.toDexMethod {
            hook {
                beforeIfEnabled { param ->
                    val menu = param.args[0]
                    val context = findContext(param.thisObject).also {
                        if (it == null) {
                            WeLogger.e(TAG, "could not find context, skipping")
                            return@beforeIfEnabled
                        }
                    } as Context

                    for (provider in providers) {
                        try {
                            for (item in provider.getMenuItems(param, context)) {
                                menu.asResolver()
                                    .firstMethod {
                                        parameters(Int::class, CharSequence::class, Drawable::class)
                                    }
                                    .invoke(item.id, item.text, item.drawable)
                            }
                        }
                        catch (ex: Exception) {
                            WeLogger.e(TAG, "provider ${provider.javaClass.name} threw while providing menu items", ex)
                        }
                    }
                }
            }
        }

        methodSelectMenuItem.toDexMethod {
            hook {
                beforeIfEnabled { param ->
                    val activity = findContext(param.thisObject).also {
                        if (it == null) {
                            WeLogger.e(TAG, "could not find MMActivity, skipping")
                            return@beforeIfEnabled
                        }
                    } as Activity

                    val menuItem = param.args[0] as android.view.MenuItem
                    val itemId = menuItem.itemId

                    val baseFinderFeed = param.thisObject.asResolver()
                        .firstField {
                            type = "com.tencent.mm.plugin.finder.model.BaseFinderFeed"
                        }
                        .get()!!
                    val finderItem = baseFinderFeed.asResolver()
                        .firstField {
                            name = "feedObject"
                            superclass()
                        }
                        .get()!!
                    val mediaType = finderItem.asResolver()
                        .firstMethod {
                            name = "getMediaType"
                        }
                        .invoke()!! as Int
                    val mediaList = finderItem.asResolver()
                        .firstMethod {
                            name = "getMediaList"
                        }
                        .invoke() as LinkedList<*>
                    val mediaJsonList = mediaList.map { media ->
                        media.asResolver()
                            .firstMethod {
                                name = "toJSON"
                                superclass()
                            }.invoke()!! as JSONObject
                    }

                    for (provider in providers) {
                        try {
                            for (item in provider.getMenuItems(param, activity)) {
                                if (item.id == itemId) {
                                    item.onClick(param, mediaType, mediaJsonList)
                                    param.result = null
                                    return@beforeIfEnabled
                                }
                            }
                        }
                        catch (ex: Exception) {
                            WeLogger.e(TAG, "provider ${provider.javaClass.name} threw while handling click event", ex)
                        }
                    }
                }
            }
        }
    }

    // FIXME: android.view.WindowManager$BadTokenException: Unable to add window -- token null is not valid; is your activity running?
    private fun findContext(hookInstance: Any): Any? {
        val clazz = hookInstance.javaClass

        val fields = clazz.declaredFields

        for (field in fields) {
            field.isAccessible = true
            val fieldValue = field.get(hookInstance) ?: continue

            val innerFields = fieldValue.javaClass.declaredFields
            for (innerField in innerFields) {
                if (innerField.type.name == "com.tencent.mm.plugin.finder.ui.fragment.FinderHomeTabFragment") {
                    innerField.isAccessible = true
                    return innerField.get(fieldValue).asResolver()
                        .firstMethod {
                            name = "getActivity"
                            superclass()
                        }.invoke()
                }
            }
        }

        return null
    }

    override fun dexFind(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        methodCreateMenu.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.plugin.finder.feed")
            matcher {
                name = "onCreateMMMenu"
                usingEqStrings("pos is error ")
            }
        }

        methodSelectMenuItem.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.plugin.finder.feed")
            matcher {
                name = "onMMMenuItemSelected"
                usingEqStrings("[getMoreMenuItemSelectedListener] feed ")
            }
        }

        return descriptors
    }
}