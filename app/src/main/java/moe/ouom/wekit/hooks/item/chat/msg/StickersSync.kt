package moe.ouom.wekit.hooks.item.chat.msg

import android.content.ContentValues
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.condition.type.Modifiers
import com.highcapable.kavaref.extension.createInstance
import com.highcapable.kavaref.extension.toClass
import moe.ouom.wekit.core.dsl.dexClass
import moe.ouom.wekit.core.dsl.dexMethod
import moe.ouom.wekit.core.model.BaseClickableFunctionHookItem
import moe.ouom.wekit.dexkit.intf.IDexFind
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.host.HostInfo
import moe.ouom.wekit.ui.compose.showComposeDialog
import moe.ouom.wekit.utils.common.ToastUtils
import moe.ouom.wekit.utils.io.PathUtils
import moe.ouom.wekit.utils.log.WeLogger
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Modifier
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk

@HookItem(path = "聊天与消息/贴纸表情同步", desc = "从指定路径将所有图片注册为贴纸表情")
class StickersSync : BaseClickableFunctionHookItem(), IDexFind {
    companion object {
        private const val TAG = "StickersSync"
        private const val STICKER_PACK_ID = "wekit.stickers.sync"
        private val ALLOWED_STICKER_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp")
    }

    private val stickers: MutableList<Any> by lazy {
        val list = mutableListOf<Any>()
        val dir = stickersDir
        if (dir == null) {
            WeLogger.e(TAG, "could not get stickers directory, skipped")
            return@lazy list
        }
        // although docs say createDirectories doesn't throw,
        // that's not true for symbolic link
        // note: symbolic link probably won't work, because of permission
        try {
            dir.createDirectories()
        }
        catch (_: java.nio.file.FileAlreadyExistsException) {
            WeLogger.i(TAG, "stickers directory is a symbolic link and already exists, ignoring exception")
        }
        catch (ex: Exception) {
            WeLogger.e(TAG, "failed to create stickers directory, skipped", ex)
            return@lazy list
        }
        ToastUtils.showToast("正在加载贴纸表情, 请稍候...")
        val images = dir.walk()
            .filter { path ->
                path.isRegularFile() && path.extension.lowercase() in ALLOWED_STICKER_EXTENSIONS
            }
            .toList()
        ToastUtils.showToast("找到 ${images.size} 个贴纸表情文件, 正在处理...")
        WeLogger.d(TAG, "found ${images.size} sticker files in ${dir.absolutePathString()}")
        images.forEach { path ->
                val actualPath = if (path.extension.lowercase() == "webp") {
                    convertWebpToPng(path) ?: return@forEach
                } else {
                    path
                }

                val absPath = actualPath.absolutePathString()
                val something = getEmojiMd5FromPath(HostInfo.getApplication(), absPath)
                val emojiThumb = getEmojiThumbByMd5(something)
                methodSaveEmojiThumb.method.invoke(emojiThumb, null, true)
                val groupItemInfo = classGroupItemInfo.clazz
                    .getDeclaredConstructor("com.tencent.mm.api.IEmojiInfo".toClass(),
                        Int::class.java, String::class.java, Int::class.java)
                    .newInstance(emojiThumb, 2, "", 0)
                list.add(groupItemInfo)
            }
        ToastUtils.showToast("成功加载 ${images.size} 个贴纸表情")
        WeLogger.i(TAG, "processed ${images.size} stickers")
        return@lazy list
    }

    private fun convertWebpToPng(webpPath: Path): Path? {
        return try {
            val pngPath = webpPath.resolveSibling("${webpPath.nameWithoutExtension}.png")

            if (pngPath.isRegularFile()) {
                // prevent logcat io bottleneck
                // WeLogger.d(TAG, "PNG already exists, using: ${pngPath.absolutePathString()}")
                return pngPath
            }

            val webpBitmap = android.graphics.BitmapFactory.decodeFile(webpPath.absolutePathString())
            if (webpBitmap == null) {
                WeLogger.e(TAG, "failed to decode WebP: ${webpPath.absolutePathString()}")
                return null
            }
            pngPath.toFile().outputStream().use { output ->
                webpBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
            }
            webpBitmap.recycle()
            // prevent logcat io bottleneck
            // WeLogger.d(TAG, "converted WebP to PNG: ${pngPath.absolutePathString()}")
            pngPath
        } catch (ex: Exception) {
            WeLogger.e(TAG, "failed to convert WebP to PNG: ${webpPath.absolutePathString()}", ex)
            null
        }
    }

    private val methodGetEmojiGroupInfo by dexMethod()
    private val methodAddAllGroupItems by dexMethod()
    private val methodServiceManagerGetService by dexMethod()
    // FIXME: this module doesn't provide a builtin dexConstructor, so i have to use dexClass,
    //        and then use .createInstance()
    private val classGroupItemInfo by dexClass()
    private val classEmojiFeatureService by dexClass()
    private val classEmojiMgrImpl by dexClass()
    private val classEmojiStorageMgr by dexClass()
    private val classEmojiInfoStorage by dexClass()
    private val methodSaveEmojiThumb by dexMethod()
    private val classSqliteDb by dexClass()
    private val classMmKernel by dexClass()
//    private val methodMmKernelGetServiceImpl by dexMethod()
    private val classCoreStorage by dexClass()

    private val stickersDir: Path?
        get() = PathUtils.moduleDataPath?.resolve("stickers")

    private fun getServiceByClass(clazz: Class<*>): Any {
        return methodServiceManagerGetService.method.invoke(null, clazz)!!
    }

    private fun getEmojiFeatureService(): Any {
        val service = getServiceByClass(classEmojiFeatureService.clazz)
        return service.asResolver()
            .firstMethod {
                returnType = classEmojiMgrImpl.clazz
            }
            .invoke()!!
    }

    private fun getEmojiMd5FromPath(context: Context, path: String): String {
        return getEmojiFeatureService()
            .asResolver()
            .firstMethod {
                parameters(Context::class.java, String::class.java)
                returnType = String::class.java
            }
            .invoke(context, path) as String
    }

    private fun getEmojiThumbByMd5(md5: String): Any {
        val emojiStorageMgr = classEmojiStorageMgr.clazz.asResolver()
            .firstMethod {
                modifiers(Modifiers.STATIC)
                returnType = classEmojiStorageMgr.clazz
            }
            .invoke()!!
        val emojiInfoStorage = emojiStorageMgr.asResolver()
            .firstMethod {
                returnType = classEmojiInfoStorage.clazz
            }
            .invoke()!!
        val emojiThumb = emojiInfoStorage.asResolver()
            .firstMethod {
                parameters(String::class)
                returnType = "com.tencent.mm.storage.emotion.EmojiInfo"
            }
            .invoke(md5)!!
        return emojiThumb
    }

    private fun getCoreStorage(): Any {
        val mmKernel = classMmKernel.clazz
        return mmKernel.asResolver()
            .firstMethod {
                returnType = classCoreStorage.clazz
                parameterCount = 0
            }
            .invoke()!!
    }

    private fun getSqliteDatabase(): Any {
        val db = getCoreStorage().asResolver()
            .firstField {
                type(classSqliteDb.clazz)
            }
            .get()!!
        return db.asResolver()
            .firstMethod {
                returnType = "com.tencent.wcdb.database.SQLiteDatabase"
                parameterCount = 0
            }
            .invoke()!!
    }

    override fun entry(classLoader: ClassLoader) {
        val emojiGroupInfoCls = "com.tencent.mm.storage.emotion.EmojiGroupInfo".toClass(classLoader)

        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
        methodGetEmojiGroupInfo.toDexMethod {
            hook {
                afterIfEnabled { param ->
                    WeLogger.i(TAG, "getEmojiGroupInfo called, result: ${param.result.javaClass.name}")

                    if (param.result !is java.util.List<*>) {
                        WeLogger.d(TAG, "param result is not list, skipped")
                        return@afterIfEnabled
                    }

                    val stickersPackData = ContentValues()
                    stickersPackData.put(
                        "packGrayIconUrl",
                        "https://avatars.githubusercontent.com/u/49312623"
                    )
                    stickersPackData.put(
                        "packIconUrl",
                        "https://avatars.githubusercontent.com/u/49312623"
                    )
                    stickersPackData.put("packName", "贴纸表情同步")
                    stickersPackData.put("packStatus", 1)
                    stickersPackData.put("productID", STICKER_PACK_ID)
                    stickersPackData.put("status", 7)
                    stickersPackData.put("sync", 2)

                    val emojiGroupInfo = emojiGroupInfoCls.createInstance()
                    emojiGroupInfoCls.getMethod("convertFrom",
                        ContentValues::class.java, Boolean::class.java)
                        .invoke(emojiGroupInfo, stickersPackData, true)

                    (param.result as java.util.List<Any?>).add(0, emojiGroupInfo)
                    WeLogger.i(TAG, "injected sticker pack info")
                }
            }
        }

        methodAddAllGroupItems.toDexMethod {
            hook {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
                beforeIfEnabled { param ->
                    val manager = param.args[0]
                    if (manager == null) {
                        WeLogger.w("args[0] is null, skipped")
                        return@beforeIfEnabled
                    }

                    val packConfig = manager.asResolver()
                        .firstMethod {
                            superclass()
                            modifiers(Modifiers.FINAL)
                            returnType {
                                it != Boolean::class.java
                            }
                        }
                        .invoke()
                    val emojiGroupInfo = packConfig!!.asResolver()
                        .firstField {
                            type("com.tencent.mm.storage.emotion.EmojiGroupInfo".toClass(classLoader))
                        }.get()!!
                    val packName = emojiGroupInfo.asResolver()
                        .firstField {
                            superclass()
                            name = "field_packName"
                        }
                        .get()!! as String
                    WeLogger.d(TAG, "current pack name: $packName")
                    WeLogger.d(TAG, "stickers count: ${stickers.size}")
                    if (packName == "贴纸表情同步") {
                        val stickerList = manager.asResolver().firstMethod {
                            superclass()
                            returnType = List::class.java
                        }.invoke() as java.util.List<Any>
                        stickerList.addAll(stickers)
                    }
                }
            }
        }
    }

    override fun dexFind(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        methodGetEmojiGroupInfo.find(dexKit, descriptors) {
            matcher {
                paramTypes(Int::class.java)
                usingEqStrings("MicroMsg.emoji.EmojiGroupInfoStorage", "get Panel EmojiGroupInfo.")
            }
        }

        methodAddAllGroupItems.find(dexKit, descriptors) {
            matcher {
                usingEqStrings("data")
                addInvoke {
                    usingEqStrings("checkScrollToPosition: ")
                }
            }
        }

        classGroupItemInfo.find(dexKit, descriptors) {
            matcher {
                methods {
                    add {
                        usingEqStrings("emojiInfo", "sosDocId")
                    }
                }
            }
        }

        methodServiceManagerGetService.find(dexKit, descriptors) {
            matcher {
                modifiers(Modifier.STATIC)
                paramTypes(Class::class.java)
                usingEqStrings("calling getService(...)")
            }
        }

        classEmojiFeatureService.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.feature.emoji")
            matcher {
                methods {
                    add {
                        usingEqStrings("MicroMsg.EmojiFeatureService", "[onAccountInitialized]")
                    }
                }
            }
        }

        classEmojiMgrImpl.find(dexKit, descriptors) {
            matcher {
                methods {
                    add {
                        usingEqStrings("MicroMsg.emoji.EmojiMgrImpl", "sendEmoji: context is null")
                    }
                }
            }
        }

        classEmojiStorageMgr.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.storage")
            matcher {
                methods {
                    add {
                        usingEqStrings("MicroMsg.emoji.EmojiStorageMgr", "EmojiStorageMgr: %s")
                    }
                }
            }
        }

        classEmojiInfoStorage.find(dexKit, descriptors) {
            matcher {
                methods {
                    add {
                        usingEqStrings("MicroMsg.emoji.EmojiInfoStorage", "md5 is null or invalue. md5:%s")
                    }
                }
            }
        }

        methodSaveEmojiThumb.find(dexKit, descriptors) {
            matcher {
                declaredClass("com.tencent.mm.storage.emotion.EmojiInfo")
                usingEqStrings("save emoji thumb error")
            }
        }

        classSqliteDb.find(dexKit, descriptors) {
            matcher {
                methods {
                    add {
                        usingEqStrings("MicroMsg.DBInit", "initSysDB checkini:%b exist:%b db:%s ")
                    }
                }
            }
        }

        classMmKernel.find(dexKit, descriptors) {
            matcher {
                methods {
                    add {
                        usingEqStrings("MicroMsg.MMKernel", "Kernel not null, has initialized.")
                    }
                }
            }
        }

        // FIXME: this has errors, although doesn't affect the functionality of this hook
//        methodMmKernelGetServiceImpl.find(dexKit, descriptors) {
//            matcher {
//                declaredClass(classMmKernel.clazz)
//                returnType(Class::class.java)
//            }
//        }

        classCoreStorage.find(dexKit, descriptors) {
            matcher {
                methods {
                    add {
                        usingEqStrings("MMKernel.CoreStorage",
                            "CheckData path[%s] blocksize:%s blockcount:%s availcount:%s")
                    }
                }
            }
        }

        return descriptors
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onClick(context: Context?) {
        showComposeDialog(context) { onDismiss ->
            AlertDialog(onDismissRequest = onDismiss,
                title = { Text("贴纸表情同步") },
                text = {
                    Column {
                        Row(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getSqliteDatabase().asResolver()
                                        .firstMethod {
                                            name = "delete"
                                            parameters(String::class, String::class, Array<String>::class)
                                        }
                                        .invoke("EmojiGroupInfo", "productID = ?", arrayOf(STICKER_PACK_ID))
                                    ToastUtils.showToast("清除成功!")
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("清除应用数据库缓存", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                })
        }
    }
}