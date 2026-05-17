package com.coworkapp.loopplayer.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 사용자 정의 플레이리스트 1개.
 *
 * - trackUris: MediaStore content:// URI 문자열 리스트.
 *   라이브러리 재스캔 후에도 URI 가 유지되므로 재매칭 시 그대로 살아남는다.
 *   인덱싱 후 매칭 실패한 URI 는 화면에서 회색 처리/숨김 처리 가능.
 *
 * @property id          고유 ID (UUID)
 * @property name        사용자 지정 이름
 * @property trackUris   순서 보존된 트랙 URI 목록
 * @property createdAtSec 생성 시각 (epoch sec)
 */
@Serializable
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "새 플레이리스트",
    val trackUris: List<String> = emptyList(),
    val createdAtSec: Long = System.currentTimeMillis() / 1000L,
)
