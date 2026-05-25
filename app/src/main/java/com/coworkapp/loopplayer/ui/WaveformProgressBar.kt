package com.coworkapp.loopplayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.coworkapp.loopplayer.ui.theme.LoopColors
import com.coworkapp.loopplayer.ui.theme.WaveformMarkerStyle

/**
 * Variant A 의 진행바 ─ 가운데 기준 미러링된 막대 파형.
 *
 * 컬러 의미:
 *  ▮ 안 지나간 부분   = surfaceVariant 톤 (연회색)
 *  ▮ 지나간 부분      = onSurfaceVariant 톤 (진회색)
 *  ▮ 활성 반복 구간  = primary (보라) ─ 진행여부 무관
 *  ▮ A-B 임시 구간   = tertiary (rust)
 *  ▮ 현재 재생 위치 = primary 세로선 + 헤드 점
 *  ▮ A / B 마커     = tertiary 점선 + 라벨 칩
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun WaveformProgressBar(
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
    val unplayed = MaterialTheme.colorScheme.outlineVariant
    val played   = MaterialTheme.colorScheme.onSurfaceVariant
    val active   = MaterialTheme.colorScheme.primary
    val temp     = MaterialTheme.colorScheme.tertiary
    val playhead = MaterialTheme.colorScheme.primary
    val labelOnTemp = MaterialTheme.colorScheme.onTertiary
    val surface  = MaterialTheme.colorScheme.surface

    val measurer = rememberTextMeasurer()
    val labelStyle = remember(labelOnTemp) {
        WaveformMarkerStyle.copy(color = labelOnTemp)
    }

    var widthPx by remember { mutableStateOf(1f) }
    fun toMs(x: Float): Long {
        if (durationMs <= 0L) return 0L
        val ratio = (x / widthPx).coerceIn(0f, 1f)
        return (ratio * durationMs).toLong()
    }

    Box(
        modifier = modifier
            .pointerInput(durationMs) {
                detectTapGestures(onTap = { offset -> onSeekTo(toMs(offset.x)) })
            }
            .pointerInput(durationMs) {
                detectDragGestures(onDrag = { change, _ -> onSeekTo(toMs(change.position.x)) })
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
                // ── 미러링된 막대 파형 ──
                val barCount = waveform.size
                val gap = 1.dp.toPx()
                val barTotal = w / barCount
                val barWidth = (barTotal - gap).coerceAtLeast(1f)
                val maxHalf = h * 0.42f
                val minHalf = 2.dp.toPx() / 2f

                for (i in 0 until barCount) {
                    val barX = i * barTotal
                    val tCenter = ((i + 0.5f) / barCount * dur).toLong()
                    val amp = waveform[i].coerceIn(0f, 1f)
                    val half = (amp * maxHalf).coerceAtLeast(minHalf)

                    // 우선순위: 활성 반복 > 임시 A-B > 진행도/안지남
                    val color = when {
                        activeStartMs != null && activeEndMs != null &&
                            tCenter in activeStartMs..activeEndMs -> active
                        tempLo != null && tempHi != null &&
                            tCenter in tempLo..tempHi -> temp
                        tCenter <= positionMs -> played
                        else -> unplayed
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(barX + 0.4f, centerY - half),
                        size = Size(barWidth, half * 2f),
                        cornerRadius = CornerRadius(barWidth / 3f),
                    )
                }
            } else {
                // ── Fallback: 파형 로드 전 단순 라인 ──
                val trackH = 8.dp.toPx()
                drawRoundRect(
                    color = unplayed,
                    topLeft = Offset(0f, centerY - trackH / 2),
                    size = Size(w, trackH),
                    cornerRadius = CornerRadius(trackH / 2),
                )
                val posX = xOf(positionMs)
                if (posX > 0f) drawRoundRect(
                    color = played,
                    topLeft = Offset(0f, centerY - trackH / 2),
                    size = Size(posX, trackH),
                    cornerRadius = CornerRadius(trackH / 2),
                )
                if (activeStartMs != null && activeEndMs != null && activeEndMs > activeStartMs) {
                    val sx = xOf(activeStartMs); val ex = xOf(activeEndMs)
                    drawRoundRect(
                        color = active,
                        topLeft = Offset(sx, centerY - trackH / 2),
                        size = Size((ex - sx).coerceAtLeast(2f), trackH),
                        cornerRadius = CornerRadius(trackH / 2),
                    )
                }
            }

            // ── A / B 마커 세로선 + 라벨 ──
            val markerLineWidth = 1.5.dp.toPx()
            val labelW = 18.dp.toPx()
            val labelH = 14.dp.toPx()
            val labelR = 4.dp.toPx()

            tempStartMs?.let { ms ->
                val x = xOf(ms)
                drawLine(
                    color = temp,
                    start = Offset(x, 0f), end = Offset(x, h),
                    strokeWidth = markerLineWidth,
                )
                drawRoundRect(
                    color = temp,
                    topLeft = Offset(x - labelW / 2f, 0f),
                    size = Size(labelW, labelH),
                    cornerRadius = CornerRadius(labelR),
                )
                val tr = measurer.measure("A", labelStyle)
                drawText(
                    textLayoutResult = tr,
                    topLeft = Offset(x - tr.size.width / 2f, (labelH - tr.size.height) / 2f),
                )
            }
            tempEndMs?.let { ms ->
                val x = xOf(ms)
                drawLine(
                    color = temp,
                    start = Offset(x, 0f), end = Offset(x, h),
                    strokeWidth = markerLineWidth,
                )
                drawRoundRect(
                    color = temp,
                    topLeft = Offset(x - labelW / 2f, 0f),
                    size = Size(labelW, labelH),
                    cornerRadius = CornerRadius(labelR),
                )
                val tr = measurer.measure("B", labelStyle)
                drawText(
                    textLayoutResult = tr,
                    topLeft = Offset(x - tr.size.width / 2f, (labelH - tr.size.height) / 2f),
                )
            }

            // ── 현재 재생 위치 ──
            val px = xOf(positionMs)
            drawLine(
                color = playhead,
                start = Offset(px, 0f), end = Offset(px, h),
                strokeWidth = 2.dp.toPx(),
            )
            // 헤드 점 (흰 테두리 + primary fill)
            drawCircle(color = surface, radius = 6.dp.toPx(), center = Offset(px, centerY))
            drawCircle(color = playhead, radius = 5.dp.toPx(), center = Offset(px, centerY))
        }

        if (waveformLoading && waveform.isEmpty()) {
            Text(
                "파형 분석 중…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
