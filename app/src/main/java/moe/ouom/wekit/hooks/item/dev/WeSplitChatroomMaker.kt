package moe.ouom.wekit.hooks.item.dev

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import moe.ouom.wekit.config.RuntimeConfig
import moe.ouom.wekit.core.model.BaseClickableFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.api.WeDatabaseApi
import moe.ouom.wekit.hooks.sdk.api.model.WeGroup
import moe.ouom.wekit.util.Initiator.loadClass
import moe.ouom.wekit.util.common.Toasts
import moe.ouom.wekit.util.log.WeLogger

@HookItem(path = "开发者选项/分裂群组", desc = "让群聊一分为二")
class WeSplitChatroomMaker : BaseClickableFunctionHookItem() {

    override fun onClick(context: Context?) {
        context ?: return

        val api = WeDatabaseApi.INSTANCE
        if (api == null) {
            Toasts.showToast(context, "数据库 API 未初始化，请先进入微信主界面")
            return
        }

        try {
            val groups = api.getChatroomList()
            if (groups.isEmpty()) {
                Toasts.showToast(context, "未获取到群聊列表，请确认是否已登录或数据是否同步")
                return
            }

            showSearchDialog(context, groups)
        } catch (e: Exception) {
            WeLogger.e("WeSchemeInvocation", "获取群聊列表失败", e)
            Toasts.showToast(context, "获取数据失败: ${e.message}")
        }
    }

    /**
     * 第一步：显示搜索框
     */
    @SuppressLint("CheckResult")
    private fun showSearchDialog(context: Context, allGroups: List<WeGroup>) {
        MaterialDialog(context).show {
            title(text = "分裂群组 - 搜索")
            input(
                hint = "输入群名 / 拼音 / ID (留空显示全部)",
                allowEmpty = true,
                waitForPositiveButton = true
            ) { dialog, text ->
                val keyword = text.toString().trim()
                filterAndShowList(context, allGroups, keyword)
            }
            positiveButton(text = "查询")
            negativeButton(text = "取消")
        }
    }

    /**
     * 第二步：过滤数据并显示选择列表
     */
    @SuppressLint("CheckResult")
    private fun filterAndShowList(context: Context, allGroups: List<WeGroup>, keyword: String) {
        val filteredList = if (keyword.isEmpty()) {
            allGroups
        } else {
            allGroups.filter { group ->
                group.nickname.contains(keyword, true) ||
                        group.pyInitial.contains(keyword, true) ||
                        group.quanPin.contains(keyword, true) ||
                        group.username.contains(keyword, true)
            }
        }

        if (filteredList.isEmpty()) {
            Toast.makeText(context, "未找到匹配的群聊", Toast.LENGTH_SHORT).show()
            showSearchDialog(context, allGroups)
            return
        }

        val displayItems = filteredList.map {
            val name = it.nickname.ifBlank { "未命名群聊" }
            "$name\n(${it.username})"
        }

        MaterialDialog(context).show {
            title(text = "选择目标群聊 (${filteredList.size})")
            listItems(items = displayItems) { _, index, _ ->
                val selectedGroup = filteredList[index]
                jumpToSplitChatroom(selectedGroup.username)
            }
            negativeButton(text = "返回搜索") {
                showSearchDialog(context, allGroups)
            }
        }
    }

    /**
     * 执行跳转逻辑
     */
    private fun jumpToSplitChatroom(chatroomId: String) {
        try {
            val activity = RuntimeConfig.getLauncherUIActivity()
            if (activity == null) {
                WeLogger.e("WeSchemeInvocation", "LauncherUI Activity is null")
                return
            }

            // 加载 ChattingUI 类
            val chattingUIClass = loadClass("com.tencent.mm.ui.chatting.ChattingUI")
            val intent = Intent(activity, chattingUIClass)

            val rawId = chatroomId.substringBefore("@")
            val targetSplitId = "${rawId}@@chatroom"

            WeLogger.i("WeSchemeInvocation", "Launching ChattingUI for chatroom: $chatroomId")

            intent.putExtra("Chat_User", targetSplitId)
            intent.putExtra("Chat_Mode", 1)

            activity.startActivity(intent)
        } catch (e: Exception) {
            WeLogger.e("WeSchemeInvocation", "跳转失败", e)
        }
    }

    override fun noSwitchWidget(): Boolean = true
}