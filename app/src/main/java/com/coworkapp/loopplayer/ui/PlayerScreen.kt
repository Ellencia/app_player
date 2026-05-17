package com.coworkapp.loopplayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coworkapp.loopplayer.PlayerUiState
import com.coworkapp.loopplayer.data.LoopSection

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
        topBar = {
            TopAppBar(
                title = { Text(if (state.trackTitle.isNotBlank()) state.trackTitle else "구간반복 플레이어") },
                actions = {
                    IconButton(onClick = onOpenFile) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "파일 열기")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (state.trackUri == null) {
                EmptyState(onOpenFile = onOpenFile)
                return@Column
            }

            // ── 타임라인 (현재위치 / 전체길이 + Slider + A/B 마커) ──
            TimelineBar(
                state = state,
                onSeekTo = onSeekTo,
            )

            Spacer(Modifier.height(8.dp))

            // ── 재생 컨트롤: -5초 / 재생-일시정지 / +5초 ──
            TransportRow(
                isPlaying = state.isPlaying,
                onSeekRelative = onSeekRelative,
                onTogglePlay = onTogglePlay,
            )

            Spacer(Modifier.height(12.dp))

            // ── A / B / R 큰 버튼 ──
            BigShortcutButtons(
                tempStartMs = state.tempStartMs,
                tempEndMs = state.tempEndMs,
                hasActiveLoop = state.activeSectionId != null,
                onMarkStart = onMarkStart,
                onMarkEnd = onMarkEnd,
                onSaveTemp = {
                    if (state.tempStartMs != null && state.tempEndMs != null) {
                        showSaveDialog = true
                        newSectionLabel = "구간 ${state.sections.size + 1}"
                    }
                },
                onRestartOrStop = {
                    if (state.activeSectionId != null) onRestartActive() else onClearTemp()
                },
            )

            Spacer(Modifier.height(12.dp))

            // ── 속도 슬라이더 ──
            SpeedRow(speed = state.speed, onSetSpeed = onSetSpeed)

            Spacer(Modifier.height(16.dp))

            // ── 저장된 구간 리스트 ──
            SectionListPanel(
                sections = state.sections,
                activeId = state.activeSectionId,
                currentLoopIndex = state.currentLoopIndex,
                onSelect = onSelectSection,
                onStop = onStopLoop,
                onEdit = { sectionBeingEdited = it },
                onDelete = onDeleteSection,
            )

            Spacer(Modifier.height(80.dp))
        }
    }

    // ── A/B 임시 구간을 저장할 때 이름 묻는 다이얼로그 ──
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
                        label = { Text("구간 이름") }
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
            }
        )
    }

    // ── 구간 상세 설정 다이얼로그 ──
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

@Composable
private fun EmptyState(onOpenFile: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.LibraryMusic,
            contentDescription = null,
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

@Composable
private fun TimelineBar(
    state: PlayerUiState,
    onSeekTo: (Long) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatTime(state.positionMs), fontWeight = FontWeight.SemiBold)
                Text(formatTime(state.durationMs))
            }

            Spacer(Modifier.height(4.dp))

            // ── 커스텀 진행바: A/B 마커 + 활성 반복 구간을 색으로 표시 ──
            val active = state.activeSectionId?.let { id ->
                state.sections.firstOrNull { it.id == id }
            }
            LoopProgressBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                tempStartMs = state.tempStartMs,
                tempEndMs = state.tempEndMs,
                activeStartMs = active?.startMs,
                activeEndMs = active?.endMs,
                waveform = state.waveform,
                waveformLoading = state.waveformLoading,
                onSeekTo = onSeekTo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(vertical = 4.dp),
            )

            Spacer(Modifier.height(4.dp))

            // A / B 마커 + 반복중 칩
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MarkerChip(
                    label = "A",
                    value = state.tempStartMs?.let { formatTime(it) } ?: "─",
                )
                MarkerChip(
                    label = "B",
                    value = state.tempEndMs?.let { formatTime(it) } ?: "─",
                )
                state.activeSectionId?.let {
                    val s = state.sections.firstOrNull { it.id == state.activeSectionId }
                    if (s != null) {
                        AssistChip(
                            onClick = {},
                            label = {
                                val rep = if (s.loopCount > 0) "${state.currentLoopIndex}/${s.loopCount}"
                                else "${state.currentLoopIndex}회"
                                Text("반복중 · ${s.label} · $rep")
                            },
                            leadingIcon = { Icon(Icons.Default.Loop, null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkerChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(value)
        }
    }
}

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
            modifier = Modifier.weight(1f).height(56.dp),
        ) {
            Icon(Icons.Default.Replay5, null)
            Spacer(Modifier.width(4.dp))
            Text("-5s")
        }
        FilledIconButton(
            onClick = onTogglePlay,
            modifier = Modifier.size(64.dp),
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
        }
        OutlinedButton(
            onClick = { onSeekRelative(5000L) },
            modifier = Modifier.weight(1f).height(56.dp),
        ) {
            Text("+5s")
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Forward5, null)
        }
    }
}

@Composable
private fun BigShortcutButtons(
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
        Modifier.fillMaxWidth().height(72.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BigButton(
            label = "A",
            sub = "시작점",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary,
            onClick = onMarkStart,
        )
        BigButton(
            label = "B",
            sub = "끝점",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary,
            onClick = onMarkEnd,
        )
        BigButton(
            label = if (canSave) "저장" else "R",
            sub = if (canSave) "구간추가" else if (hasActiveLoop) "처음으로" else "─",
            modifier = Modifier.weight(1f),
            color = if (canSave) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.secondary,
            onClick = if (canSave) onSaveTemp else onRestartOrStop,
        )
    }
}

@Composable
private fun BigButton(
    label: String,
    sub: String,
    modifier: Modifier = Modifier,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        color = color,
        onClick = onClick,
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary)
            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun SpeedRow(speed: Float, onSetSpeed: (Float) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("속도", fontWeight = FontWeight.SemiBold)
                Text("%.2fx".format(speed))
            }
            Slider(
                value = speed,
                valueRange = 0.5f..2.0f,
                steps = 14, // 0.1 단위
                onValueChange = onSetSpeed,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { v ->
                    OutlinedButton(
                        onClick = { onSetSpeed(v) },
                        modifier = Modifier.weight(1f),
                    ) { Text("${v}x") }
                }
            }
        }
    }
}

// ────────────── 진행바 (파형 + 구간 시각화) ──────────────
//
// 막대 색상 의미:
//   ▮ 안 지나간 부분 = 연한 회색 (surfaceVariant)
//   ▮ 지나간 부분 = 진한 회색 (onSurfaceVariant)
//   ▮ 활성 반복 구간 = primary (보라) - 진행 여부와 무관하게 강조
//   ▮ A-B 임시 구간 = tertiary (주황) - 아직 저장 안 한 새 구간 후보
//   ▮ 현재 재생 위치 = primary 색 세로선
//
// 파형이 아직 분석 중일 때는 단순 라인 막대로 fallback.
//
@Composable
private fun LoopProgressBar(
    positionMs: Long,
    durationMs: Long,
    tempStartMs: Long?,
    tempEndMs: Long?,
    activeStartMs: Long?,
    activeEndMs: Long?,
    waveform: List<Float>,
    waveformLoading: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val unplayedColor = MaterialTheme.colorScheme.outlineVariant
    val playedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary
    val tempColor = MaterialTheme.colorScheme.tertiary
    val playheadColor = MaterialTheme.colorScheme.primary

    // 캔버스의 실제 가로 픽셀 크기 - 탭/드래그 좌표를 시간으로 환산할 때 사용
    var widthPx by remember { mutableStateOf(1f) }
    fun toMs(x: Float): Long {
        if (durationMs <= 0L) return 0L
        val ratio = (x / widthPx).coerceIn(0f, 1f)
        return (ratio * durationMs).toLong()
    }

    Box(
        modifier = modifier
            .pointerInput(durationMs) {
                detectTapGestures(onTap = { offset ->
                    onSeekTo(toMs(offset.x))
                })
            }
            .pointerInput(durationMs) {
                detectDragGestures(onDrag = { change, _ ->
                    onSeekTo(toMs(change.position.x))
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            widthPx = size.width
            val w = size.width
            val h = size.height
            val centerY = h / 2f
            val dur = durationMs.coerceAtLeast(1L).toFloat()

            fun xOf(ms: Long): Float = (ms.toFloat() / dur * w).coerceIn(0f, w)

            val tempLo = if (tempStartMs != null && tempEndMs != null)
                minOf(tempStartMs, tempEndMs) else null
            val tempHi = if (tempStartMs != null && tempEndMs != null)
                maxOf(tempStartMs, tempEndMs) else null

            if (waveform.isNotEmpty()) {
                // ── 파형 막대 그리기 ──
                val barCount = waveform.size
                val totalGap = 1.dp.toPx()
                val barTotal = w / barCount
                val barWidth = (barTotal - totalGap).coerceAtLeast(1f)
                val maxBarHeight = h * 0.85f
                val minBarHeight = 2.dp.toPx()

                for (i in 0 until barCount) {
                    val barX = i * barTotal
                    val barCenterTimeMs = ((i + 0.5f) / barCount * dur).toLong()
                    val amp = waveform[i].coerceIn(0f, 1f)
                    val barH = (amp * maxBarHeight).coerceAtLeast(minBarHeight)

                    // 우선순위: 활성 반복 > 임시 A-B > 진행도/안지남
                    val color = when {
                        activeStartMs != null && activeEndMs != null &&
                            barCenterTimeMs in activeStartMs..activeEndMs -> activeColor
                        tempLo != null && tempHi != null &&
                            barCenterTimeMs in tempLo..tempHi -> tempColor
                        barCenterTimeMs <= positionMs -> playedColor
                        else -> unplayedColor
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(barX, centerY - barH / 2),
                        size = Size(barWidth, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3),
                    )
                }
            } else {
                // ── Fallback: 파형 분석 전엔 단순 라인 ──
                val trackHeight = 8.dp.toPx()
                val activeTrackHeight = 14.dp.toPx()

                drawRoundRect(
                    color = unplayedColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(w, trackHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2),
                )
                val posX = xOf(positionMs)
                if (posX > 0f) {
                    drawRoundRect(
                        color = playedColor,
                        topLeft = Offset(0f, centerY - trackHeight / 2),
                        size = Size(posX, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2),
                    )
                }
                if (activeStartMs != null && activeEndMs != null && activeEndMs > activeStartMs) {
                    val sx = xOf(activeStartMs); val ex = xOf(activeEndMs)
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(sx, centerY - activeTrackHeight / 2),
                        size = Size((ex - sx).coerceAtLeast(2f), activeTrackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(activeTrackHeight / 2),
                    )
                }
                if (tempLo != null && tempHi != null) {
                    val sx = xOf(tempLo); val ex = xOf(tempHi)
                    drawRoundRect(
                        color = tempColor.copy(alpha = 0.45f),
                        topLeft = Offset(sx, centerY - activeTrackHeight / 2),
                        size = Size((ex - sx).coerceAtLeast(2f), activeTrackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(activeTrackHeight / 2),
                    )
                    drawRoundRect(
                        color = tempColor,
                        topLeft = Offset(sx, centerY - activeTrackHeight / 2),
                        size = Size((ex - sx).coerceAtLeast(2f), activeTrackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(activeTrackHeight / 2),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                        ),
                    )
                }
            }

            // ── 공통: 임시 A/B 마커 세로선 (파형 위에 살짝 굵게) ──
            tempStartMs?.let { ms ->
                val x = xOf(ms)
                drawLine(
                    color = tempColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            tempEndMs?.let { ms ->
                val x = xOf(ms)
                drawLine(
                    color = tempColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 2.dp.toPx(),
                )
            }

            // ── 현재 재생 위치: 세로 라인 (파형스러움) ──
            val posX = xOf(positionMs)
            drawLine(
                color = playheadColor,
                start = Offset(posX, 0f),
                end = Offset(posX, h),
                strokeWidth = 2.5.dp.toPx(),
            )
        }

        // 파형 분석 중일 때 로딩 표시
        if (waveformLoading && waveform.isEmpty()) {
            Text(
                "파형 분석 중…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/* ────────────── 시간 포맷 유틸 ────────────── */
fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    val cs = (ms % 1000) / 10  // centiseconds
    return "%d:%02d.%02d".format(m, s, cs)
}
