package com.coworkapp.loopplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * 모서리 둥글기 토큰. 역할별 이름을 가짐 (값이 비슷해도 의미가 다르면 분리).
 *
 * - Pill: 칩, 알약 버튼
 * - SubRow: 펼침 그룹 안 작은 항목 (Artist/Playlist drawer)
 * - PlaylistBadge: 플레이리스트 아이콘 사각 배지
 * - Card: 일반 리스트 카드 (TrackRow, ArtistGroupCard)
 * - MediumCard: 더 큰 카드 (SectionRow, PlaylistCard, 스와이프 배경)
 * - Button: A/B/R 빅 단축 버튼
 * - FeatureCard: 강조 카드 (속도 카드, 메인 Play 버튼)
 * - HeroCard: 최상단 핵심 카드 (파형 카드)
 */
object LoopShapes {
    val Pill         = RoundedCornerShape(100.dp)
    val SubRow       = RoundedCornerShape(10.dp)
    val PlaylistBadge = RoundedCornerShape(12.dp)
    val Card         = RoundedCornerShape(14.dp)
    val MediumCard   = RoundedCornerShape(16.dp)
    val Button       = RoundedCornerShape(18.dp)
    val FeatureCard  = RoundedCornerShape(20.dp)
    val HeroCard     = RoundedCornerShape(24.dp)
}
