package com.coworkapp.loopplayer.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 저장된 구간 1개를 표현하는 데이터 클래스
 *
 * @property id       고유 ID (UUID)
 * @property label    사용자가 붙인 이름 (예: "후렴", "1절", "어려운 부분")
 * @property startMs  구간 시작 위치 (밀리초)
 * @property endMs    구간 끝 위치 (밀리초)
 * @property loopCount 반복 횟수 (0 = 무한, 1 이상 = 해당 횟수만큼)
 * @property speed    재생 속도 (1.0 = 보통, 0.5 = 절반, 2.0 = 2배)
 * @property gapMs    한 번 반복 후 다음 반복까지 쉬는 시간 (밀리초)
 */
@Serializable
data class LoopSection(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "구간",
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val loopCount: Int = 0,
    val speed: Float = 1.0f,
    val gapMs: Long = 0L,
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

/**
 * 현재 트랙(곡) 1개 + 그 안의 구간들
 */
@Serializable
data class TrackProfile(
    val uri: String,
    val title: String,
    val sections: List<LoopSection> = emptyList(),
)
