package com.coworkapp.loopplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coworkapp.loopplayer.data.LibraryRepository
import com.coworkapp.loopplayer.data.MusicTrack
import com.coworkapp.loopplayer.data.Playlist
import com.coworkapp.loopplayer.data.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 라이브러리 메인 탭 */
enum class LibraryTab { MUSIC, ARTIST, PLAYLIST, RECORDING }

/** 녹음 탭 안 하위 필터 */
enum class RecordingKind { VOICE, CALL }

/** 아티스트 그룹 1개 — 화면용 파생 모델 */
data class ArtistGroup(
    val artist: String,
    val tracks: List<MusicTrack>,
)

/**
 * 라이브러리 상태.
 */
data class LibraryUiState(
    val loading: Boolean = false,
    val permissionGranted: Boolean = false,
    val tracks: List<MusicTrack> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val query: String = "",
    val tab: LibraryTab = LibraryTab.MUSIC,
    /** 녹음 탭에서 켜진 필터들 (둘 다 켜져있으면 음성+통화 모두 표시) */
    val recordingFilter: Set<RecordingKind> = setOf(RecordingKind.VOICE, RecordingKind.CALL),
    /** 아티스트 탭에서 펼쳐진 아티스트 이름들 */
    val expandedArtists: Set<String> = emptySet(),
    /** 플레이리스트 탭에서 펼쳐진 플레이리스트 id */
    val expandedPlaylistId: String? = null,

    /** 다이얼로그 상태 */
    val showCreatePlaylistDialog: Boolean = false,
    /** 트랙을 플레이리스트에 추가하는 다이얼로그 — 추가할 트랙 (null 이면 닫힘) */
    val addToPlaylistTrack: MusicTrack? = null,
) {
    val musicTracks: List<MusicTrack> get() = tracks.filter {
        !it.isCallRecording && !it.isVoiceRecording
    }
    val voiceTracks: List<MusicTrack> get() = tracks.filter { it.isVoiceRecording }
    val callTracks: List<MusicTrack> get() = tracks.filter { it.isCallRecording }

    /** 현재 탭의 원본 리스트 (검색 적용 전, ARTIST/PLAYLIST 탭은 무관) */
    val currentTabTracks: List<MusicTrack>
        get() = when (tab) {
            LibraryTab.MUSIC -> musicTracks
            LibraryTab.RECORDING -> {
                val out = ArrayList<MusicTrack>()
                if (RecordingKind.VOICE in recordingFilter) out += voiceTracks
                if (RecordingKind.CALL  in recordingFilter) out += callTracks
                // 추가순(최근 우선) 유지
                out.sortedByDescending { it.dateAddedSec }
            }
            else -> emptyList()
        }

    /** MUSIC / RECORDING 탭용 검색 결과 */
    val filtered: List<MusicTrack>
        get() {
            val base = currentTabTracks
            val q = query.trim()
            if (q.isEmpty()) return base
            return base.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.artist.contains(q, ignoreCase = true) ||
                    it.folder.contains(q, ignoreCase = true) ||
                    (it.category?.contains(q, ignoreCase = true) == true)
            }
        }

    /** ARTIST 탭용 — 아티스트별 그룹, 검색 적용 */
    val artistGroups: List<ArtistGroup>
        get() {
            val grouped = musicTracks
                .groupBy { it.artist }
                .toSortedMap(compareBy { it })
                .map { (a, ts) -> ArtistGroup(a, ts.sortedBy { it.title }) }
            val q = query.trim()
            if (q.isEmpty()) return grouped
            return grouped.mapNotNull { g ->
                val filteredTracks = g.tracks.filter {
                    it.title.contains(q, ignoreCase = true) ||
                        it.artist.contains(q, ignoreCase = true)
                }
                if (filteredTracks.isEmpty() && !g.artist.contains(q, ignoreCase = true)) null
                else g.copy(tracks = if (g.artist.contains(q, ignoreCase = true)) g.tracks else filteredTracks)
            }
        }
}

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LibraryRepository(app)
    private val playlistRepo = PlaylistRepository(app)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        // 플레이리스트 변경 관찰
        viewModelScope.launch {
            playlistRepo.observe().collect { list ->
                _uiState.update { it.copy(playlists = list) }
            }
        }
    }

    // ─────────────── 권한·인덱싱 ───────────────
    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted) }
        if (granted && _uiState.value.tracks.isEmpty()) {
            refresh()
        }
    }

    fun refresh() {
        if (!_uiState.value.permissionGranted) return
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            val list = repo.loadAllTracks()
            _uiState.update { it.copy(tracks = list, loading = false) }
        }
    }

    // ─────────────── UI 상태 토글 ───────────────
    fun setQuery(q: String) { _uiState.update { it.copy(query = q) } }
    fun setTab(tab: LibraryTab) {
        // 탭 바꾸면 검색은 지우지 말고 유지. 펼침 상태는 새 탭에 영향 없음.
        _uiState.update { it.copy(tab = tab) }
    }

    fun toggleRecordingFilter(kind: RecordingKind) {
        _uiState.update {
            val next = it.recordingFilter.toMutableSet()
            if (kind in next) next.remove(kind) else next.add(kind)
            // 비어버리면 둘 다 켠 상태로 복귀 (UX: 빈 화면 방지)
            if (next.isEmpty()) {
                it.copy(recordingFilter = setOf(RecordingKind.VOICE, RecordingKind.CALL))
            } else {
                it.copy(recordingFilter = next)
            }
        }
    }

    fun toggleArtistExpanded(artist: String) {
        _uiState.update {
            val next = it.expandedArtists.toMutableSet()
            if (artist in next) next.remove(artist) else next.add(artist)
            it.copy(expandedArtists = next)
        }
    }

    fun togglePlaylistExpanded(playlistId: String) {
        _uiState.update {
            it.copy(
                expandedPlaylistId = if (it.expandedPlaylistId == playlistId) null else playlistId,
            )
        }
    }

    // ─────────────── 플레이리스트 CRUD ───────────────
    fun showCreatePlaylistDialog(show: Boolean) {
        _uiState.update { it.copy(showCreatePlaylistDialog = show) }
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim().ifEmpty {
            "새 플레이리스트 ${_uiState.value.playlists.size + 1}"
        }
        viewModelScope.launch {
            val current = playlistRepo.getAll()
            playlistRepo.saveAll(current + Playlist(name = trimmed))
        }
        _uiState.update { it.copy(showCreatePlaylistDialog = false) }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            val updated = playlistRepo.getAll().filterNot { it.id == id }
            playlistRepo.saveAll(updated)
        }
    }

    fun renamePlaylist(id: String, newName: String) {
        viewModelScope.launch {
            val updated = playlistRepo.getAll().map {
                if (it.id == id) it.copy(name = newName.trim().ifEmpty { it.name })
                else it
            }
            playlistRepo.saveAll(updated)
        }
    }

    /** 트랙을 플레이리스트에 추가 (중복은 제거) */
    fun addTrackToPlaylist(trackUri: String, playlistId: String) {
        viewModelScope.launch {
            val updated = playlistRepo.getAll().map { pl ->
                if (pl.id == playlistId && trackUri !in pl.trackUris)
                    pl.copy(trackUris = pl.trackUris + trackUri)
                else pl
            }
            playlistRepo.saveAll(updated)
        }
        _uiState.update { it.copy(addToPlaylistTrack = null) }
    }

    fun removeTrackFromPlaylist(trackUri: String, playlistId: String) {
        viewModelScope.launch {
            val updated = playlistRepo.getAll().map { pl ->
                if (pl.id == playlistId)
                    pl.copy(trackUris = pl.trackUris - trackUri)
                else pl
            }
            playlistRepo.saveAll(updated)
        }
    }

    /** "이 트랙을 어느 플레이리스트에 추가할까" 다이얼로그 열기 */
    fun openAddToPlaylistDialog(track: MusicTrack?) {
        _uiState.update { it.copy(addToPlaylistTrack = track) }
    }

    /** 플레이리스트 안의 trackUri 들을 실제 트랙 객체로 매핑 (없어진 건 null) */
    fun resolvePlaylistTracks(playlist: Playlist): List<MusicTrack> {
        val map = _uiState.value.tracks.associateBy { it.uri }
        return playlist.trackUris.mapNotNull { map[it] }
    }
}
