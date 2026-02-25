package moe.ouom.wekit.hooks.items.shortvideos

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import de.robv.android.xposed.XC_MethodHook
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.ui.WeShortVideosShareMenuApi
import moe.ouom.wekit.ui.utils.showComposeDialog
import moe.ouom.wekit.utils.common.ModuleRes
import moe.ouom.wekit.utils.common.ToastUtils
import moe.ouom.wekit.utils.log.WeLogger
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@SuppressLint("StaticFieldLeak")
@HookItem(path = "视频号/查看媒体链接", desc = "向视频分享菜单中添加 '复制链接' 菜单项 (下载还没写, 目前先自己手动下载)")
object DisplayMediaLinks : BaseSwitchFunctionHookItem(), WeShortVideosShareMenuApi.IMenuItemsProvider {

    override fun entry(classLoader: ClassLoader) {
        Activity::class.asResolver()
            .firstMethod { name = "onResume" }
            .hookAfter { param ->
                val activity = param.thisObject as Activity
                if (activity.javaClass.name == "com.tencent.mm.plugin.finder.ui.FinderHomeAffinityUI") {
                    WeLogger.d("found FinderHomeAffinityUI")
                    context = activity
                }
            }

        WeShortVideosShareMenuApi.addProvider(this)
    }

    override fun unload(classLoader: ClassLoader) {
        WeShortVideosShareMenuApi.removeProvider(this)
        super.unload(classLoader)
    }

    private fun formatBytesSize(bytes: Int): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB", "PiB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()

        // Format to 2 decimal places
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return "%.2f %s".format(value, units[digitGroups])
    }

    // FIXME: still! doesn't! work!
    // pls somebody help me fix this
    private var context: Context? = null
    override fun getMenuItems(
        param: XC_MethodHook.MethodHookParam,
    ): List<WeShortVideosShareMenuApi.MenuItem> {
        return listOf(
            WeShortVideosShareMenuApi.MenuItem(777001, "复制链接", ModuleRes.getDrawable("link_24px"))
            { _, mediaType, mediaList ->
                if (mediaType == 2) {
                    val imageUrls = mediaList.map { json ->
                        json.getString("url") + json.getString("url_token")
                    }

                    showComposeDialog(context!!) { onDismiss ->
                        AlertDialog(onDismissRequest = onDismiss,
                            title = { Text("图片链接 (点击复制)") },
                            text = {
                                LazyColumn {
                                    itemsIndexed(imageUrls) { index, url ->
                                        ListItem(
                                            modifier = Modifier.clickable {
                                                val clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Url", url)
                                                clipboard.setPrimaryClip(clip)
                                                ToastUtils.showToast("已复制")
                                            },
                                            headlineContent = { Text("第 ${index + 1} 张") },
                                            supportingContent = { Text(url) }
                                        )
                                    }
                                }
                            },
                            confirmButton = { Button(onClick = onDismiss) { Text("关闭") } })
                    }
                    return@MenuItem
                }

                if (mediaType == 4) {
                    val json = mediaList[0]

                    val displayItems = mutableListOf<Pair<String, String>>()

                    val duration = json.getInt("videoDuration")
                    val size = json.getInt("fileSize")
                    val displayDuration = "%02d:%02d:%02d".format(Locale.CHINA,
                        duration / 3600, (duration % 3600) / 60, duration % 60)
                    val displaySize = formatBytesSize(size)
                    displayItems += "时长" to displayDuration
                    displayItems += "大小" to displaySize

                    val cdnInfo = json.optJSONObject("media_cdn_info")
                    if (cdnInfo == null || !cdnInfo.has("pcdn_url")) {
                        val url = json.getString("url")
                        val urlToken = json.getString("url_token")
                        val decodeKey = json.getString("decodeKey")
                        displayItems += "密链" to (url + urlToken)
                        displayItems += "密钥" to decodeKey
                    }
                    else {
                        displayItems += "链接" to json.getString("pcdn_url")
                    }

                    showComposeDialog { onDismiss ->
                        AlertDialog(onDismissRequest = onDismiss,
                            title = { Text("视频链接 (点击复制)") },
                            text = {
                                LazyColumn {
                                    items(displayItems) { (name, content) ->
                                        ListItem(
                                            modifier = Modifier.clickable {
                                                val clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Content", content)
                                                clipboard.setPrimaryClip(clip)
                                                ToastUtils.showToast("已复制")
                                            },
                                            headlineContent = { Text(name) },
                                            supportingContent = { Text(content) }
                                        )
                                    }
                                }
                            },
                            confirmButton = { Button(onClick = onDismiss) { Text("关闭") } })
                    }
                }

                ToastUtils.showToast("未知的媒体类型, 无法复制链接")
            }
        )
    }
}