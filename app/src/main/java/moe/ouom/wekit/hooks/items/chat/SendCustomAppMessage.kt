package moe.ouom.wekit.hooks.items.chat

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.ui.utils.showComposeDialog

@SuppressLint("DiscouragedApi")
@HookItem(path = "聊天增强/发送卡片消息", desc = "长按 '发送' 按钮, 发送 XML 卡片消息")
object SendCustomAppMessage : BaseSwitchFunctionHookItem() {
    // 实现逻辑在 WeChatFooterApi

    override fun onBeforeToggle(newState: Boolean, context: Context): Boolean {
        if (newState) {
            showComposeDialog(context = context) { onDismiss ->
                AlertDialog(
                    onDismissRequest = { onDismiss() },
                    title = { Text(text = "警告") },
                    text = { Text(text = "此功能可能导致账号异常, 确定要启用吗?") },
                    confirmButton = {
                        TextButton(onClick = {
                            applyToggle(true)
                            onDismiss()
                        }) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                    }
                )
            }
            return false
        }

        return true
    }
}
