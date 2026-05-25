package com.coworkapp.loopplayer.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/* ─────────────────────────────────────────────────────────────
 * SongRow — 한 곡의 2행 레이아웃
 *   row 1: 썸네일 · 제목 · (mm:ss · BPM·키)
 *   row 2: └─ 미니 웨이브폼 + 구간 마커 · 아티스트 · (lastPracticed · loops)
 * ───────────────────────────────────────────────────────────── */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: LibrarySong,
    options: LibraryViewOptions,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .then(
                if (song.isActive) Modifier.background(LibraryColors.AccentSofter)
                else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (options.showThumbnail) {
                ColorThumbnail(song = song, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(11.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                // Title line + duration·bpm
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        song.title,
                        color = LibraryColors.OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    DurationMeta(song = song, options = options)
                }

                Spacer(Modifier.height(4.dp))

                // Waveform + artist row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (options.showWaveform) {
                        MiniWaveform(
                            seed       = song.hueDeg + song.sections * 7,
                            sections   = song.sections,
                            active     = song.isActive,
                            modifier   = Modifier
                                .width(168.dp)
                                .height(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            song.artist,
                            color = LibraryColors.OnSurfaceMuted,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        LastPracticedMeta(song = song)
                    }
                }
            }
        }
        Spacer(Modifier.height(1.dp))
        HorizontalDivider(color = LibraryColors.Divider, thickness = 1.dp)
    }
}

@Composable
private fun DurationMeta(song: LibrarySong, options: LibraryViewOptions) {
    Row {
        Text(
            formatDuration(song.durationMs),
            color = LibraryColors.OnSurface,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 0.2.sp,
        )
        if (options.showBpmKey && song.bpm != null && song.musicKey != null) {
            Text(
                "  ·  ",
                color = LibraryColors.OnSurfaceFaint,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                "${song.bpm}·${song.musicKey}",
                color = LibraryColors.OnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 0.2.sp,
            )
        }
    }
}

@Composable
private fun LastPracticedMeta(song: LibrarySong) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (song.isActive) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(100))
                    .background(LibraryColors.Accent),
            )
        }
        Text(
            "${formatLast(song.lastPracticed)}·${song.loops}회",
            color = LibraryColors.OnSurfaceMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
    }
}

/* ─────────────────────────────────────────────────────────────
 * ColorThumbnail — 곡 hue 기반 컬러 그라디언트 + 이니셜.
 *   active 곡은 라임 inset 보더.
 *   즐겨찾기는 우상단 작은 별.
 * ───────────────────────────────────────────────────────────── */
@Composable
fun ColorThumbnail(song: LibrarySong, modifier: Modifier = Modifier) {
    val base    = hsl(song.hueDeg.toFloat(),                     0.42f, 0.38f)
    val baseAlt = hsl(((song.hueDeg + 24) % 360).toFloat(),       0.30f, 0.22f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Brush.linearGradient(listOf(base, baseAlt)))
            .then(
                if (song.isActive)
                    Modifier.border(
                        width = 1.5.dp,
                        color = LibraryColors.Accent.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(9.dp),
                    )
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = song.title.firstOrNull { it.isLetter() }?.uppercase() ?: "·",
            color = Color(0xD1FFFFFF),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )
        if (song.favorite) {
            Canvas(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 3.dp),
            ) {
                // small filled star (approx)
                val s = size.minDimension
                val path = androidx.compose.ui.graphics.Path().apply {
                    val cx = s / 2; val cy = s / 2
                    val outer = s * 0.5f; val inner = s * 0.22f
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outer else inner
                        val a = (-PI / 2 + i * PI / 5).toFloat()
                        val x = cx + r * cos(a)
                        val y = cy + r * sin(a)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path = path, color = Color(0xD9FFFFFF))
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * MiniWaveform — 시안의 SVG 와 동일 알고리즘.
 *   - 56개 막대 (seeded RNG envelope)
 *   - 구간 N개를 가로 폭에 분배해서 반투명 오버레이 + 보더
 *   - 구간 안쪽 막대는 밝게, 바깥은 어둡게
 *   - active 곡: 라임 톤 + 28% 지점에 playhead
 * ───────────────────────────────────────────────────────────── */
@Composable
fun MiniWaveform(
    seed: Int,
    sections: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val bars = generateMiniWave(seed = seed, count = 56)
        val ranges = distributeSections(n = sections, seed = seed + 3)

        // overlays
        ranges.forEach { (a, b) ->
            val x = a * w; val rw = (b - a) * w
            val rectColor = if (active) Color(0x24C7E463) else Color(0x0FFFFFFF)
            drawRoundRect(
                color = rectColor,
                topLeft = Offset(x, 1f),
                size    = Size(rw, h - 2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
            )
            drawRoundRect(
                color = if (active) Color(0x80C7E463) else Color(0x2EFFFFFF),
                topLeft = Offset(x, 1f),
                size    = Size(rw, h - 2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                style   = Stroke(width = 0.8f),
            )
        }

        // bars
        val barW = 1.6.dp.toPx()
        val gap  = 1.6.dp.toPx()
        val stride = barW + gap
        bars.forEachIndexed { i, v ->
            val x = i * stride + 1f
            if (x > w - barW) return@forEachIndexed
            val barH = max(1.2f, v * (h - 4f))
            val tx = x / w
            val inSec = ranges.any { (a, b) -> tx in a..b }
            val color = when {
                inSec && active   -> LibraryColors.Accent.copy(alpha = 0.95f)
                inSec             -> LibraryColors.OnSurface.copy(alpha = 0.85f)
                else              -> Color(0x8C5A5D60)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (h - barH) / 2f),
                size = Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.8f, 0.8f),
            )
        }

        // playhead for active rows
        if (active) {
            drawRect(
                color = Color(0xD9FFFFFF),
                topLeft = Offset(w * 0.28f, 0f),
                size = Size(1.4f, h),
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * Utility math — seed RNG, section distribution, formatting, HSL
 * (Pure functions — keep here so the row is self-contained.)
 * ───────────────────────────────────────────────────────────── */

internal fun generateMiniWave(seed: Int, count: Int): FloatArray {
    var s = seed.toLong().let { if (it == 0L) 1L else it }
    val rng = { s = (s * 9301 + 49297) % 233280; s / 233280.0 }
    return FloatArray(count) { i ->
        val t = i.toDouble() / count
        val env = 0.4 + 0.6 * sin(t * PI)
        val beat = 0.5 + 0.5 * abs(sin(t * PI * 6 + seed))
        val noise = 0.6 + 0.4 * rng()
        max(0.12, min(1.0, env * beat * noise)).toFloat()
    }
}

internal fun distributeSections(n: Int, seed: Int): List<Pair<Float, Float>> {
    if (n <= 0) return emptyList()
    var s = seed.toLong().let { if (it == 0L) 1L else it }
    val rng = { s = (s * 9301 + 49297) % 233280; s / 233280.0 }
    val out = mutableListOf<Pair<Float, Float>>()
    var cursor = 0.05
    for (i in 0 until n) {
        val remain = 0.95 - cursor
        val slot = remain / (n - i)
        val start = cursor + slot * 0.15 * rng()
        val w     = slot * (0.35 + 0.45 * rng())
        out += (start.toFloat() to (start + w).toFloat())
        cursor = start + w + slot * 0.05
    }
    return out
}

internal fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}

internal fun formatLast(epoch: Long?): String {
    if (epoch == null) return "미연습"
    val now = System.currentTimeMillis()
    val d = ((now - epoch) / (24 * 3600 * 1000)).toInt().coerceAtLeast(0)
    return when {
        d == 0  -> "오늘"
        d == 1  -> "어제"
        d <  7  -> "${d}일 전"
        d < 14  -> "지난주"
        d < 30  -> "${d / 7}주 전"
        else    -> "${d / 30}달 전"
    }
}

/** HSL → Color (sRGB). Compose Color 는 HSL 직접 입력 시 채도/명도 해석이 미묘해서 직접 변환. */
internal fun hsl(h: Float, s: Float, l: Float): Color {
    val c = (1 - abs(2 * l - 1)) * s
    val hp = h / 60f
    val x = c * (1 - abs(hp.mod(2f) - 1))
    val (r1, g1, b1) = when {
        hp < 1 -> Triple(c, x, 0f)
        hp < 2 -> Triple(x, c, 0f)
        hp < 3 -> Triple(0f, c, x)
        hp < 4 -> Triple(0f, x, c)
        hp < 5 -> Triple(x, 0f, c)
        else   -> Triple(c, 0f, x)
    }
    val m = l - c / 2
    return Color(r1 + m, g1 + m, b1 + m)
}

// (Pure Compose — no external icon libraries required.)
