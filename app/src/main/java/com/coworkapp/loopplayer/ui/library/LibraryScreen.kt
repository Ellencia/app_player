package com.coworkapp.loopplayer.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Variant E — 라이브러리(노래 리스트) 화면 진입점.
 *
 * 화면 구성:
 *  ┌ Header (드로어 트리거 + 검색 + 보기/정렬 버튼)
 *  ├ 칩 rail (모두 / 연습 중 / ★ / 폴더…)
 *  ├ Sort 라인
 *  └ LazyColumn(SongRow)
 *
 *  상호작용:
 *  - 헤더 좌측 앱 마크 탭 → 좌측 드로어
 *  - 헤더 우측 햄버거 탭 → 하단 시트 (정렬/그룹/필터/보기/액션)
 *  - 헤더 우측 검색 탭 → 검색 모드 (별도 처리; 본 컴포저블은 trigger 만)
 *  - 행 롱프레스 → 다중 선택 (콜백만 노출)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onSearchClick: () -> Unit,
    onSearchExit: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onGroupChange: (LibraryGroup) -> Unit,
    onFilterToggle: (LibraryQuickFilter) -> Unit,
    onViewOptionChange: (LibraryViewOptions) -> Unit,
    onChipSelect: (String) -> Unit,
    onResetSheet: () -> Unit,
    onApplySheet: () -> Unit,
    onSongClick: (LibrarySong) -> Unit,
    onSongLongPress: (LibrarySong) -> Unit,
    onToggleFavorite: (LibrarySong) -> Unit,
    onNavigate: (LibraryDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sheetOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Wrap in a width-bounded ModalDrawerSheet so the Material 3 drawer
            // shows our dark Surface instead of the default theme color.
            ModalDrawerSheet(
                drawerContainerColor = LibraryColors.Surface,
                drawerContentColor   = LibraryColors.OnSurface,
                drawerShape          = RoundedCornerShape(0.dp),
                modifier             = Modifier.width(332.dp),
            ) {
                NavigationDrawerContent(
                    todayMinutes = 0,
                    streakDays   = 0,
                    todaySongs   = 0,
                    todayLoops   = 0,
                    counts       = state.toDrawerCounts(),
                    activeDestination = when (state.selectedChip) {
                        "녹음" -> LibraryDestination.Recordings
                        "★"    -> LibraryDestination.Favorites
                        else   -> LibraryDestination.Library
                    },
                    onNavigate   = { dest -> scope.launch { drawerState.close() }; onNavigate(dest) },
                    onClose      = { scope.launch { drawerState.close() } },
                )
            }
        },
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color    = LibraryColors.Background,
            contentColor = LibraryColors.OnSurface,
        ) {
            Column(Modifier.fillMaxSize()) {
                LibraryHeader(
                    streakDays    = 0,
                    streakOutOf   = 7,
                    totalCount    = state.totalCount,
                    totalLoops    = state.totalLoops,
                    totalHours    = state.totalMinutes / 60,
                    searchMode    = state.searchMode,
                    query         = state.query,
                    onQueryChange = onQueryChange,
                    onSearchExit  = onSearchExit,
                    onDrawerOpen  = { scope.launch { drawerState.open() } },
                    onSearchClick = onSearchClick,
                    onMenuClick   = { sheetOpen = true },
                )
                ChipRail(
                    selected = state.selectedChip,
                    chips = state.chips,
                    onSelect = onChipSelect,
                )
                SortLine()
                LazyColumn(Modifier.fillMaxSize()) {
                    if (state.group == LibraryGroup.None) {
                        items(state.songs, key = { it.id }) { song ->
                            SongRow(
                                song    = song,
                                options = state.viewOptions,
                                onClick = { onSongClick(song) },
                                onLongPress = { onSongLongPress(song) },
                                onToggleFavorite = { onToggleFavorite(song) },
                            )
                        }
                    } else {
                        // 그룹화: group 키별로 정렬, 각 그룹은 stickyHeader.
                        // 그룹 내 순서는 ViewModel의 sort 결과 유지.
                        val grouped = state.songs.groupBy { song ->
                            when (state.group) {
                                LibraryGroup.Artist -> song.artist.ifBlank { "—" }
                                LibraryGroup.Folder -> song.folder ?: "—"
                                LibraryGroup.Key    -> song.musicKey ?: "—"
                                LibraryGroup.None   -> ""
                            }
                        }.toSortedMap()
                        grouped.forEach { (groupName, songs) ->
                            stickyHeader(key = "header::$groupName") {
                                GroupHeader(label = groupName, count = songs.size)
                            }
                            items(songs, key = { it.id }) { song ->
                                SongRow(
                                    song    = song,
                                    options = state.viewOptions,
                                    onClick = { onSongClick(song) },
                                    onLongPress = { onSongLongPress(song) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState       = sheetState,
            containerColor   = LibraryColors.SurfaceElevated,
            scrimColor       = LibraryColors.Scrim,
            shape            = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            dragHandle       = { BottomSheetDefaults.DragHandle(color = Color(0x38FFFFFF)) },
        ) {
            SortMenuSheetContent(
                state         = state,
                onSort        = onSortChange,
                onGroup       = onGroupChange,
                onFilter      = onFilterToggle,
                onView        = onViewOptionChange,
                onReset       = onResetSheet,
                onApply       = { onApplySheet(); sheetOpen = false },
                onDismiss     = { sheetOpen = false },
            )
        }
    }
}

@Composable
private fun GroupHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LibraryColors.Background)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = LibraryColors.OnSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        )
        Text(
            "$count",
            color = LibraryColors.OnSurfaceMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
    }
}

/** 드로어 네비게이션 목적지. ViewModel/Activity 가 라우팅 책임. */
enum class LibraryDestination {
    Library, Recordings, Playlists, Favorites, RecentPractice,
    Stats, Folder, ImportFiles, Settings, Help, AppInfo,
}

/** Drawer 의 카운트 배지에 쓰는 값 묶음. */
data class DrawerCounts(
    val libraryCount: Int = 0,
    val recordings:   Int = 0,
    val playlists:    Int = 0,
    val favorites:    Int = 0,
    val folders:      List<Pair<String, Int>> = emptyList(),
)

private fun LibraryUiState.toDrawerCounts(): DrawerCounts = DrawerCounts(
    libraryCount = totalCount,
    recordings   = recordingCount,
    playlists    = 0,
    favorites    = favoriteCount,
    folders      = folders.map { it.name to it.count },
)
