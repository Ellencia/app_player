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
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

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
    actions: PlayerActions,
) {
    var sectionBeingEdited by remember { mutableStateOf<LoopSection?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var newSectionLabel by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    IconButton(onClick = actions.onOpenFile) {
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
                EmptyState(onOpenFile = actions.onOpenFile)
                return@Column
            }

            // ── ① 파형 + 시간 + 칩 ──
            WaveformCard(state, onSeekTo = actions.onSeekTo)

            // ── ② Transport ──
            TransportRow(
                isPlaying = state.isPlaying,
                onSeekRelative = actions.onSeekRelative,
                onTogglePlay = actions.onTogglePlay,
            )

            // ── ③ A / B / R ──
            BigShortcutRow(
                tempStartMs = state.tempStartMs,
                tempEndMs   = state.tempEndMs,
                hasActiveLoop = state.activeSectionId != null,
                onMarkStart = actions.onMarkStart,
                onMarkEnd   = actions.onMarkEnd,
                onSaveTemp  = {
                    if (state.tempStartMs != null && state.tempEndMs != null) {
                        showSaveDialog = true
                        newSectionLabel = "구간 ${state.sections.size + 1}"
                    }
                },
                onRestartOrStop = {
                    if (state.activeSectionId != null) actions.onRestartActive()
                    else actions.onClearTemp()
                },
            )

            // ── ④ 속도 ──
            SpeedCard(speed = state.speed, onSetSpeed = actions.onSetSpeed)

            // ── ⑤ 저장된 구간 ──
            SectionListPanel(
                sections = state.sections,
                activeId = state.activeSectionId,
                currentLoopIndex = state.currentLoopIndex,
                onSelect = actions.onSelectSection,
                onStop = actions.onStopLoop,
                onEdit = { sectionBeingEdited = it },
                onDelete = { section ->
                    // 스와이프 즉시 삭제 → 스낵바로 짧게 실행취소 제공
                    val idx = state.sections.indexOfFirst { it.id == section.id }
                    actions.onDeleteSection(section.id)
                    snackbarScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "‘${section.label}’ 삭제됨",
                            actionLabel = "실행취소",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            actions.onRestoreSection(section, idx.coerceAtLeast(0))
                        }
                    }
                },
            )

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showSaveDialog) {
        // 다이얼로그 열릴 때 자동 포커스 + 기본 라벨 전체 선택 → 한 번에 덮어쓰기 가능
        val focusRequester = remember { FocusRequester() }
        var fieldValue by remember(showSaveDialog) {
            mutableStateOf(
                TextFieldValue(
                    text = newSectionLabel,
                    selection = TextRange(0, newSectionLabel.length),
                )
            )
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("구간 저장") },
            text = {
                Column {
                    Text("이름을 정해주세요. (비워두면 자동 번호)")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fieldValue,
                        onValueChange = {
                            fieldValue = it
                            newSectionLabel = it.text
                        },
                        singleLine = true,
                        label = { Text("구간 이름") },
                        modifier = Modifier.focusRequester(focusRequester),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    actions.onSaveTempSection(newSectionLabel.takeIf { it.isNotBlank() })
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
                actions.onUpdateSection(it)
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
            Icon(Icons.Default.FastRewind, null)
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
            Icon(Icons.Default.FastForward, null)
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
        Modifier.fillMaxWidth().height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BigButton(
            label = "A", sub = "시작",
            color = MaterialTheme.colorScheme.primary,
            active = tempStartMs != null,
            modifier = Modifier.weight(1f),
            onClick = onMarkStart,
        )
        BigButton(
            label = "B", sub = "끝",
            color = MaterialTheme.colorScheme.primary,
            active = tempEndMs != null,
            modifier = Modifier.weight(1f),
            onClick = onMarkEnd,
        )
        // canSave일 때 = "저장" (강조 색), 아니면 "R" (보조 색).
        // 같은 버튼이지만 색·라벨·아이콘 모두 바뀌어 모드 전환이 명확함.
        BigButton(
            label = if (canSave) "저장" else "R",
            sub = when {
                canSave -> "구간추가"
                hasActiveLoop -> "처음으로"
                else -> "리셋"
            },
            color = if (canSave) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.secondary,
            active = canSave,
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
    // active일 때 약간 더 진한 톤 + outline 으로 강조 (기존 dead-code 글로우 대체)
    val containerColor = if (active) color else color.copy(alpha = 0.85f)
    val onColor = MaterialTheme.colorScheme.onPrimary
    Surface(
        modifier = modifier.clip(RoundedCornerShape(18.dp)),
        color = containerColor,
        onClick = onClick,
        shadowElevation = if (active) 4.dp else 0.dp,
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = onColor,
            )
            Text(
                sub,
                fontSize = 11.sp,
                color = onColor.copy(alpha = 0.85f),
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
            // 굵은 dot 표시 안 나오게 steps 제거 → 단일 연속 트랙.
            // 0.05 단위로 round 해서 너무 잔파리 값으로 가지 않게.
            Slider(
                value = speed,
                valueRange = 0.5f..2.0f,
                onValueChange = { v -> onSetSpeed((kotlin.math.round(v * 20f) / 20f)) },
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
