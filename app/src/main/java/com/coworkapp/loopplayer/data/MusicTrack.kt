package com.coworkapp.loopplayer.data

/**
 * 라이브러리 인덱싱 결과로 표시되는 트랙 1개.
 *
 * 분류 규칙 (3가지 카테고리, 우선순위 순):
 *  1. isCallRecording  — 통화녹음 폴더/키워드 매칭
 *  2. isVoiceRecording — 음성녹음 폴더/플래그/키워드 매칭 (통화 아님)
 *  3. 음악             — 위 둘 다 아니고 IS_MUSIC=1
 *
 * @property id          MediaStore._ID
 * @property uri         content://media/external/audio/media/{id}
 * @property title       DISPLAY_NAME or TITLE
 * @property artist      ARTIST (없으면 "알 수 없는 아티스트")
 * @property durationMs  DURATION
 * @property dateAddedSec MediaStore 가 기록한 추가 시각 (epoch seconds)
 * @property folder      RELATIVE_PATH 또는 파일 경로의 상위 폴더명 — UI 그룹화 키
 * @property category    폴더 경로 마지막 세그먼트 — 사용자가 만든 하위 분류 (예: "연주")
 * @property isCallRecording 통화녹음 여부
 * @property isVoiceRecording 음성녹음(통화 아님) 여부
 */
data class MusicTrack(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val dateAddedSec: Long,
    val folder: String,
    val category: String?,
    val isCallRecording: Boolean,
    val isVoiceRecording: Boolean,
)
