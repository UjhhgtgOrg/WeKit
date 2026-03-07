package moe.ouom.wekit.ui.utils

object MainActivityLifecycleOwnerProvider {
    val lifecycleOwner by lazy { XposedLifecycleOwner().apply { onCreate(); onResume() } }
}