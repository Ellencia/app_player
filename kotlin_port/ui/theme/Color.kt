package com.coworkapp.loopplayer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Variant A — Polished Material 3 Purple
 * 디자인 시안 기준 색상값. 라이트/다크 둘 다 같은 톤 유지.
 */
object LoopColors {
    // Light
    val Background        = Color(0xFFFEF7FF)
    val Surface           = Color(0xFFFFFFFF)
    val SurfaceContainer  = Color(0xFFF3EDF7)
    val SurfaceVariant    = Color(0xFFE7E0EC)
    val Outline           = Color(0xFFCAC4D0)

    val Primary           = Color(0xFF6750A4)
    val OnPrimary         = Color(0xFFFFFFFF)
    val PrimaryContainer  = Color(0xFFEADDFF)
    val OnPrimaryContainer = Color(0xFF21005D)

    val Secondary         = Color(0xFF625B71)
    val SecondaryContainer = Color(0xFFE8DEF8)

    val Tertiary          = Color(0xFF7D5260)
    val OnTertiary        = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFFFD8E4)

    val Ink               = Color(0xFF1D1B20)
    val InkSoft           = Color(0xFF49454F)
    val InkFaint          = Color(0xFF79747E)

    // Waveform semantic
    val WaveUnplayed      = Color(0xFFCAC4D0)
    val WavePlayed        = Color(0xFF49454F)
    val WaveActive        = Primary
    val WaveTemp          = Tertiary
}
