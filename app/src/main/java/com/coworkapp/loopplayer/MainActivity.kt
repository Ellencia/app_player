package com.coworkapp.loopplayer

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.coworkapp.loopplayer.ui.PlayerScreen
import com.coworkapp.loopplayer.ui.theme.LoopPlayerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    // 파일 선택기 (Storage Access Framework)
    // mp3, m4a, wav, ogg, flac 등 audio MIME만 보여줌
    private val openAudioLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // URI를 영구 권한으로 잡아놔야 다음 실행 때도 읽을 수 있음
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* 권한 없는 URI는 무시 */ }
            val name = queryDisplayName(uri) ?: "오디오 파일"
            viewModel.openTrack(uri, name)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        var name: String? = null
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && it.moveToFirst()) name = it.getString(idx)
        }
        return name
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoopPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.uiState.collectAsState()
                    PlayerScreen(
                        state = state,
                        onOpenFile = { openAudioLauncher.launch(arrayOf("audio/*")) },
                        onTogglePlay = viewModel::togglePlay,
                        onSeekTo = viewModel::seekTo,
                        onSeekRelative = viewModel::seekRelative,
                        onMarkStart = viewModel::markStartHere,
                        onMarkEnd = viewModel::markEndHere,
                        onSaveTempSection = viewModel::saveTempAsSection,
                        onClearTemp = viewModel::clearTempMarkers,
                        onSelectSection = viewModel::startLoop,
                        onStopLoop = viewModel::stopLoop,
                        onRestartActive = viewModel::restartActiveSection,
                        onSetSpeed = viewModel::setSpeed,
                        onUpdateSection = viewModel::updateSection,
                        onDeleteSection = viewModel::deleteSection,
                    )
                }
            }
        }
    }
}
