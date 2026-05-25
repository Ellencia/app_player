package com.coworkapp.loopplayer.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaStore.Audio 에서 기기 전체 오디오 인덱싱.
 *
 * 분류 우선순위 (위에서 매칭되면 더 아래 카테고리는 false):
 *  1. 통화녹음: 폴더/파일명에 "call" "통화" 키워드, 또는 CallRecord 류 폴더
 *  2. 음성녹음:
 *      - IS_RECORDING=1 (Android 12+)
 *      - 폴더/파일명에 "recording" "recorder" "voice" "녹음" 키워드
 *      - 폴더 경로에 "Recordings" "Sounds/Voice" 등 표준 녹음 경로
 *      - IS_MUSIC=0 이면서 알림/알람/벨소리도 아닌 경우 (= 기타 음원으로 추정)
 *  3. 음악: IS_MUSIC=1 이고 위 둘 다 아닌 경우
 *
 * 카테고리(category):
 *  - 사용자가 폴더로 분류해놓은 경우만 인식
 *  - RELATIVE_PATH 의 마지막 폴더명을 category 로 추출
 *  - 예: "Recordings/연주/foo.m4a" → folder="Recordings/연주", category="연주"
 *
 * 필터:
 *  - 알림/알람/벨소리 제외
 *  - 길이 1.5초 미만 제외 (시스템음 가능성)
 */
class LibraryRepository(private val context: Context) {

    // 통화녹음 키워드 - 좀 더 명확한 단어들 (오탐 방지)
    private val callKeywords = listOf(
        "call", "callrecord", "callrecorder", "callrecording", "callrec",
        "통화", "통화녹음",
    )

    // 음성녹음 키워드 - 일반 녹음
    private val voiceKeywords = listOf(
        "recording", "recordings", "recorder", "voice", "voicerecorder",
        "녹음", "음성녹음", "voicenote", "voicememo",
    )

    // 메신저 다운로드 경로 — 음성메시지/녹음으로 추정할 수 있는 보조 신호.
    // 메신저 경로 자체로는 음악(친구가 공유한 노래)도 포함될 수 있어
    // duration/아티스트 태그와 함께 휴리스틱으로 사용.
    private val messengerPathHints = listOf(
        "kakaotalk", "kakaotalkdownload",
    )

    // 메신저 음성메시지 길이 상한 가정값.
    // KakaoTalk 음성메시지는 1분 제한이지만 다른 서비스/포워딩 케이스 여유로 5분.
    private val messengerVoiceMaxDurationMs = 5 * 60 * 1000L

    suspend fun loadAllTracks(): List<MusicTrack> = withContext(Dispatchers.IO) {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val baseProjection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.IS_NOTIFICATION,
            MediaStore.Audio.Media.IS_ALARM,
            MediaStore.Audio.Media.IS_RINGTONE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            baseProjection += MediaStore.Audio.Media.RELATIVE_PATH
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            baseProjection += MediaStore.Audio.Media.IS_RECORDING
        }
        val projection = baseProjection.toTypedArray()

        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val args = arrayOf("1500")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val out = ArrayList<MusicTrack>()
        context.contentResolver.query(uri, projection, selection, args, sortOrder)?.use { c ->
            val idCol      = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val nameCol    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistCol  = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durCol     = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val musicCol   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)
            val notifCol   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_NOTIFICATION)
            val alarmCol   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_ALARM)
            val ringCol    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_RINGTONE)
            val pathCol    = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH) else -1
            val recCol     = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                c.getColumnIndex(MediaStore.Audio.Media.IS_RECORDING) else -1

            while (c.moveToNext()) {
                val isMusic = c.getInt(musicCol) != 0
                val isNotif = c.getInt(notifCol) != 0
                val isAlarm = c.getInt(alarmCol) != 0
                val isRing  = c.getInt(ringCol) != 0
                // 알림/알람/벨소리는 라이브러리에서 완전 제외
                if (isNotif || isAlarm || isRing) continue

                val id = c.getLong(idCol)
                val displayName = c.getString(nameCol) ?: ""
                val title = c.getString(titleCol)?.takeIf { it.isNotBlank() }
                    ?: displayName.ifBlank { "이름 없음" }
                val rawArtist = c.getString(artistCol)
                val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>")
                    "알 수 없는 아티스트" else rawArtist
                val duration = c.getLong(durCol)
                val date = c.getLong(dateCol)
                val relPath = if (pathCol >= 0) c.getString(pathCol) ?: "" else ""
                val isRecordingFlag =
                    if (recCol >= 0) c.getInt(recCol) != 0 else false

                // === 분류 ===
                val pathLower = relPath.lowercase()
                val nameLower = (displayName + " " + title).lowercase()

                val matchCall = callKeywords.any { kw ->
                    pathLower.contains(kw) || nameLower.contains(kw)
                }
                val matchVoice = voiceKeywords.any { kw ->
                    pathLower.contains(kw) || nameLower.contains(kw)
                }

                // 메신저(카카오톡 등) 휴리스틱: 경로가 메신저 다운로드면서
                // (아티스트 태그가 비어있거나 5분 미만)이면 음성메시지로 추정.
                // 같은 폴더의 진짜 음악(메타 있고 충분히 김)은 음악으로 남음.
                val fromMessenger = messengerPathHints.any { pathLower.contains(it) }
                val hasNoArtistTag = rawArtist.isNullOrBlank() || rawArtist == "<unknown>"
                val shortDuration = duration in 1..(messengerVoiceMaxDurationMs - 1)
                val messengerVoiceHint =
                    fromMessenger && (hasNoArtistTag || shortDuration)

                // 통화녹음이 음성녹음 키워드와 겹칠 수 있으니 통화를 우선
                val isCall = matchCall
                // 명시적 신호(IS_RECORDING 플래그 또는 키워드)에 메신저 휴리스틱을 더해
                // 녹음으로 인정.
                val isVoice = !isCall && (
                    isRecordingFlag || matchVoice || messengerVoiceHint
                )

                // 알림/알람/벨소리는 위에서 이미 제외했고, 분류 안 된 나머지(IS_MUSIC=0
                // 이지만 키워드 없는 mp3 등)는 음악 탭으로 흡수.

                val folder = relPath.trim('/').takeIf { it.isNotEmpty() } ?: "기타"
                // 카테고리: 폴더 경로의 마지막 세그먼트
                // 예) "Recordings/연주" → "연주"
                //     "Music" → null (단일 표준 폴더라서 카테고리 의미 없음)
                val segments = folder.split('/').filter { it.isNotBlank() }
                val category = if (segments.size >= 2) segments.last() else null

                val trackUri = ContentUris.withAppendedId(uri, id)
                out += MusicTrack(
                    id = id,
                    uri = trackUri.toString(),
                    title = title,
                    artist = artist,
                    durationMs = duration,
                    dateAddedSec = date,
                    folder = folder,
                    category = category,
                    isCallRecording = isCall,
                    isVoiceRecording = isVoice,
                )
            }
        }
        out
    }
}
