package com.coworkapp.loopplayer.ui.library

/**
 * Library song model.
 *
 * @param durationMs    총 길이 (재생 길이 표시는 mm:ss 포맷)
 * @param sections      저장된 구간 개수 (1..N)
 * @param lastPracticed 마지막 연습 시점 (밀리초 epoch). null 이면 미연습.
 * @param loops         누적 반복 횟수
 * @param bpm           템포 (null 가능)
 * @param musicKey      키 (예: "Eb", "Dm")
 * @param folder        폴더/카테고리 라벨
 * @param hueDeg        썸네일 그라디언트의 베이스 hue. 보통 곡 메타에서 산출 (artist hash mod 360 등).
 */
data class LibrarySong(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val sections: Int,
    val lastPracticed: Long?,
    val loops: Int,
    val bpm: Int?,
    val musicKey: String?,
    val folder: String?,
    val hueDeg: Int,
    val favorite: Boolean = false,
    val isActive: Boolean = false,   // 현재 연습 중 (active loop saved or recently practiced)
    val isRecording: Boolean = false, // 음성/통화 녹음 파일 여부
)

/** 정렬 기준 — sheet 라디오 리스트와 1:1. */
enum class LibrarySort(val label: String) {
    RecentPractice("최근 연습"),
    RecentlyAdded("최근 추가"),
    Title("제목"),
    Artist("아티스트"),
    MostSections("구간 많은 순"),
    MostLoops("반복 많은 순"),
    ShortestFirst("길이 짧은 순"),
}

enum class LibraryGroup(val label: String) {
    None("없음"),
    Artist("아티스트"),
    Folder("폴더"),
    Key("키"),
}

/** 빠른 필터 — 다중 선택. */
enum class LibraryQuickFilter(val label: String) {
    FavoritesOnly("즐겨찾기만"),
    PracticingOnly("연습 중만"),
    NotPracticed("미연습"),
    ManySections("> 5구간"),
}

/** 보기 토글 — 시트 VIEW 섹션. */
data class LibraryViewOptions(
    val showWaveform: Boolean      = true,
    val showBpmKey:   Boolean      = true,
    val showThumbnail: Boolean     = true,
    val pinActiveOnTop: Boolean    = false,
)

/** 칩 rail에 표시할 칩 1개. */
data class LibraryChip(
    val label: String,
    val count: Int? = null,
    val accent: Boolean = false,
)

/** 드로어 폴더 항목 1개. */
data class LibraryFolder(
    val name: String,
    val count: Int,
)

/** UI state — ViewModel 이 이 한 덩어리를 expose 하면 됨. */
data class LibraryUiState(
    val songs:          List<LibrarySong> = emptyList(),
    val totalCount:     Int               = 0,
    val totalLoops:     Int               = 0,    // 누적 반복 (현재 미추적 → 0)
    val totalMinutes:   Int               = 0,
    val activeCount:    Int               = 0,
    val recordingCount: Int               = 0,
    val favoriteCount:  Int               = 0,
    val sort:           LibrarySort       = LibrarySort.RecentPractice,
    val sortDescending: Boolean           = true,
    val group:          LibraryGroup      = LibraryGroup.None,
    val filters:        Set<LibraryQuickFilter> = emptySet(),
    val viewOptions:    LibraryViewOptions = LibraryViewOptions(),
    val selectedChip:   String            = "모두",
    val chips:          List<LibraryChip> = emptyList(),
    val folders:        List<LibraryFolder> = emptyList(),
    val permissionGranted: Boolean = false,
    val loading:        Boolean = false,
)
