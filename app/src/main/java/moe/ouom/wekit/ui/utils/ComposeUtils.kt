package moe.ouom.wekit.ui.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.ujhhgtg.nameof.nameof
import moe.ouom.wekit.host.HostInfo
import moe.ouom.wekit.utils.log.WeLogger

private val TAG = nameof(::showComposeDialog)

// useful for showing a compose dialog in non-compose context,
// or when you don't want to manage the state for a dialog inside a composable
//
// note that you should use AlertDialogContent instead of AlertDialog inside 'content' to avoid
// creating multiple windows
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun showComposeDialog(
    context: Context? = null,
    dismissable: Boolean = true,
    content: @Composable (onDismiss: () -> Unit) -> Unit
) {
    var ctx = context

    ctx = if (ctx == null)
        HostInfo.getApplication()
    else
        CommonContextWrapper.createAppCompatContext(ctx)

    val dialog = Dialog(ctx, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth)
    val lifecycleOwner = XposedLifecycleOwner().apply { onCreate(); onResume() }

    dialog.apply {
        window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            requestFeature(Window.FEATURE_NO_TITLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                attributes.blurBehindRadius = 20
            } else {
                WeLogger.w(TAG, "sdk < 31, not applying blur behind dialog")
            }
        }
        setCanceledOnTouchOutside(dismissable)
        setCancelable(dismissable)

        setContentView(
            ComposeView(ctx).apply {
                setLifecycleOwner(lifecycleOwner)

                setContent {
                    AppTheme {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            content(::dismiss)
                        }
                    }
                }
            }
        )

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        setOnDismissListener { lifecycleOwner.onDestroy() }
        show()
    }
}

fun View.setLifecycleOwner(lifecycleOwner: XposedLifecycleOwner) {
    this.apply {
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeViewModelStoreOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)
    }
}