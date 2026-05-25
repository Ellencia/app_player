package com.coworkapp.loopplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coworkapp.loopplayer.PlayerUiState
import com.coworkapp.loopplayer.data.LoopSection
import com.coworkapp.loopplayer.ui.theme.SpeedDisplayStyle
import com.coworkapp.loopplayer.ui.theme.TimeMainStyle
import com.coworkapp.loopplayer.ui.theme.TimeSubStyle

/**
 * Variant A — Polished Material 3 Purple
 *
 * 구조 (위→아래):
 *   ┌─ TopAppBar : 트랙 제목 + 폴더 아이콘
 *   ├─ Card : 시간/파형(WaveformProgressBar) + A/B 칩 + 반복중 칩
 *   ├─ TransportRow : −5초 / Play-Pause / +5초
 *   ├─ Row : A / B / R(또는 저장) ─ 76dp, 라운드 18dp
 *   ├─ Card : 속도 슬라이더 + 0.75x / 1x / 1.25x / 1.5x 프리셋
 *   └─ SectionListPanel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onOpenFile: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onMarkStart: () -> Unit,
    onMarkEnd: () -> Unit,
    onSaveTempSection: (String?) -> Unit,
    onClearTemp: () -> Unit,
    onSelectSection: (LoopSection) -> Unit,
    onStopLoop: () -> Unit,
    onRestartActive: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onUpdateSection: (LoopSection) -> Unit,
    onDeleteSection: (String) -> Unit,
) {
    var sectionBeingEdited by remember { mutableStateOf<LoopSection?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var newSectionLabel by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.trackTitle.isNotBlank()) state.trackTitle
                        else "구간반복 플레이어",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenFile) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "파일 열기")
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.trackUri == null) {
                EmptyState(onOpenFile = onOpenFile)
                return@Column
            }

            // ── ① 파형 + 시간 + 칩 ──
            WaveformCard(state, onSeekTo = onSeekTo)

            // ── ② Transport ──
            TransportRow(
                isPlaying = state.isPlaying,
                onSeekRelative = onSeekRelative,
                onTogglePlay = onTogglePlay,
            )

            // ── ③ A / B / R ──
            BigShortcutRow(
                tempStartMs = state.tempStartMs,
                tempEndMs   = state.tempEndMs,
                hasActiveLoop = state.activeSectionId != null,
                onMarkStart = onMarkStart,
                onMarkEnd   = onMarkEnd,
                onSaveTemp  = {
                    if (state.tempStartMs != null && state.tempEndMs != null) {
                        showSaveDialog = true
                        newSectionLabel = "구간 ${state.sections.size + 1}"
                    }
                },
                onRestartOrStop = {
                    if (state.activeSectionId != null) onRestartActive() else onClearTemp()
                },
            )

            // ── ④ 속도 ──
            SpeedCard(speed = state.speed, onSetSpeed = onSetSpeed)

            // ── ⑤ 저장된 구간 ──
            SectionListPanel(
                sections = state.sections,
                activeId = state.activeSectionId,
                currentLoopIndex = state.currentLoopIndex,
                onSelect = onSelectSection,
                onStop = onStopLoop,
                onEdit = { sectionBeingEdited = it },
                onDelete = onDeleteSection,
            )

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("구간 저장") },
            text = {
                Column {
                    Text("이름을 정해주세요. (비워두면 자동 번호)")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newSectionLabel,
                        onValueChange = { newSectionLabel = it },
                        singleLine = true,
                        label = { Text("구간 이름") },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveTempSection(newSectionLabel.takeIf { it.isNotBlank() })
                    showSaveDialog = false
                }) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("취소") }
            },
        )
    }

    sectionBeingEdited?.let { sec ->
        AdvancedSettingsDialog(
            section = sec,
            duration = state.durationMs,
            onDismiss = { sectionBeingEdited = null },
            onSave = {
                onUpdateSection(it)
                sectionBeingEdited = null
            },
        )
    }
}

/* ─────────────────────────  ①  ───────────────────────── */

@Composable
private fun WaveformCard(
    state: PlayerUiState,
    onSeekTo: (Long) -> Unit,
) {
    val active: LoopSection? = state.activeSectionId?.let { id ->
        state.sections.firstOrNull { it.id == id }
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(formatTime(state.positionMs), style = TimeMainStyle,
                     color = MaterialTheme.colorScheme.onSurface)
                Text(formatTime(state.durationMs), style = TimeSubStyle,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(6.dp))

            WaveformProgressBar(
                positionMs    = state.positionMs,
                durationMs    = state.durationMs,
                tempStartMs   = state.tempStartMs,
                tempEndMs     = state.tempEndMs,
                activeStartMs = active?.startMs,
                activeEndMs   = active?.endMs,
                waveform      = state.waveform,
                waveformLoading = state.waveformLoading,
                onSeekTo      = onSeekTo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .padding(vertical = 4.dp),
            )

            Spacer(Modifier.height(10.dp))

            // A/B 칩 + 반복중 칩
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MarkerChip("A", state.tempStartMs?.let { formatTimeShort(it) } ?: "─")
                MarkerChip("B", state.tempEndMs?.let { formatTimeShort(it) } ?: "─")
                if (active != null) {
                    val rep =
                        if (active.loopCount > 0) "${state.currentLoopIndex}/${active.loopCount}"
                        else "${state.currentLoopIndex}회"
                    AssistChip(
                        onClick = {},
                        label = { Text("반복중 · ${active.label} · $rep") },
                        leadingIcon = { Icon(Icons.Default.Loop, null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkerChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontWeight = FontWeight.Bold,
                 color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(6.dp))
            Text(value, style = androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 12.sp,
            ), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/* ─────────────────────────  ②  ───────────────────────── */

@Composable
private fun TransportRow(
    isPlaying: Boolean,
    onSeekRelative: (Long) -> Unit,
    onTogglePlay: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = { onSeekRelative(-5000L) },
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(100.dp),
        ) {
            Icon(Icons.Default.Replay5, null)
            Spacer(Modifier.width(4.dp))
            Text("5초")
        }
        FilledIconButton(
            onClick = onTogglePlay,
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
        OutlinedButton(
            onClick = { onSeekRelative(5000L) },
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(100.dp),
        ) {
            Text("5초")
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Forward5, null)
        }
    }
}

/* ─────────────────────────  ③  ───────────────────────── */

@Composable
private fun BigShortcutRow(
    tempStartMs: Long?,
    tempEndMs: Long?,
    hasActiveLoop: Boolean,
    onMarkStart: () -> Unit,
    onMarkEnd: () -> Unit,
    onSaveTemp: () -> Unit,
    onRestartOrStop: () -> Unit,
) {
    val canSave = tempStartMs != null && tempEndMs != null
    Row(
        Modifier.fillMaxWidth().height(76.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BigButton(
            label = "A", sub = "시작점",
            color = MaterialTheme.colorScheme.primary,
            active = tempStartMs != null,
            modifier = Modifier.weight(1f),
            onClick = onMarkStart,
        )
        BigButton(
            label = "B", sub = "끝점",
            color = MaterialTheme.colorScheme.primary,
            active = tempEndMs != null,
            modifier = Modifier.weight(1f),
            onClick = onMarkEnd,
        )
        BigButton(
            label = if (canSave) "저장" else "R",
            sub = when {
                canSave -> "구간추가"
                hasActiveLoop -> "처음으로"
                else -> "─"
            },
            color = if (canSave) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.secondary,
            active = false,
            modifier = Modifier.weight(1f),
            onClick = if (canSave) onSaveTemp else onRestartOrStop,
        )
    }
}

@Composable
private fun BigButton(
    label: String,
    sub: String,
    color: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            // 활성(임시 마커가 잡힌) 상태일 때 배경색과 같은 톤의 더블링 글로우
            .then(
                if (active) Modifier.background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(18.dp),
                ) else Modifier
            ),
        color = color,
        onClick = onClick,
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                label, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                sub, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
            )
        }
    }
}

/* ─────────────────────────  ④  ───────────────────────── */

@Composable
private fun SpeedCard(speed: Float, onSetSpeed: (Float) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("속도", style = MaterialTheme.typography.titleSmall,
                     color = MaterialTheme.colorScheme.onSurface)
                Text("%.2fx".format(speed),
                     style = SpeedDisplayStyle,
                     color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = speed,
                valueRange = 0.5f..2.0f,
                steps = 14,
                onValueChange = onSetSpeed,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { v ->
                    val sel = kotlin.math.abs(speed - v) < 0.001f
                    if (sel) {
                        FilledTonalButton(
                            onClick = { onSetSpeed(v) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            contentPadding = PaddingValues(vertical = 6.dp),
                        ) { Text("%.2fx".format(v),
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 13.sp,
                                )) }
                    } else {
                        OutlinedButton(
                            onClick = { onSetSpeed(v) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(vertical = 6.dp),
                        ) { Text("%.2fx".format(v),
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 13.sp,
                                )) }
                    }
                }
            }
        }
    }
}

/* ─────────────────────────  Empty state  ───────────────────────── */

@Composable
private fun EmptyState(onOpenFile: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.LibraryMusic, contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text("mp3 파일을 열어주세요", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenFile) {
            Icon(Icons.Default.FolderOpen, null)
            Spacer(Modifier.width(8.dp))
            Text("파일 선택")
        }
    }
}

/* ─────────────────────────  Time utils  ───────────────────────── */

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    val cs = (ms % 1000) / 10
    return "%d:%02d.%02d".format(m, s, cs)
}

/** mm:ss (시간 칩, 구간 리스트 같은 좁은 슬롯용) */
fun formatTimeShort(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
