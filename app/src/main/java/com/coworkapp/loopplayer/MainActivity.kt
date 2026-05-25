package com.coworkapp.loopplayer

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.coworkapp.loopplayer.ui.PlayerActions
import com.coworkapp.loopplayer.ui.PlayerScreen
import com.coworkapp.loopplayer.ui.library.LibraryColors
import com.coworkapp.loopplayer.ui.library.LibraryDestination
import com.coworkapp.loopplayer.ui.library.LibraryScreen
import com.coworkapp.loopplayer.ui.theme.LoopPlayerTheme

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    private val mediaPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasMediaPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, mediaPermission) ==
            PackageManager.PERMISSION_GRANTED

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        libraryViewModel.setPermissionGranted(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        libraryViewModel.setPermissionGranted(hasMediaPermission())

        setContent {
            LoopPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val playerState by playerViewModel.uiState.collectAsState()
                    val hasTrack = playerState.trackUri != null

                    var libraryRequested by remember { mutableStateOf(true) }
                    val showLibrary = libraryRequested || !hasTrack

                    if (showLibrary) {
                        val libState by libraryViewModel.uiState.collectAsState()
                        if (!libState.permissionGranted) {
                            PermissionGate(onRequest = {
                                if (hasMediaPermission()) {
                                    libraryViewModel.setPermissionGranted(true)
                                } else {
                                    permissionLauncher.launch(mediaPermission)
                                }
                            })
                        } else {
                            LibraryScreen(
                                state = libState,
                                onSearchClick = libraryViewModel::enterSearch,
                                onSearchExit = libraryViewModel::exitSearch,
                                onQueryChange = libraryViewModel::setQuery,
                                onSortChange = libraryViewModel::setSort,
                                onGroupChange = libraryViewModel::setGroup,
                                onFilterToggle = libraryViewModel::toggleFilter,
                                onViewOptionChange = libraryViewModel::setViewOptions,
                                onChipSelect = libraryViewModel::selectChip,
                                onResetSheet = libraryViewModel::resetSheet,
                                onApplySheet = { /* 시트 적용은 즉시 반영되므로 닫기만 */ },
                                onSongClick = { song ->
                                    playerViewModel.openTrack(Uri.parse(song.id), song.title)
                                    libraryRequested = false
                                },
                                onSongLongPress = { /* TODO: 다중 선택 */ },
                                onToggleFavorite = { song ->
                                    libraryViewModel.toggleFavorite(song.id)
                                },
                                onNavigate = { dest ->
                                    when (dest) {
                                        LibraryDestination.Library    -> libraryViewModel.selectChip("모두")
                                        LibraryDestination.Recordings -> libraryViewModel.selectChip("녹음")
                                        LibraryDestination.Favorites  -> libraryViewModel.selectChip("★")
                                        else -> { /* TODO */ }
                                    }
                                },
                            )
                        }
                    } else {
                        // 플레이어 화면에서 시스템 뒤로가기 → 라이브러리로 복귀
                        BackHandler { libraryRequested = true }
                        PlayerScreen(
                            state = playerState,
                            actions = PlayerActions(
                                onOpenFile = {
                                    if (!hasMediaPermission()) {
                                        permissionLauncher.launch(mediaPermission)
                                    }
                                    libraryRequested = true
                                },
                                onTogglePlay = playerViewModel::togglePlay,
                                onSeekTo = playerViewModel::seekTo,
                                onSeekRelative = playerViewModel::seekRelative,
                                onMarkStart = playerViewModel::markStartHere,
                                onMarkEnd = playerViewModel::markEndHere,
                                onSaveTempSection = playerViewModel::saveTempAsSection,
                                onClearTemp = playerViewModel::clearTempMarkers,
                                onSelectSection = playerViewModel::startLoop,
                                onStopLoop = playerViewModel::stopLoop,
                                onRestartActive = playerViewModel::restartActiveSection,
                                onSetSpeed = playerViewModel::setSpeed,
                                onUpdateSection = playerViewModel::updateSection,
                                onDeleteSection = playerViewModel::deleteSection,
                                onRestoreSection = playerViewModel::restoreSection,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LibraryColors.Background)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "음악 파일 접근 권한이 필요합니다",
            color = LibraryColors.OnSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "기기의 음악·녹음을 인덱싱하려면 권한을 허용해 주세요.",
            color = LibraryColors.OnSurfaceMuted,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequest) { Text("권한 허용") }
    }
}
