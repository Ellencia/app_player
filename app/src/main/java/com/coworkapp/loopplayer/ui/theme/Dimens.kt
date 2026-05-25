package com.coworkapp.loopplayer.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 치수 토큰. 디자인 관련 픽셀값을 한 곳에서 관리.
 *
 * IconSize: 아이콘(글리프) 자체 크기
 * AvatarSize: 아이콘을 감싸는 원/사각 배지
 * Surface: 큰 표면 (Play 버튼, 빈 상태 일러스트)
 * Component: 행/카드 컴포넌트 높이
 */
object LoopDimens {
    object IconSize {
        val Tiny    = 9.dp    // 파형 위 A/B 마커 글리프
        val Small   = 16.dp   // 인라인 액션 아이콘
        val Medium  = 18.dp   // 보조 행의 아이콘
        val Default = 20.dp   // 일반 아이콘 (transport, badge 안 등)
        val Badge   = 22.dp   // 배지 원 안 글리프
        val Display = 22.dp   // 메인 Play 아이콘 (45dp 버튼 안, 70% 축소)
    }

    object AvatarSize {
        val Small   = 36.dp   // 구간 행 아이콘 배지
        val Default = 40.dp   // 라이브러리 트랙·아티스트 배지
    }

    object Surface {
        val PlayButton  = 45.dp   // 메인 Play 토글 버튼 (기존 64dp의 70%)
        val EmptySmall  = 64.dp   // 작은 빈 상태 아이콘
        val EmptyMedium = 72.dp   // 중간 빈 상태 아이콘
        val EmptyLarge  = 96.dp   // 큰 빈 상태 아이콘
    }

    object Component {
        val TransportButton = 36.dp  // ±5초 버튼 (기존 52dp의 70%)
        val BigButton       = 56.dp  // A/B/R 빅 단축 버튼 행
        val Waveform        = 88.dp  // 파형 프로그레스 바
        val TouchTarget     = 48.dp  // 슬라이더 등 최소 터치 영역
    }
}
