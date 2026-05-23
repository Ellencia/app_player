package com.coworkapp.loopplayer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.coworkapp.loopplayer.data.LoopSection
import com.coworkapp.loopplayer.data.SectionRepository
import com.coworkapp.loopplayer.data.WaveformAnalyzer
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
    private val waveformAnalyzer = WaveformAnalyzer(app)

    /** ExoPlayer 인스턴스 - 액티비티 lifecycle보다 ViewModel이 길어서 여기에 둠 */
    val player: ExoPlayer = ExoPlayer.Builder(app).build().apply {
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.update { it.copy(durationMs = duration.coerceAtLeast(0L)) }
                }
            }
        })
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var positionJob: Job? = null
    private var loopJob: Job? = null

    init {
        startPositionPolling()
    }

    /** 재생 중일 때만 50ms마다 위치 갱신. 일시정지/EOS 상태에서는 idle. */
    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    // 변동이 있을 때만 update (StateFlow가 equal 이면 emit 안 하지만 명시)
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
            player.setMediaItem(MediaItem.fromUri(uri))
            player.playbackParameters = PlaybackParameters(1.0f)
            player.prepare()
            player.play()

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

    // ─────────────────────────────  재생 컨트롤  ─────────────────────────────

    fun togglePlay() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(ms: Long) {
        val clamped = ms.coerceIn(0L, _uiState.value.durationMs.coerceAtLeast(0L))
        player.seekTo(clamped)
        // 일시정지 중이면 polling이 idle이라 UI가 안 따라옴 → 직접 반영
        _uiState.update { it.copy(positionMs = clamped) }
    }

    fun seekRelative(deltaMs: Long) {
        seekTo(player.currentPosition + deltaMs)
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        player.playbackParameters = PlaybackParameters(clamped)
        _uiState.update { it.copy(speed = clamped) }
    }

    // ─────────────────────────  구간 만들기 (A / B 마커)  ─────────────────────────

    /** 지금 위치를 A(시작점)로 잡기 */
    fun markStartHere() {
        _uiState.update { it.copy(tempStartMs = player.currentPosition) }
    }

    /** 지금 위치를 B(끝점)로 잡기 */
    fun markEndHere() {
        _uiState.update { it.copy(tempEndMs = player.currentPosition) }
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
        _uiState.update { it.copy(activeSectionId = section.id, currentLoopIndex = 0) }
        setSpeed(section.speed)
        seekTo(section.startMs)
        player.play()
        loopJob = viewModelScope.launch {
            var loopIdx = 0
            while (true) {
                // endMs 도달까지 대기. 재생속도 반영해서 남은 실제시간을 계산 후
                // 큰 덩어리로 sleep하고, 마지막 ~120ms 윈도우에서만 짧게 폴링.
                while (true) {
                    if (!isStillActive(section.id)) return@launch
                    val pos = player.currentPosition
                    if (pos >= section.endMs) break

                    val speed = player.playbackParameters.speed.coerceAtLeast(0.1f)
                    val remainingPlayMs = ((section.endMs - pos) / speed).toLong()
                    val sleep = when {
                        // 일시정지 중이거나 끝점 직전: 짧게 폴링
                        !player.isPlaying || remainingPlayMs <= 120L -> 30L
                        // 그 외엔 끝점 ~100ms 전까지 한 번에
                        else -> (remainingPlayMs - 100L).coerceAtLeast(30L)
                    }
                    delay(sleep)
                }
                loopIdx += 1
                _uiState.update { it.copy(currentLoopIndex = loopIdx) }

                // 지정 횟수만큼 반복했으면 멈추기 (0 = 무한)
                if (section.loopCount in 1..loopIdx) {
                    player.pause()
                    _uiState.update { it.copy(activeSectionId = null) }
                    return@launch
                }

                // 구간 사이 간격
                if (section.gapMs > 0) {
                    player.pause()
                    delay(section.gapMs)
                    if (!isStillActive(section.id)) return@launch
                    player.play()
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
        player.seekTo(active.startMs)
        _uiState.update { it.copy(currentLoopIndex = 0) }
    }

    fun activeSection(): LoopSection? {
        val id = _uiState.value.activeSectionId ?: return null
        return _uiState.value.sections.firstOrNull { it.id == id }
    }

    override fun onCleared() {
        positionJob?.cancel()
        loopJob?.cancel()
        player.release()
        super.onCleared()
    }
}
