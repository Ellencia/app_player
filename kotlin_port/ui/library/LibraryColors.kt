package com.coworkapp.loopplayer.ui.library

import androidx.compose.ui.graphics.Color

/**
 * Variant E — Library screen color tokens.
 * 시안 그대로의 hex 값. 다크 전용. 액센트(라임)는 "연습 중" 상태에만.
 */
object LibraryColors {
    val Background        = Color(0xFF0D0E0E)
    val Surface           = Color(0xFF131414)   // drawer panel, sheet bg
    val SurfaceElevated   = Color(0xFF171818)   // sheet bg (slightly higher)
    val Scrim             = Color(0x8C000000)   // 0.55 alpha

    val OnSurface         = Color(0xFFECECEA)
    val OnSurfaceMuted    = Color(0xFF8E9194)   // dim
    val OnSurfaceFaint    = Color(0xFF52555A)   // faint

    val Divider           = Color(0x0DFFFFFF)   // rgba(255,255,255,0.05)
    val DividerStrong     = Color(0x14FFFFFF)   // rgba(255,255,255,0.08)
    val OutlineFaint      = Color(0x24FFFFFF)   // rgba(255,255,255,0.14)

    val Accent            = Color(0xFFC7E463)   // lime — practicing
    val AccentSoft        = Color(0x1AC7E463)   // accent @ 0.10 — backgrounds
    val AccentSofter      = Color(0x09C7E463)   // accent @ 0.035 — active row tint
    val OnAccent          = Color(0xFF0D0E0E)

    val FavStar           = Color(0xE6FFFFFF)

    // Thumbnail gradient — paired with each song's `hue` (HSL).
    // We build the Color in code: Color.hsl(hue, 0.42f, 0.38f) → Color.hsl((hue+24)%360, 0.30f, 0.22f)
}
