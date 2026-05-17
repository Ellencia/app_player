package com.coworkapp.loopplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    background         = LoopColors.Background,
    surface            = LoopColors.Surface,
    surfaceVariant     = LoopColors.SurfaceContainer,
    onSurface          = LoopColors.Ink,
    onSurfaceVariant   = LoopColors.InkSoft,
    outline            = LoopColors.Outline,
    outlineVariant     = LoopColors.SurfaceVariant,

    primary            = LoopColors.Primary,
    onPrimary          = LoopColors.OnPrimary,
    primaryContainer   = LoopColors.PrimaryContainer,
    onPrimaryContainer = LoopColors.OnPrimaryContainer,

    secondary          = LoopColors.Secondary,
    secondaryContainer = LoopColors.SecondaryContainer,

    tertiary           = LoopColors.Tertiary,
    onTertiary         = LoopColors.OnTertiary,
    tertiaryContainer  = LoopColors.TertiaryContainer,
)

// 다크: 라이트와 같은 톤을 유지하되 명도만 반전
private val DarkColors = darkColorScheme(
    background         = androidx.compose.ui.graphics.Color(0xFF141218),
    surface            = androidx.compose.ui.graphics.Color(0xFF1D1B20),
    surfaceVariant     = androidx.compose.ui.graphics.Color(0xFF2B2930),
    onSurface          = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    onSurfaceVariant   = androidx.compose.ui.graphics.Color(0xFFCAC4D0),

    primary            = androidx.compose.ui.graphics.Color(0xFFD0BCFF),
    onPrimary          = androidx.compose.ui.graphics.Color(0xFF381E72),
    primaryContainer   = androidx.compose.ui.graphics.Color(0xFF4F378B),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFEADDFF),

    secondary          = androidx.compose.ui.graphics.Color(0xFFCCC2DC),
    tertiary           = androidx.compose.ui.graphics.Color(0xFFEFB8C8),
    tertiaryContainer  = androidx.compose.ui.graphics.Color(0xFF633B48),
)

@Composable
fun LoopPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = LoopTypography,
        content     = content,
    )
}
