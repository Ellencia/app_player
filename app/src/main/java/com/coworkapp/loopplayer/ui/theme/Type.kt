package com.coworkapp.loopplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 타입 시스템. 시간 표시(분:초.cs)는 항상 monospace + tabular-numerals 느낌으로.
 * 본문은 시스템 sans + Noto Sans KR fallback.
 */
val MonoFamily = FontFamily.Monospace

val LoopTypography = Typography(
    titleLarge  = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall  = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),

    bodyLarge   = TextStyle(fontSize = 16.sp),
    bodyMedium  = TextStyle(fontSize = 14.sp),
    bodySmall   = TextStyle(fontSize = 12.sp),

    labelLarge  = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall  = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

// 큰 시간 표시 — TopBar 옆 카드 안 24sp 모노
val TimeMainStyle = TextStyle(
    fontFamily = MonoFamily,
    fontSize   = 24.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.2).sp,
)

// 보조 시간 표시 (총 길이)
val TimeSubStyle = TextStyle(
    fontFamily = MonoFamily,
    fontSize   = 14.sp,
)

// 구간 카드 안의 시작→끝 표기
val TimeListStyle = TextStyle(
    fontFamily = MonoFamily,
    fontSize   = 12.sp,
)

// 속도 표시
val SpeedDisplayStyle = TextStyle(
    fontFamily = MonoFamily,
    fontSize   = 18.sp,
    fontWeight = FontWeight.Medium,
)

// 라이브러리 트랙·구간 등의 리스트 항목 제목 (Material 의 titleMedium=16보다 1sp 작게)
val ListItemTitleStyle = TextStyle(
    fontSize = 15.sp,
    fontWeight = FontWeight.SemiBold,
)

// 작은 액션 버튼 / 칩 텍스트 (Material 의 bodySmall=12 보다 1sp 큼)
val ActionLabelStyle = TextStyle(
    fontSize = 13.sp,
)

// 속도 프리셋 버튼 (모노 + 13sp)
val SpeedPresetStyle = TextStyle(
    fontFamily = MonoFamily,
    fontSize = 13.sp,
)

// A/B/R 빅 단축 버튼의 큰 라벨
val BigButtonLabelStyle = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold,
)

// A/B/R 빅 단축 버튼의 보조 설명
val BigButtonSubStyle = TextStyle(
    fontSize = 11.sp,
)

// 파형 위 A/B 마커 칩 안 글자
val WaveformMarkerStyle = TextStyle(
    fontSize = 9.sp,
    fontWeight = FontWeight.Bold,
)

// A/B 마커 칩의 값 (모노 12sp)
val MarkerChipValueStyle = TextStyle(
    fontFamily = MonoFamily,
    fontSize = 12.sp,
)
