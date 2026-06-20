package com.coworkapp.loopplayer

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 백그라운드 재생 + 시스템 미디어 컨트롤(알림·잠금화면·이어폰 버튼)을 위한 서비스.
 *
 * ExoPlayer 를 여기서 소유하고 MediaSession 으로 감싼다. UI(PlayerViewModel)는
 * MediaController 로 이 세션에 붙어 같은 Player 인터페이스로 제어한다.
 *
 * media3-session 이 MediaSession 하나만 있으면 표준 미디어 알림과 포그라운드 서비스
 * 승격을 자동 처리하므로 별도 Notification 코드가 거의 필요 없다.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true) // 이어폰 빠지면 일시정지
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
