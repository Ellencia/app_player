package com.coworkapp.loopplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coworkapp.loopplayer.LibraryTab
import com.coworkapp.loopplayer.LibraryUiState
import com.coworkapp.loopplayer.RecordingKind
import com.coworkapp.loopplayer.data.MusicTrack
import com.coworkapp.loopplayer.data.Playlist
import com.coworkapp.loopplayer.ui.theme.ListItemTitleStyle
import com.coworkapp.loopplayer.ui.theme.LoopDimens
import com.coworkapp.loopplayer.ui.theme.LoopShapes

/**
 * 자체 인덱싱한 음악 라이브러리 화면.
 *
 * 구조:
 *   ┌─ TopAppBar         : 닫기 + "라이브러리" + 새로고침
 *   ├─ ScrollableTabRow  : 음악 · 아티스트 · 플레이리스트 · 녹음
 *   ├─ 검색바            : 제목/아티스트/폴더/카테고리 부분 일치 (PLAYLIST 탭에선 숨김)
 *   └─ 본문              : 탭별 분기
 *
 * 탭:
 *   · MUSIC      → 트랙 리스트 (최근 추가순)
 *   · ARTIST     → 아티스트별 expandable 그룹
 *   · PLAYLIST   → 플레이리스트 목록 + 만들기 + 펼침
 *   · RECORDING  → 상단 FilterChip [음성][통화] + 트랙 리스트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    canClose: Boolean,
    onClose: () -> Unit,
    onRequestPermission: () -> Unit,
    onQueryChange: (String) -> Unit,
    onTabChange: (LibraryTab) -> Unit,
    onRefresh: () -> Unit,
    onPickTrack: (MusicTrack) -> Unit,
    onToggleRecordingFilter: (RecordingKind) -> Unit,
    onToggleArtistExpanded: (String) -> Unit,
    onTogglePlaylistExpanded: (String) -> Unit,
    onOpenCreatePlaylistDialog: () -> Unit,
    onConfirmCreatePlaylist: (String) -> Unit,
    onDismissCreatePlaylistDialog: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onOpenAddToPlaylistDialog: (MusicTrack?) -> Unit,
    onAddTrackToPlaylist: (trackUri: String, playlistId: String) -> Unit,
    onRemoveTrackFromPlaylist: (trackUri: String, playlistId: String) -> Unit,
    resolvePlaylistTracks: (Playlist) -> List<MusicTrack>,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("라이브러리") },
                navigationIcon = {
                    // 트랙이 로드돼 있을 때만 플레이어로 돌아갈 수 있음.
                    // 첫 진입(트랙 없음)에서는 닫을 곳이 없어 아이콘 숨김.
                    if (canClose) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "플레이어로")
                        }
                    }
                },
                actions = {
                    if (state.tab == LibraryTab.PLAYLIST) {
                        IconButton(onClick = onOpenCreatePlaylistDialog) {
                            Icon(Icons.Default.Add, contentDescription = "새 플레이리스트")
                        }
                    }
                    IconButton(onClick = onRefresh, enabled = state.permissionGranted) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (!state.permissionGranted) {
                PermissionNeeded(onRequest = onRequestPermission)
                return@Column
            }

            // ── 탭 (4개) ──
            val tabIndex = when (state.tab) {
                LibraryTab.MUSIC -> 0
                LibraryTab.ARTIST -> 1
                LibraryTab.PLAYLIST -> 2
                LibraryTab.RECORDING -> 3
            }
            ScrollableTabRow(
                selectedTabIndex = tabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                edgePadding = 12.dp,
            ) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { onTabChange(LibraryTab.MUSIC) },
                    text = { Text("음악 · ${state.musicTracks.size}") },
                    icon = { Icon(Icons.Default.LibraryMusic, null) },
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { onTabChange(LibraryTab.ARTIST) },
                    text = { Text("아티스트") },
                    icon = { Icon(Icons.Default.Person, null) },
                )
                Tab(
                    selected = tabIndex == 2,
                    onClick = { onTabChange(LibraryTab.PLAYLIST) },
                    text = { Text("플레이리스트 · ${state.playlists.size}") },
                    icon = { Icon(Icons.Default.QueueMusic, null) },
                )
                Tab(
                    selected = tabIndex == 3,
                    onClick = { onTabChange(LibraryTab.RECORDING) },
                    text = { Text("녹음 · ${state.voiceTracks.size + state.callTracks.size}") },
                    icon = { Icon(Icons.Default.Mic, null) },
                )
            }

            // ── 검색바 (PLAYLIST 탭에선 숨김) ──
            if (state.tab != LibraryTab.PLAYLIST) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    placeholder = { Text("제목, 아티스트, 폴더, 카테고리 검색") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "지우기")
                            }
                        }
                    },
                    singleLine = true,
                    shape = LoopShapes.Pill,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
            }

            // ── 본문 ──
            when {
                state.loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> when (state.tab) {
                    LibraryTab.MUSIC -> MusicTabBody(
                        state = state,
                        onPickTrack = onPickTrack,
                        onAddToPlaylist = onOpenAddToPlaylistDialog,
                    )
                    LibraryTab.ARTIST -> ArtistTabBody(
                        state = state,
                        onToggleExpanded = onToggleArtistExpanded,
                        onPickTrack = onPickTrack,
                        onAddToPlaylist = onOpenAddToPlaylistDialog,
                    )
                    LibraryTab.PLAYLIST -> PlaylistTabBody(
                        state = state,
                        resolvePlaylistTracks = resolvePlaylistTracks,
                        onToggleExpanded = onTogglePlaylistExpanded,
                        onPickTrack = onPickTrack,
                        onDeletePlaylist = onDeletePlaylist,
                        onRenamePlaylist = onRenamePlaylist,
                        onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist,
                        onCreatePlaylist = onOpenCreatePlaylistDialog,
                    )
                    LibraryTab.RECORDING -> RecordingTabBody(
                        state = state,
                        onToggleFilter = onToggleRecordingFilter,
                        onPickTrack = onPickTrack,
                        onAddToPlaylist = onOpenAddToPlaylistDialog,
                    )
                }
            }
        }
    }

    // 다이얼로그들
    if (state.showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onConfirm = onConfirmCreatePlaylist,
            onDismiss = onDismissCreatePlaylistDialog,
        )
    }
    state.addToPlaylistTrack?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = state.playlists,
            onPick = { pid -> onAddTrackToPlaylist(track.uri, pid) },
            onDismiss = { onOpenAddToPlaylistDialog(null) },
            onCreateNew = {
                onOpenAddToPlaylistDialog(null)
                onOpenCreatePlaylistDialog()
            },
        )
    }
}

/* ─────────────────────  MUSIC 탭  ───────────────────── */

@Composable
private fun MusicTabBody(
    state: LibraryUiState,
    onPickTrack: (MusicTrack) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
) {
    when {
        state.currentTabTracks.isEmpty() -> EmptyMessage("기기에서 재생 가능한 음악을 찾지 못했습니다.")
        state.filtered.isEmpty() -> EmptyMessage("\"${state.query}\" 검색 결과가 없습니다.")
        else -> {
            Text(
                "총 ${state.currentTabTracks.size}개 · 최근 추가순",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            ) {
                items(state.filtered, key = { it.id }) { tr ->
                    TrackRow(
                        track = tr,
                        onClick = { onPickTrack(tr) },
                        onAddToPlaylist = { onAddToPlaylist(tr) },
                    )
                }
            }
        }
    }
}

/* ─────────────────────  ARTIST 탭  ───────────────────── */

@Composable
private fun ArtistTabBody(
    state: LibraryUiState,
    onToggleExpanded: (String) -> Unit,
    onPickTrack: (MusicTrack) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
) {
    val groups = state.artistGroups
    when {
        state.musicTracks.isEmpty() -> EmptyMessage("음악이 없어 아티스트가 표시되지 않습니다.")
        groups.isEmpty() -> EmptyMessage("\"${state.query}\" 검색 결과가 없습니다.")
        else -> {
            Text(
                "총 ${groups.size}명 · 가나다순",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            ) {
                items(groups, key = { it.artist }) { group ->
                    val expanded = group.artist in state.expandedArtists
                    ArtistGroupCard(
                        artistName = group.artist,
                        count = group.tracks.size,
                        expanded = expanded,
                        onToggle = { onToggleExpanded(group.artist) },
                        tracks = group.tracks,
                        onPickTrack = onPickTrack,
                        onAddToPlaylist = onAddToPlaylist,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistGroupCard(
    artistName: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    tracks: List<MusicTrack>,
    onPickTrack: (MusicTrack) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LoopShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onToggle,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(LoopDimens.AvatarSize.Default),
                    shape = LoopShapes.Pill,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person, null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(LoopDimens.IconSize.Badge),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        artistName,
                        style = ListItemTitleStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "$count 곡",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "접기" else "펼치기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    tracks.forEach { tr ->
                        SubTrackRow(
                            track = tr,
                            onClick = { onPickTrack(tr) },
                            onAddToPlaylist = { onAddToPlaylist(tr) },
                        )
                    }
                }
            }
        }
    }
}

/* ─────────────────────  PLAYLIST 탭  ───────────────────── */

@Composable
private fun PlaylistTabBody(
    state: LibraryUiState,
    resolvePlaylistTracks: (Playlist) -> List<MusicTrack>,
    onToggleExpanded: (String) -> Unit,
    onPickTrack: (MusicTrack) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onRemoveTrackFromPlaylist: (String, String) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    if (state.playlists.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.QueueMusic, null,
                modifier = Modifier.size(LoopDimens.Surface.EmptySmall),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text("플레이리스트가 없습니다", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "원하는 곡들을 모아 플레이리스트로 관리할 수 있습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCreatePlaylist) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("플레이리스트 만들기")
            }
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
    ) {
        items(state.playlists, key = { it.id }) { pl ->
            val expanded = state.expandedPlaylistId == pl.id
            PlaylistCard(
                playlist = pl,
                expanded = expanded,
                tracks = if (expanded) resolvePlaylistTracks(pl) else emptyList(),
                onToggle = { onToggleExpanded(pl.id) },
                onDelete = { onDeletePlaylist(pl.id) },
                onRename = { newName -> onRenamePlaylist(pl.id, newName) },
                onPickTrack = onPickTrack,
                onRemoveTrack = { trackUri -> onRemoveTrackFromPlaylist(trackUri, pl.id) },
            )
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    expanded: Boolean,
    tracks: List<MusicTrack>,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onPickTrack: (MusicTrack) -> Unit,
    onRemoveTrack: (String) -> Unit,
) {
    var showRename by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LoopShapes.MediumCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onToggle,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(LoopDimens.AvatarSize.Default),
                    shape = LoopShapes.PlaylistBadge,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.QueueMusic, null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(LoopDimens.IconSize.Badge),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        playlist.name,
                        style = ListItemTitleStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${playlist.trackUris.size} 곡",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, "메뉴",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("이름 변경") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuOpen = false; showRename = true },
                        )
                        DropdownMenuItem(
                            text = { Text("삭제", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteOutline, null,
                                    tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                    if (tracks.isEmpty() && playlist.trackUris.isEmpty()) {
                        Text(
                            "곡을 추가하려면 음악·녹음 탭에서 트랙 우측 ➕ 버튼을 누르세요.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else if (tracks.isEmpty()) {
                        Text(
                            "(${playlist.trackUris.size}곡이 등록돼 있지만 라이브러리에서 찾지 못했습니다.)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            tracks.forEach { tr ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    SubTrackRow(
                                        track = tr,
                                        onClick = { onPickTrack(tr) },
                                        onAddToPlaylist = null,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { onRemoveTrack(tr.uri) }) {
                                        Icon(
                                            Icons.Default.RemoveCircleOutline,
                                            contentDescription = "플레이리스트에서 제거",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRename) {
        RenamePlaylistDialog(
            initial = playlist.name,
            onConfirm = { newName -> showRename = false; onRename(newName) },
            onDismiss = { showRename = false },
        )
    }
}

/* ─────────────────────  RECORDING 탭  ───────────────────── */

@Composable
private fun RecordingTabBody(
    state: LibraryUiState,
    onToggleFilter: (RecordingKind) -> Unit,
    onPickTrack: (MusicTrack) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
) {
    // 필터 chip Row
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = RecordingKind.VOICE in state.recordingFilter,
            onClick = { onToggleFilter(RecordingKind.VOICE) },
            label = { Text("음성 녹음 · ${state.voiceTracks.size}") },
            leadingIcon = { Icon(Icons.Default.Mic, null, modifier = Modifier.size(LoopDimens.IconSize.Small)) },
        )
        FilterChip(
            selected = RecordingKind.CALL in state.recordingFilter,
            onClick = { onToggleFilter(RecordingKind.CALL) },
            label = { Text("통화 녹음 · ${state.callTracks.size}") },
            leadingIcon = { Icon(Icons.Default.Call, null, modifier = Modifier.size(LoopDimens.IconSize.Small)) },
        )
    }

    when {
        state.currentTabTracks.isEmpty() -> EmptyMessage("선택한 종류의 녹음 파일이 없습니다.")
        state.filtered.isEmpty() -> EmptyMessage("\"${state.query}\" 검색 결과가 없습니다.")
        else -> {
            Text(
                "총 ${state.currentTabTracks.size}개 · 최근 추가순",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            ) {
                items(state.filtered, key = { it.id }) { tr ->
                    TrackRow(
                        track = tr,
                        onClick = { onPickTrack(tr) },
                        onAddToPlaylist = { onAddToPlaylist(tr) },
                    )
                }
            }
        }
    }
}

/* ─────────────────────  공통 Row 컴포넌트  ───────────────────── */

@Composable
private fun TrackRow(
    track: MusicTrack,
    onClick: () -> Unit,
    onAddToPlaylist: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LoopShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 카테고리별 아이콘
            val icon = when {
                track.isCallRecording -> Icons.Default.Call
                track.isVoiceRecording -> Icons.Default.Mic
                else -> Icons.Default.MusicNote
            }
            Surface(
                modifier = Modifier.size(LoopDimens.AvatarSize.Default),
                shape = LoopShapes.Pill,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(LoopDimens.IconSize.Badge),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = ListItemTitleStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // 음악: 아티스트, 녹음: 카테고리(있으면) > 폴더
                val sub = when {
                    track.isCallRecording || track.isVoiceRecording ->
                        track.category ?: track.folder
                    else -> track.artist
                }
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                formatTimeShort(track.durationMs),
                style = com.coworkapp.loopplayer.ui.theme.TimeListStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onAddToPlaylist != null) {
                IconButton(onClick = onAddToPlaylist) {
                    Icon(
                        Icons.Default.PlaylistAdd,
                        contentDescription = "플레이리스트에 추가",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** ArtistGroup 안 들여쓰기용 / PlaylistCard expand 안용 — 카드 없이 단순 row */
@Composable
private fun SubTrackRow(
    track: MusicTrack,
    onClick: () -> Unit,
    onAddToPlaylist: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = LoopShapes.SubRow,
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.PlayArrow,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(LoopDimens.IconSize.Medium),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                track.title,
                style = com.coworkapp.loopplayer.ui.theme.ActionLabelStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                formatTimeShort(track.durationMs),
                style = com.coworkapp.loopplayer.ui.theme.TimeListStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onAddToPlaylist != null) {
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.PlaylistAdd,
                        contentDescription = "플레이리스트에 추가",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(LoopDimens.IconSize.Medium),
                    )
                }
            }
        }
    }
}

/* ─────────────────────  안내 / 다이얼로그  ───────────────────── */

@Composable
private fun PermissionNeeded(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 80.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.LibraryMusic, null,
            modifier = Modifier.size(LoopDimens.Surface.EmptyMedium),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text("음악 파일 접근 권한이 필요합니다", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "기기의 음악·녹음을 인덱싱하려면 권한을 허용해 주세요.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequest) { Text("권한 허용") }
    }
}

@Composable
private fun EmptyMessage(msg: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            msg,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 플레이리스트") },
        text = {
            Column {
                Text("이름을 입력해 주세요. (비워두면 자동 번호)")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("플레이리스트 이름") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("만들기") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@Composable
private fun RenamePlaylistDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이름 변경") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("플레이리스트 이름") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("저장") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@Composable
private fun AddToPlaylistDialog(
    track: MusicTrack,
    playlists: List<Playlist>,
    onPick: (playlistId: String) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("플레이리스트에 추가") },
        text = {
            Column {
                Text(
                    "\"${track.title}\"",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                if (playlists.isEmpty()) {
                    Text(
                        "플레이리스트가 없습니다. 먼저 새로 만들어 주세요.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        playlists.forEach { pl ->
                            val already = track.uri in pl.trackUris
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = LoopShapes.SubRow,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { if (!already) onPick(pl.id) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.QueueMusic, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(LoopDimens.IconSize.Default),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        pl.name,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (already) {
                                        Text(
                                            "추가됨",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    } else {
                                        Text(
                                            "${pl.trackUris.size}곡",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreateNew) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(LoopDimens.IconSize.Small))
                Spacer(Modifier.width(4.dp))
                Text("새 플레이리스트")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
    )
}
