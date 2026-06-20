package com.coworkapp.loopplayer

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.coworkapp.loopplayer.data.LoopSection
import com.coworkapp.loopplayer.data.SectionRepository
import com.coworkapp.loopplayer.data.TrackMetadataRepository
import com.coworkapp.loopplayer.data.WaveformAnalyzer
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI 상태
 */
data class PlayerUiState(
    val trackUri: String? = null,
    val trackTitle: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val sections: List<LoopSection> = emptyList(),
    val activeSectionId: String? = null,
    /** A/B 마커 (구간 만들기 중) */
    val tempStartMs: Long? = null,
    val tempEndMs: Long? = null,
    /** 현재 활성 구간이 몇 번째 반복인지 (0부터) */
    val currentLoopIndex: Int = 0,
    /** 파형 데이터 - 0.0~1.0으로 정규화된 진폭 배열. 비어있으면 아직 분석중 또는 실패. */
    val waveform: List<Float> = emptyList(),
    val waveformLoading: Boolean = false,
)

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SectionRepository(app)
    private val metaRepo = TrackMetadataRepository(app)
    private val waveformAnalyzer = WaveformAnalyzer(app)

    /**
     * 실제 재생은 PlaybackService(MediaSessionService) 안의 ExoPlayer 가 담당.
     * 여기서는 MediaController 로 그 세션에 비동기 연결해서 제어한다.
     * 연결 전(controller == null)에는 openTrack 요청을 pendingTrack 에 보관했다가
     * 연결되면 적용.
     */
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingTrack: Pair<Uri, String>? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _uiState.update {
                    it.copy(durationMs = (controller?.duration ?: 0L).coerceAtLeast(0L))
                }
            }
        }
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var positionJob: Job? = null
    private var loopJob: Job? = null

    init {
        connectToService()
        startPositionPolling()
    }

    private fun connectToService() {
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(playerListener)
            // 연결 전에 들어온 트랙 요청이 있으면 지금 적용
            pendingTrack?.let { (uri, name) ->
                pendingTrack = null
                applyTrack(uri, name)
            }
        }, ContextCompat.getMainExecutor(app))
    }

    /** 재생 중일 때만 50ms마다 위치 갱신. 일시정지/EOS 상태에서는 idle. */
    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (true) {
                val c = controller
                if (c != null && c.isPlaying) {
                    val pos = c.currentPosition.coerceAtLeast(0L)
                    if (pos != _uiState.value.positionMs) {
                        _uiState.update { it.copy(positionMs = pos) }
                    }
                }
                delay(50)
            }
        }
    }

    // ─────────────────────────────  파일 열기  ─────────────────────────────

    /**
     * 사용자가 mp3 파일을 고르면 호출됨
     */
    fun openTrack(uri: Uri, displayName: String) {
        viewModelScope.launch {
            val uriStr = uri.toString()
            val sections = repo.getSections(uriStr)
            _uiState.update {
                PlayerUiState(
                    trackUri = uriStr,
                    trackTitle = displayName,
                    sections = sections,
                    speed = 1.0f,
                    waveformLoading = true,
                )
            }

            val c = controller
            if (c == null) {
                // 아직 서비스 연결 전 → 연결되면 적용하도록 보관
                pendingTrack = uri to displayName
            } else {
                applyTrack(uri, displayName)
            }

            // 마지막 연습 시각 갱신 (라이브러리에서 "최근 연습" 정렬 / 메타 표시용)
            launch(Dispatchers.IO) { metaRepo.touchPracticed(uriStr) }

            // 백그라운드에서 파형 분석. 트랙 빠르게 바꾸면 이전 작업 결과는 무시되어야 함
            // → 현재 trackUri와 결과 도착 시점의 trackUri 비교로 가드
            launch(Dispatchers.IO) {
                val waveform = waveformAnalyzer.analyze(uri)
                if (_uiState.value.trackUri == uriStr) {
                    _uiState.update { it.copy(waveform = waveform, waveformLoading = false) }
                }
            }
        }
    }

    /** controller 가 준비된 뒤 실제로 트랙을 로드. 알림 표시용 메타데이터 포함. */
    private fun applyTrack(uri: Uri, displayName: String) {
        val c = controller ?: return
        val item = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(displayName)
                    .setArtist("구간반복 플레이어")
                    .build()
            )
            .build()
        c.setMediaItem(item)
        c.playbackParameters = PlaybackParameters(1.0f)
        c.prepare()
        c.play()
    }

    // ─────────────────────────────  재생 컨트롤  ─────────────────────────────

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(ms: Long) {
        val c = controller ?: return
        val clamped = ms.coerceIn(0L, _uiState.value.durationMs.coerceAtLeast(0L))
        c.seekTo(clamped)
        // 일시정지 중이면 polling이 idle이라 UI가 안 따라옴 → 직접 반영
        _uiState.update { it.copy(positionMs = clamped) }
    }

    fun seekRelative(deltaMs: Long) {
        val c = controller ?: return
        seekTo(c.currentPosition + deltaMs)
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        controller?.playbackParameters = PlaybackParameters(clamped)
        _uiState.update { it.copy(speed = clamped) }
    }

    // ─────────────────────────  구간 만들기 (A / B 마커)  ─────────────────────────

    /** 지금 위치를 A(시작점)로 잡기 */
    fun markStartHere() {
        val c = controller ?: return
        _uiState.update { it.copy(tempStartMs = c.currentPosition) }
    }

    /** 지금 위치를 B(끝점)로 잡기 */
    fun markEndHere() {
        val c = controller ?: return
        _uiState.update { it.copy(tempEndMs = c.currentPosition) }
    }

    fun clearTempMarkers() {
        _uiState.update { it.copy(tempStartMs = null, tempEndMs = null) }
    }

    /**
     * 현재 임시 A/B로 잡힌 구간을 정식 LoopSection으로 저장
     * label이 비어 있으면 자동으로 "구간 N"으로 채움
     */
    fun saveTempAsSection(label: String? = null) {
        val s = _uiState.value
        val start = s.tempStartMs ?: return
        val end = s.tempEndMs ?: return
        val safeStart = minOf(start, end)
        val safeEnd = maxOf(start, end)
        val name = label?.takeIf { it.isNotBlank() }
            ?: "구간 ${s.sections.size + 1}"
        val newSection = LoopSection(
            label = name,
            startMs = safeStart,
            endMs = safeEnd,
            loopCount = 0,
            speed = s.speed,
            gapMs = 0L,
        )
        val updated = s.sections + newSection
        _uiState.update { it.copy(sections = updated, tempStartMs = null, tempEndMs = null) }
        persistSections(updated)
    }

    // ─────────────────────────────  구간 편집  ─────────────────────────────

    fun updateSection(updated: LoopSection) {
        val list = _uiState.value.sections.map { if (it.id == updated.id) updated else it }
        _uiState.update { it.copy(sections = list) }
        persistSections(list)
    }

    fun deleteSection(id: String) {
        val list = _uiState.value.sections.filterNot { it.id == id }
        val activeId = _uiState.value.activeSectionId
        _uiState.update {
            it.copy(
                sections = list,
                activeSectionId = if (activeId == id) null else activeId
            )
        }
        if (activeId == id) stopLoop()
        persistSections(list)
    }

    /** 스와이프 삭제 후 Undo 흐름용 - 원래 인덱스 자리에 다시 끼워넣기. */
    fun restoreSection(section: LoopSection, atIndex: Int) {
        val cur = _uiState.value.sections
        if (cur.any { it.id == section.id }) return // 이미 있으면 무시
        val safeIdx = atIndex.coerceIn(0, cur.size)
        val list = cur.toMutableList().apply { add(safeIdx, section) }
        _uiState.update { it.copy(sections = list) }
        persistSections(list)
    }

    private fun persistSections(list: List<LoopSection>) {
        val uri = _uiState.value.trackUri ?: return
        viewModelScope.launch { repo.saveSections(uri, list) }
    }

    // ─────────────────────────────  구간 반복 실행  ─────────────────────────────

    /**
     * 구간 선택 → 즉시 그 구간으로 점프 + 반복 시작
     */
    fun startLoop(section: LoopSection) {
        stopLoop()
        val c = controller ?: return
        _uiState.update { it.copy(activeSectionId = section.id, currentLoopIndex = 0) }
        setSpeed(section.speed)
        seekTo(section.startMs)
        c.play()
        loopJob = viewModelScope.launch {
            var loopIdx = 0
            while (true) {
                // endMs 도달까지 대기. 재생속도 반영해서 남은 실제시간을 계산 후
                // 큰 덩어리로 sleep하고, 마지막 ~120ms 윈도우에서만 짧게 폴링.
                while (true) {
                    if (!isStillActive(section.id)) return@launch
                    val pos = c.currentPosition
                    if (pos >= section.endMs) break

                    val speed = c.playbackParameters.speed.coerceAtLeast(0.1f)
                    val remainingPlayMs = ((section.endMs - pos) / speed).toLong()
                    val sleep = when {
                        // 일시정지 중이거나 끝점 직전: 짧게 폴링
                        !c.isPlaying || remainingPlayMs <= 120L -> 30L
                        // 그 외엔 끝점 ~100ms 전까지 한 번에
                        else -> (remainingPlayMs - 100L).coerceAtLeast(30L)
                    }
                    delay(sleep)
                }
                loopIdx += 1
                _uiState.update { it.copy(currentLoopIndex = loopIdx) }

                // 트랙별 누적 반복 카운트 +1 (라이브러리에서 표시)
                val curUri = _uiState.value.trackUri
                if (curUri != null) {
                    launch(Dispatchers.IO) { metaRepo.incrementLoops(curUri, 1) }
                }

                // 지정 횟수만큼 반복했으면 멈추기 (0 = 무한)
                if (section.loopCount in 1..loopIdx) {
                    c.pause()
                    _uiState.update { it.copy(activeSectionId = null) }
                    return@launch
                }

                // 구간 사이 간격
                if (section.gapMs > 0) {
                    c.pause()
                    delay(section.gapMs)
                    if (!isStillActive(section.id)) return@launch
                    c.play()
                }
                seekTo(section.startMs)
            }
        }
    }

    private fun isStillActive(id: String): Boolean =
        _uiState.value.activeSectionId == id

    fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
        _uiState.update { it.copy(activeSectionId = null, currentLoopIndex = 0) }
    }

    /** 활성화된 구간이 있으면 그 안에서 다시 처음으로 (R 버튼) */
    fun restartActiveSection() {
        val active = activeSection() ?: return
        controller?.seekTo(active.startMs)
        _uiState.update { it.copy(currentLoopIndex = 0) }
    }

    fun activeSection(): LoopSection? {
        val id = _uiState.value.activeSectionId ?: return null
        return _uiState.value.sections.firstOrNull { it.id == id }
    }

    override fun onCleared() {
        positionJob?.cancel()
        loopJob?.cancel()
        controller?.removeListener(playerListener)
        // MediaController 만 해제. 실제 ExoPlayer 와 재생은 서비스가 계속 소유하므로
        // 화면을 떠나도(앱 백그라운드) 음악은 이어진다.
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }
}
