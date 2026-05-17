package com.coworkapp.loopplayer

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.coworkapp.loopplayer.ui.LibraryScreen
import com.coworkapp.loopplayer.ui.PlayerScreen
import com.coworkapp.loopplayer.ui.theme.LoopPlayerTheme

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    /** Android 13+ 는 READ_MEDIA_AUDIO, 그 이하는 READ_EXTERNAL_STORAGE */
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
        // 시작 시 권한 상태를 ViewModel 에 반영. 이미 있으면 자동 스캔.
        libraryViewModel.setPermissionGranted(hasMediaPermission())

        setContent {
            LoopPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showLibrary by remember { mutableStateOf(false) }

                    if (showLibrary) {
                        val libState by libraryViewModel.uiState.collectAsState()
                        LibraryScreen(
                            state = libState,
                            onClose = { showLibrary = false },
                            onRequestPermission = {
                                if (hasMediaPermission()) {
                                    libraryViewModel.setPermissionGranted(true)
                                } else {
                                    permissionLauncher.launch(mediaPermission)
                                }
                            },
                            onQueryChange = libraryViewModel::setQuery,
                            onTabChange = libraryViewModel::setTab,
                            onRefresh = libraryViewModel::refresh,
                            onPickTrack = { track ->
                                playerViewModel.openTrack(Uri.parse(track.uri), track.title)
                                showLibrary = false
                            },
                            onToggleRecordingFilter = libraryViewModel::toggleRecordingFilter,
                            onToggleArtistExpanded = libraryViewModel::toggleArtistExpanded,
                            onTogglePlaylistExpanded = libraryViewModel::togglePlaylistExpanded,
                            onOpenCreatePlaylistDialog = {
                                libraryViewModel.showCreatePlaylistDialog(true)
                            },
                            onConfirmCreatePlaylist = libraryViewModel::createPlaylist,
                            onDismissCreatePlaylistDialog = {
                                libraryViewModel.showCreatePlaylistDialog(false)
                            },
                            onDeletePlaylist = libraryViewModel::deletePlaylist,
                            onRenamePlaylist = libraryViewModel::renamePlaylist,
                            onOpenAddToPlaylistDialog = libraryViewModel::openAddToPlaylistDialog,
                            onAddTrackToPlaylist = libraryViewModel::addTrackToPlaylist,
                            onRemoveTrackFromPlaylist = libraryViewModel::removeTrackFromPlaylist,
                            resolvePlaylistTracks = libraryViewModel::resolvePlaylistTracks,
                        )
                    } else {
                        val state by playerViewModel.uiState.collectAsState()
                        PlayerScreen(
                            state = state,
                            onOpenFile = {
                                // 권한 없으면 미리 요청, 있으면 그냥 라이브러리 열기
                                if (!hasMediaPermission()) {
                                    permissionLauncher.launch(mediaPermission)
                                }
                                showLibrary = true
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
                        )
                    }
                }
            }
        }
    }
}
