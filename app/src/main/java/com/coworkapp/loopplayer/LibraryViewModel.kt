package com.coworkapp.loopplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coworkapp.loopplayer.data.LibraryRepository
import com.coworkapp.loopplayer.data.MusicTrack
import com.coworkapp.loopplayer.data.SectionRepository
import com.coworkapp.loopplayer.data.TrackMetadata
import com.coworkapp.loopplayer.data.TrackMetadataRepository
import com.coworkapp.loopplayer.ui.library.LibraryChip
import com.coworkapp.loopplayer.ui.library.LibraryFolder
import com.coworkapp.loopplayer.ui.library.LibraryGroup
import com.coworkapp.loopplayer.ui.library.LibraryQuickFilter
import com.coworkapp.loopplayer.ui.library.LibrarySong
import com.coworkapp.loopplayer.ui.library.LibrarySort
import com.coworkapp.loopplayer.ui.library.LibraryUiState
import com.coworkapp.loopplayer.ui.library.LibraryViewOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Variant E 라이브러리 ViewModel.
 *
 * 데이터 소스:
 *  - LibraryRepository: MediaStore.Audio 인덱싱 → List<MusicTrack>
 *  - SectionRepository.getAllSectionCounts(): 트랙별 저장된 구간 개수
 *
 * 매핑: MusicTrack + sectionCount → LibrarySong
 *  - lastPracticed / loops / bpm / musicKey 는 우리 앱이 아직 추적하지 않아 null/0
 *  - hueDeg 는 artist 문자열 해시로 산출 (안정적 색상)
 *  - isActive = sectionCount > 0 (저장된 구간이 있는 트랙을 "연습 중"으로 간주)
 *  - favorite = false (별도 저장소 필요, 후속 작업)
 *
 * sort/group/filter/selectedChip 변경 시 재정렬·재필터링은 항상 raw tracks 기준으로
 * 즉시 계산해서 새 state 발행.
 */
class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LibraryRepository(app)
    private val sectionRepo = SectionRepository(app)
    private val metaRepo = TrackMetadataRepository(app)

    /** 원본 트랙 + sectionCount + metadata. UI 가공 전 raw 캐시. */
    private var rawTracks: List<MusicTrack> = emptyList()
    private var sectionCounts: Map<String, Int> = emptyMap()
    private var metadata: Map<String, TrackMetadata> = emptyMap()

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        // 메타데이터(즐겨찾기·loops·lastPracticed)는 PlayerViewModel·UI 양쪽이 write 하므로
        // Flow 로 관찰해서 변경 시마다 자동 재계산.
        viewModelScope.launch {
            metaRepo.observeAll().collect { map ->
                metadata = map
                if (rawTracks.isNotEmpty()) recompute()
            }
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted) }
        if (granted && rawTracks.isEmpty()) refresh()
    }

    fun refresh() {
        if (!_uiState.value.permissionGranted) return
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            rawTracks = repo.loadAllTracks()
            sectionCounts = sectionRepo.getAllSectionCounts()
            recompute()
            _uiState.update { it.copy(loading = false) }
        }
    }

    // ─────────────── 사용자 액션 ───────────────
    fun setSort(sort: LibrarySort) {
        _uiState.update { it.copy(sort = sort) }
        recompute()
    }

    fun setGroup(group: LibraryGroup) {
        _uiState.update { it.copy(group = group) }
        recompute()
    }

    fun toggleFilter(filter: LibraryQuickFilter) {
        _uiState.update {
            val cur = it.filters
            it.copy(filters = if (filter in cur) cur - filter else cur + filter)
        }
        recompute()
    }

    fun setViewOptions(opts: LibraryViewOptions) {
        _uiState.update { it.copy(viewOptions = opts) }
        // 시각 토글만 바뀌므로 재계산 불필요
    }

    fun selectChip(chip: String) {
        _uiState.update { it.copy(selectedChip = chip) }
        recompute()
    }

    fun resetSheet() {
        _uiState.update {
            it.copy(
                sort = LibrarySort.RecentPractice,
                sortDescending = true,
                group = LibraryGroup.None,
                filters = emptySet(),
                viewOptions = LibraryViewOptions(),
            )
        }
        recompute()
    }

    // ─────────────── 메타데이터 / 검색 ───────────────

    fun toggleFavorite(trackUri: String) {
        val curFav = metadata[trackUri]?.favorite ?: false
        viewModelScope.launch { metaRepo.setFavorite(trackUri, !curFav) }
        // 실제 반영은 metaRepo Flow 가 emit 하면 init 의 collect 로 자동 recompute
    }

    fun enterSearch() {
        _uiState.update { it.copy(searchMode = true) }
    }

    fun exitSearch() {
        _uiState.update { it.copy(searchMode = false, query = "") }
        recompute()
    }

    fun setQuery(q: String) {
        _uiState.update { it.copy(query = q) }
        recompute()
    }

    // ─────────────── 핵심: 정렬·필터·칩 적용 후 LibrarySong 리스트 생성 ───────────────
    private fun recompute() {
        val allSongs = rawTracks.map {
            it.toLibrarySong(
                sectionCount = sectionCounts[it.uri] ?: 0,
                meta = metadata[it.uri],
            )
        }
        val chips = buildChips(allSongs)
        val folders = buildFolders(allSongs)
        val filtered = applyChipAndFilters(allSongs)
        val sorted = applySort(filtered)
        _uiState.update {
            it.copy(
                songs = sorted,
                totalCount = allSongs.size,
                totalLoops = allSongs.sumOf { s -> s.loops },
                totalMinutes = (allSongs.sumOf { s -> s.durationMs } / 60_000L).toInt(),
                activeCount = allSongs.count { s -> s.isActive },
                recordingCount = allSongs.count { s -> s.isRecording },
                favoriteCount = allSongs.count { s -> s.favorite },
                chips = chips,
                folders = folders,
            )
        }
    }

    private fun applyChipAndFilters(songs: List<LibrarySong>): List<LibrarySong> {
        val state = _uiState.value
        val chip = state.selectedChip
        var result = songs

        // 칩 (단일 선택): "모두" / "음악" / "녹음" / "연습 중" / "★" / 폴더명
        result = when (chip) {
            "모두" -> result
            "음악" -> result.filter { !it.isRecording }
            "녹음" -> result.filter { it.isRecording }
            "연습 중" -> result.filter { it.isActive }
            "★" -> result.filter { it.favorite }
            else -> result.filter { it.folder == chip } // 폴더명 매칭
        }

        // 빠른 필터 (다중 선택)
        state.filters.forEach { f ->
            result = when (f) {
                LibraryQuickFilter.FavoritesOnly -> result.filter { it.favorite }
                LibraryQuickFilter.PracticingOnly -> result.filter { it.isActive }
                LibraryQuickFilter.NotPracticed -> result.filter { it.lastPracticed == null }
                LibraryQuickFilter.ManySections -> result.filter { it.sections > 5 }
            }
        }

        // 검색 query (제목/아티스트/폴더 부분 일치, 대소문자 무시)
        val q = state.query.trim()
        if (q.isNotEmpty()) {
            result = result.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.artist.contains(q, ignoreCase = true) ||
                    (it.folder?.contains(q, ignoreCase = true) == true)
            }
        }
        return result
    }

    private fun applySort(songs: List<LibrarySong>): List<LibrarySong> {
        val state = _uiState.value
        val ordered = when (state.sort) {
            LibrarySort.RecentPractice -> songs.sortedByDescending { it.lastPracticed ?: 0L }
            LibrarySort.RecentlyAdded  -> songs // raw tracks 가 이미 date_added DESC 정렬
            LibrarySort.Title          -> songs.sortedBy { it.title }
            LibrarySort.Artist         -> songs.sortedBy { it.artist }
            LibrarySort.MostSections   -> songs.sortedByDescending { it.sections }
            LibrarySort.MostLoops      -> songs.sortedByDescending { it.loops }
            LibrarySort.ShortestFirst  -> songs.sortedBy { it.durationMs }
        }
        // pinActiveOnTop 옵션
        return if (state.viewOptions.pinActiveOnTop) {
            ordered.sortedByDescending { it.isActive }
        } else ordered
    }

    private fun buildChips(songs: List<LibrarySong>): List<LibraryChip> {
        val musicCount = songs.count { !it.isRecording }
        val recCount   = songs.count { it.isRecording }
        val fixed = buildList {
            add(LibraryChip("모두", songs.size))
            if (musicCount > 0) add(LibraryChip("음악", musicCount))
            if (recCount > 0)   add(LibraryChip("녹음", recCount))
            add(LibraryChip("연습 중", songs.count { it.isActive }, accent = true))
            add(LibraryChip("★", songs.count { it.favorite }))
        }
        // 상위 5개 폴더를 칩으로 노출 (음악·녹음 카운트로 이미 분류됐으니 폴더는 보조)
        val folders = songs.mapNotNull { it.folder }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { LibraryChip(it.key, it.value) }
        return fixed + folders
    }

    private fun buildFolders(songs: List<LibrarySong>): List<LibraryFolder> =
        songs.mapNotNull { it.folder }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { LibraryFolder(it.key, it.value) }

    // ─────────────── MusicTrack → LibrarySong ───────────────
    private fun MusicTrack.toLibrarySong(sectionCount: Int, meta: TrackMetadata?): LibrarySong {
        // category 가 있으면 그걸, 없으면 folder 경로의 첫 세그먼트를 폴더명으로.
        val folderName = category ?: folder.split('/').firstOrNull()?.takeIf { it.isNotBlank() }
        return LibrarySong(
            id = uri,
            title = title,
            artist = artist,
            durationMs = durationMs,
            sections = sectionCount,
            lastPracticed = meta?.lastPracticedMs,
            loops = meta?.loops ?: 0,
            bpm = null,                      // 메타 없음
            musicKey = null,                 // 메타 없음
            folder = folderName,
            hueDeg = stableHue(artist),
            favorite = meta?.favorite ?: false,
            // "연습 중" = 저장된 구간이 있거나 최근(예: 7일 내) 연주된 트랙
            isActive = sectionCount > 0 ||
                (meta?.lastPracticedMs?.let {
                    System.currentTimeMillis() - it < 7L * 24 * 3600 * 1000
                } ?: false),
            isRecording = isCallRecording || isVoiceRecording,
        )
    }

    /** 아티스트 문자열에서 안정적인 hue 산출 (0..359). */
    private fun stableHue(seed: String): Int {
        if (seed.isBlank()) return 200
        var h = 0
        seed.forEach { h = (h * 31 + it.code) and 0x7FFFFFFF }
        return h % 360
    }
}
