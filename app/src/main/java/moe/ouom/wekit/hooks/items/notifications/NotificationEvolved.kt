package moe.ouom.wekit.hooks.items.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.ouom.wekit.constants.PackageConstants
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.base.WeConversationApi
import moe.ouom.wekit.hooks.sdk.base.WeDatabaseApi
import moe.ouom.wekit.hooks.sdk.base.WeMessageApi
import moe.ouom.wekit.hooks.sdk.protocol.WeApi
import moe.ouom.wekit.host.HostInfo
import moe.ouom.wekit.utils.log.WeLogger
import java.net.HttpURLConnection
import java.net.URL

@HookItem(
    path = "通知/通知进化",
    desc = "让应用的新消息通知更易用\n1. '快速回复' 按钮\n2. '标记为已读' 按钮\n3. 使用原生对话样式 (MessagingStyle)"
)
object NotificationEvolved : BaseSwitchFunctionHookItem() {

    private const val TAG = "NotificationEvolved"
    private const val ACTION_REPLY = "${PackageConstants.PACKAGE_NAME_WECHAT}.ACTION_WEKIT_REPLY"
    private const val ACTION_MARK_READ =
        "${PackageConstants.PACKAGE_NAME_WECHAT}.ACTION_WEKIT_MARK_READ"

    // cache friends to avoid repeating sql queries
    // TODO: build a sql statement to directly query target contact
    private val friends by lazy { WeDatabaseApi.getFriends() }

    // TODO: see if we can retrieve avatar icon from local storage instead of remote
    private var meAvatarIcon: Icon? = null

    val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val targetWxId = intent.getStringExtra("extra_target_wxid") ?: return
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            when (intent.action) {
                ACTION_REPLY -> {
                    val results = RemoteInput.getResultsFromIntent(intent) ?: return
                    val replyContent = results.getCharSequence("key_reply_content")?.toString()

                    if (replyContent.isNullOrEmpty())
                        return

                    WeLogger.i(TAG, "Quick replying '$replyContent' to $targetWxId")
                    WeMessageApi.sendText(targetWxId, replyContent)
                    WeConversationApi.markAsRead(targetWxId)
                    notificationManager.cancel(targetWxId.hashCode())
                }

                ACTION_MARK_READ -> {
                    WeLogger.i(TAG, "Marking chat as read for $targetWxId")
                    WeConversationApi.markAsRead(targetWxId)
                    notificationManager.cancel(targetWxId.hashCode())
                }
            }
        }
    }

    val multiMessageRegex = Regex("""^\[\d+条].+?: (.*)$""")

    override fun entry(classLoader: ClassLoader) {
        val context = HostInfo.getApplication()

        val filter = IntentFilter().apply {
            addAction(ACTION_REPLY)
            addAction(ACTION_MARK_READ)
        }
        ContextCompat.registerReceiver(
            context, notificationReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )

        Notification.Builder::class.asResolver()
            .firstMethod { name = "build" }
            .hookBefore { param ->
                // fetch the avatar at some point where we're sure that mmPrefs is initialized
                if (meAvatarIcon == null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        meAvatarIcon = runCatching {
                            val urlString = WeDatabaseApi.getAvatarUrl(WeApi.selfWxId)
                            val connection = URL(urlString).openConnection() as HttpURLConnection
                            connection.doInput = true

                            connection.inputStream.use { input ->
                                val bitmap = BitmapFactory.decodeStream(input)
                                WeLogger.i(TAG, "fetched me avatar")
                                Icon.createWithBitmap(bitmap)
                            }
                        }.onFailure { e ->
                            WeLogger.e(TAG, "failed to fetch me avatar", e)
                        }.getOrNull()
                    }
                }

                val builder = param.thisObject as Notification.Builder
                val notification = builder.asResolver().firstField { type = Notification::class }
                    .get() as Notification
                val channelId = notification.channelId

                if (channelId != "message_channel_new_id") {
                    return@hookBefore
                }

                val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: "Unknown"
                val rawText =
                    notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                        ?: ""

                val matchResult = multiMessageRegex.find(rawText)
                var cleanText = if (matchResult != null) {
                    matchResult.groupValues[1]
                } else {
                    rawText
                }

                cleanText = cleanText
                    .replaceRichContent()
                    .replaceEmojis()

                // 1. Resolve exact WXID immediately during notification creation
                val friend =
                    friends.firstOrNull { it.nickname == title || it.remarkName == title }
                val targetWxid = friend?.wxid

                if (targetWxid == null) {
                    WeLogger.w(TAG, "could not resolve wxid for $title, skipping enhancements")
                    return@hookBefore
                }

                WeLogger.i(TAG, "enhancing notification for $title ($targetWxid)")

                // 2. Build the MessagingStyle
                // TODO: add cropping
                val mePerson = Person.Builder().setName("我").setIcon(meAvatarIcon).build()
                val senderPerson = Person.Builder().setName(title).build()

                val messagingStyle = Notification.MessagingStyle(mePerson)
                messagingStyle.addMessage(cleanText, System.currentTimeMillis(), senderPerson)

                // FIXME: the >=2nd unread message from a group chat omits the sender's name
                if (isGroupChat(targetWxid)) {
                    messagingStyle.isGroupConversation = true
                    messagingStyle.conversationTitle = title
                }

                builder.style = messagingStyle

                // 3. Quick Reply Action
                val remoteInput = RemoteInput.Builder("key_reply_content")
                    .setLabel("输入回复内容...")
                    .build()

                val replyIntent = Intent(ACTION_REPLY).apply {
                    setPackage(PackageConstants.PACKAGE_NAME_WECHAT)
                    putExtra("extra_target_wxid", targetWxid)
                }
                val replyPendingIntent = PendingIntent.getBroadcast(
                    context, targetWxid.hashCode(), replyIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val replyAction = Notification.Action.Builder(
                    Icon.createWithResource(context, android.R.drawable.ic_menu_send),
                    "回复", replyPendingIntent
                ).addRemoteInput(remoteInput).build()

                // 4. Mark as Read Action
                val readIntent = Intent(ACTION_MARK_READ).apply {
                    setPackage(PackageConstants.PACKAGE_NAME_WECHAT)
                    putExtra("extra_target_wxid", targetWxid)
                }
                val readPendingIntent = PendingIntent.getBroadcast(
                    context, targetWxid.hashCode(), readIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val readAction = Notification.Action.Builder(
                    Icon.createWithResource(context, android.R.drawable.ic_menu_view),
                    "标为已读", readPendingIntent
                ).build()

                // Apply actions directly to the builder
                builder.addAction(replyAction)
                builder.addAction(readAction)
            }
    }

    private val MAP_REGEX = Regex("\\[[^]]+]")

    private val RICH_CONTENT_MAP = mapOf(
        "[图片]" to "\uD83D\uDDBC\uFE0F",
        "[视频]" to "\uD83C\uDFA5",
        "[文件]" to "\uD83D\uDCC1",
        "[语音]" to "\uD83D\uDDE3\uFE0F",
        "[位置]" to "\uD83D\uDDFA\uFE0F",
        "[红包]" to "\uD83E\uDDE7",
        "[转账]" to "\uD83D\uDCB5"
    )

    private fun String.replaceRichContent(): String {
        return MAP_REGEX.replace(this) { matchResult ->
            RICH_CONTENT_MAP[matchResult.value] ?: matchResult.value
        }
    }

    private val EMOJI_MAP = mapOf(
        "[微笑]" to "🙂", "[撇嘴]" to "😕", "[色]" to "😍", "[发呆]" to "😳", "[得意]" to "😎",
        "[流泪]" to "😭", "[害羞]" to "😊", "[闭嘴]" to "🤐", "[睡]" to "😴", "[大哭]" to "😫",
        "[尴尬]" to "😅", "[发怒]" to "😡", "[调皮]" to "😜", "[呲牙]" to "😁", "[惊讶]" to "😱",
        "[难过]" to "🙁", "[囧]" to "😨", "[抓狂]" to "😫", "[吐]" to "🤮", "[偷笑]" to "🤭",
        "[愉快]" to "😊", "[白眼]" to "🙄", "[傲慢]" to "😏", "[困]" to "🥱", "[惊恐]" to "😨",
        "[憨笑]" to "😃", "[悠闲]" to "☕", "[咒骂]" to "🤬", "[疑问]" to "❓", "[嘘]" to "🤫",
        "[晕]" to "😵", "[衰]" to "☹️", "[骷髅]" to "💀", "[敲打]" to "🔨", "[再见]" to "👋",
        "[擦汗]" to "😓", "[抠鼻]" to "👃", "[鼓掌]" to "👏", "[坏笑]" to "😏", "[右哼哼]" to "😒",
        "[鄙视]" to "🙄", "[委屈]" to "🥺", "[快哭了]" to "😭", "[阴险]" to "😈", "[亲亲]" to "😘",
        "[可怜]" to "🥺", "[笑脸]" to "😄", "[生病]" to "😷", "[脸红]" to "😳", "[破涕为笑]" to "😂",
        "[恐惧]" to "😨", "[失望]" to "😞", "[无语]" to "😶", "[嘿哈]" to "🕺", "[捂脸]" to "🤦",
        "[奸笑]" to "😏", "[机智]" to "😏", "[皱眉]" to "😟", "[耶]" to "✌️", "[吃瓜]" to "🍉",
        "[加油]" to "💪", "[汗]" to "😓", "[天啊]" to "😱", "[Emm]" to "🤔", "[社会社会]" to "🤝",
        "[旺柴]" to "\uD83D\uDC36", "[好的]" to "👌", "[打脸]" to "🖐️", "[哇]" to "🤩", "[翻白眼]" to "🙄",
        "[666]" to "🤙", "[让我看看]" to "🫣", "[叹气]" to "😮‍💨", "[苦涩]" to "😭", "[嘴唇]" to "👄",
        "[爱心]" to "❤️", "[心碎]" to "💔", "[拥抱]" to "🤗", "[强]" to "👍", "[弱]" to "👎",
        "[握手]" to "🤝", "[胜利]" to "✌️", "[抱拳]" to "🙏", "[勾引]" to "☝️", "[拳头]" to "👊",
        "[OK]" to "👌", "[合十]" to "🙏", "[啤酒]" to "🍺", "[咖啡]" to "☕", "[蛋糕]" to "🎂",
        "[玫瑰]" to "🌹", "[凋谢]" to "🥀", "[菜刀]" to "🔪", "[炸弹]" to "💣", "[便便]" to "💩",
        "[月亮]" to "🌙", "[太阳]" to "☀️", "[庆祝]" to "🎉", "[礼物]" to "🎁", "[红包]" to "🧧",
        "[發]" to "🀅", "[福]" to "🧧", "[烟花]" to "🎆", "[爆竹]" to "🧨", "[猪头]" to "🐷",
        "[跳跳]" to "💃", "[发抖]" to "🫨", "[转圈]" to "🌀"
    )

    private fun String.replaceEmojis(): String {
        return MAP_REGEX.replace(this) { matchResult ->
            EMOJI_MAP[matchResult.value] ?: matchResult.value
        }
    }

    private fun isGroupChat(wxid: String): Boolean {
        return wxid.endsWith("@chatroom")
    }
}