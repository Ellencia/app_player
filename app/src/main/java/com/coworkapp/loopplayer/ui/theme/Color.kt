package com.coworkapp.loopplayer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 다크 + 라임 액센트 (Variant E 라이브러리와 동일 톤).
 * 라이브러리의 [com.coworkapp.loopplayer.ui.library.LibraryColors] 와 hex 일치.
 *
 * 라임은 "활성/연습 중" 의미에만 사용 (Play 버튼, 활성 반복 구간, 칩 active 상태).
 * 일반 surface/text 는 모두 다크 그레이/화이트 톤.
 */
object LoopColors {
    // Dark surfaces — 라이브러리 동일
    val Background        = Color(0xFF0D0E0E)
    val Surface           = Color(0xFF131414)
    val SurfaceContainer  = Color(0xFF171818)   // elevated cards
    val SurfaceVariant    = Color(0xFF1E1F1F)   // 한 단계 더 밝은 카드 표면
    val Outline           = Color(0x24FFFFFF)   // 14% white
    val OutlineVariant    = Color(0x14FFFFFF)   // 8% white (faint divider)

    // Text / on-surface
    val Ink               = Color(0xFFECECEA)
    val InkSoft           = Color(0xFF8E9194)
    val InkFaint          = Color(0xFF52555A)

    // Accent — 라임. "활성" 의미.
    val Primary           = Color(0xFFC7E463)
    val OnPrimary         = Color(0xFF0D0E0E)
    val PrimaryContainer  = Color(0x1AC7E463)   // accent 10%
    val OnPrimaryContainer = Color(0xFFC7E463)

    // Secondary = 액센트 보조 톤
    val Secondary         = Color(0xFFC7E463)
    val SecondaryContainer = Color(0x1AC7E463)

    // Tertiary = 화이트톤. "임시 A/B 마커" 같이 활성과 구분돼야 하는 강조에 사용.
    val Tertiary          = Color(0xFFECECEA)
    val OnTertiary        = Color(0xFF0D0E0E)
    val TertiaryContainer = Color(0x14FFFFFF)

    // Waveform semantic
    val WaveUnplayed      = Color(0xFF3A3D40)   // 안 지나간 부분 — 어두운 회색
    val WavePlayed        = Color(0xFF8E9194)   // 지나간 부분 — 중간 회색
    val WaveActive        = Primary
    val WaveTemp          = Tertiary
}
