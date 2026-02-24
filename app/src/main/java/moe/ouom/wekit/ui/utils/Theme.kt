package moe.ouom.wekit.ui.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import moe.ouom.wekit.ui.utils.theme.darkScheme
import moe.ouom.wekit.ui.utils.theme.lightScheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) darkScheme else lightScheme,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}