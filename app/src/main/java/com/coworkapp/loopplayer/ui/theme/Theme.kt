package com.coworkapp.loopplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * 다크 + 라임 단일 테마. 라이트 모드는 지원하지 않음 (라이브러리 디자인이 다크 전용).
 *
 * Player·Library 양쪽이 같은 [LoopColors] 팔레트를 공유:
 *  - Player 는 MaterialTheme.colorScheme 경유 (자동 반영)
 *  - Library 는 [com.coworkapp.loopplayer.ui.library.LibraryColors] 직접 참조 (동일 hex)
 */
private val DarkColors = darkColorScheme(
    background         = LoopColors.Background,
    surface            = LoopColors.Surface,
    surfaceVariant     = LoopColors.SurfaceVariant,
    onSurface          = LoopColors.Ink,
    onSurfaceVariant   = LoopColors.InkSoft,
    outline            = LoopColors.Outline,
    outlineVariant     = LoopColors.OutlineVariant,

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

@Composable
fun LoopPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = LoopTypography,
        content     = content,
    )
}
