package moe.ouom.wekit.ui.utils

import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

object NeverDisposeStrategy : ViewCompositionStrategy {
    override fun installFor(view: AbstractComposeView): () -> Unit {
        // We return an empty lambda here.
        // This means when the View system tries to 'uninstall' the strategy
        // (usually during detachment), nothing happens.
        return {}
    }
}