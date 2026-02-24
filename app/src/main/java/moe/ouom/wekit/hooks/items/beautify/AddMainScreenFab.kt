package moe.ouom.wekit.hooks.items.beautify

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Process
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import moe.ouom.wekit.constants.PackageConstants
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.base.WeConversationApi
import moe.ouom.wekit.ui.utils.XposedLifecycleOwner
import moe.ouom.wekit.utils.common.ToastUtils
import moe.ouom.wekit.utils.log.WeLogger

@HookItem(path = "美化/主屏幕添加 FAB", desc = "向应用主屏幕添加浮动操作按钮")
object AddMainScreenFab : BaseSwitchFunctionHookItem() {

    private const val TAG = "AddMainScreenFab"

    private fun startActivityByName(context: Context, className: String) {
        val intent = Intent().apply {
            setClassName(PackageConstants.PACKAGE_NAME_WECHAT, className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun entry(classLoader: ClassLoader) {
        WeMainActivityBeautifyApi.methodDoOnCreate.toDexMethod {
            hook {
                afterIfEnabled { param ->
                    val activity = param.thisObject.asResolver()
                        .firstField {
                            type = "com.tencent.mm.ui.MMFragmentActivity"
                        }
                        .get()!! as Activity

                    val menuItems = mapOf(
                        "视频号" to (Icons.Default.Movie to {
                            startActivityByName(activity,
                                "com.tencent.mm.plugin.finder.ui.FinderHomeAffinityUI")
                        }),
                        "扫一扫" to (Icons.Default.QrCodeScanner to {
                            startActivityByName(activity,
                                "com.tencent.mm.plugin.scanner.ui.BaseScanUI")
                        }),
                        "朋友圈" to (Icons.Default.Camera to {
                            startActivityByName(activity,
                                "com.tencent.mm.plugin.sns.ui.improve.ImproveSnsTimelineUI")
                        }),
                        "钱包" to (Icons.Default.Wallet to {
                            startActivityByName(activity,
                                "com.tencent.mm.plugin.mall.ui.MallIndexUIv2")
                        }),
                        "设置" to (Icons.Default.Settings to {
                            startActivityByName(activity,
                                "com.tencent.mm.plugin.setting.ui.setting_new.MainSettingsUI")
                        }),
                        "强制停止" to (Icons.Default.Cancel to {
                            Process.killProcess(Process.myPid())
                        }),
                        "全部已读" to (Icons.Default.Update to {
                            WeConversationApi.clearUnreadCounts()
                            ToastUtils.showToast("已将全部未读消息标为已读")
                        }))

                    val rootView = activity.findViewById<ViewGroup>(android.R.id.content)

                    val lifecycleOwner = XposedLifecycleOwner().apply { onCreate(); onResume() }
                    val decorView = activity.window.decorView

                    // Compose traverse up the view hierarchy to find a LifecycleOwner from the root or parent views
                    decorView.setViewTreeLifecycleOwner(lifecycleOwner)
                    decorView.setViewTreeViewModelStoreOwner(lifecycleOwner)
                    decorView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                    rootView.setViewTreeLifecycleOwner(lifecycleOwner)
                    rootView.setViewTreeViewModelStoreOwner(lifecycleOwner)
                    rootView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

                    WeLogger.i(TAG, "injected compose fab into root view")
                    rootView.addView(
                        ComposeView(activity).apply {
                            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)

                            setViewTreeLifecycleOwner(lifecycleOwner)
                            setViewTreeViewModelStoreOwner(lifecycleOwner)
                            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

                            setContent {
                                // WeChat doesn't follow MaterialTheme so we don't use that too
                                // or else different color palettes clash and it's hideous
                                val isDark = isSystemInDarkTheme()
                                val backgroundColor = if (isDark) Color(0xFF191919) else Color(0xFFF7F7F7)
                                val activeColor = Color(0xFF07C160)

                                var expanded by remember { mutableStateOf(false) }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 60.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // 1. Expandable Menu Items
                                        AnimatedVisibility(
                                            visible = expanded,
                                            enter = expandVertically(),
                                            exit = shrinkVertically()
                                        ) {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                menuItems.forEach { (name, pair) ->
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        // The Floating Label
                                                        AnimatedVisibility(
                                                            visible = expanded,
                                                            enter = expandHorizontally(expandFrom = Alignment.End),
                                                            exit = shrinkHorizontally(shrinkTowards = Alignment.End)
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = backgroundColor,
                                                                tonalElevation = 2.dp,
                                                                shadowElevation = 2.dp
                                                            ) {
                                                                Text(
                                                                    text = name,
                                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                                    color = activeColor,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            }
                                                        }

                                                        // The Small FAB
                                                        SmallFloatingActionButton(
                                                            onClick = {
                                                                pair.second()
                                                                expanded = false
                                                            },
                                                            containerColor = backgroundColor,
                                                            shape = CircleShape,
                                                            elevation = FloatingActionButtonDefaults.elevation(2.dp)
                                                        ) {
                                                            Icon(pair.first, contentDescription = null, tint = activeColor)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 2. Main Toggle FAB (remains the same)
                                        FloatingActionButton(
                                            onClick = { expanded = !expanded },
                                            containerColor = backgroundColor,
                                            shape = CircleShape
                                        ) {
                                            val rotation by animateFloatAsState(if (expanded) 45f else 0f)
                                            Icon(
                                                Icons.Filled.Add,
                                                contentDescription = null,
                                                tint = activeColor,
                                                modifier = Modifier.rotate(rotation)
                                            )
                                        }
                                    }
                                }
                            }
                        })
                }
            }
        }
    }
}