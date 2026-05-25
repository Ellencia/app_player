package com.coworkapp.loopplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas

/* ─────────────────────────────────────────────────────────────
 * LibraryHeader — 앱 마크(드로어 트리거) · 타이틀 · 검색 · 햄버거
 * ───────────────────────────────────────────────────────────── */
@Composable
fun LibraryHeader(
    streakDays: Int,
    streakOutOf: Int,
    totalCount: Int,
    totalLoops: Int,
    totalHours: Int,
    onDrawerOpen: () -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            DrawerTrigger(streakDays = streakDays, streakOutOf = streakOutOf, onClick = onDrawerOpen)
            Column {
                Text(
                    text = "라이브러리",
                    color = LibraryColors.OnSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                )
                Text(
                    text = "$totalCount TRACKS · ${"%,d".format(totalLoops)} LOOPS · ${totalHours}h",
                    color = LibraryColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.2.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            HeaderIconButton(onClick = onSearchClick) { SearchIcon() }
            HeaderIconButton(onClick = onMenuClick)   { MenuLinesIcon() }
        }
    }
}

@Composable
private fun DrawerTrigger(streakDays: Int, streakOutOf: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // streak ring
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = Stroke(width = 1.4f * density, cap = StrokeCap.Round)
            // base ring
            drawCircle(
                color = Color(0x2EC7E463),
                radius = size.minDimension / 2 - stroke.width / 2,
                style  = Stroke(width = 1f * density),
            )
            // progress arc
            val sweep = 360f * (streakDays.coerceAtMost(streakOutOf).toFloat() / streakOutOf.toFloat())
            drawArc(
                color    = LibraryColors.Accent,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter  = false,
                topLeft    = Offset(stroke.width / 2, stroke.width / 2),
                size       = androidx.compose.ui.geometry.Size(
                    size.width - stroke.width,
                    size.height - stroke.width,
                ),
                style = stroke,
            )
        }
        // app mark (lime gradient + bars)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFC7E463), Color(0xFF6D8F24)),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            BarsGlyph(color = Color(0xFF0A0B0B))
        }
    }
}

@Composable
private fun HeaderIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun SearchIcon() {
    Canvas(modifier = Modifier.size(17.dp)) {
        val s = size.minDimension
        drawCircle(
            color = LibraryColors.OnSurface,
            radius = s * 0.27f,
            center = Offset(s * 0.46f, s * 0.46f),
            style  = Stroke(width = 1.5f * density),
        )
        drawLine(
            color = LibraryColors.OnSurface,
            start = Offset(s * 0.67f, s * 0.67f),
            end   = Offset(s * 0.92f, s * 0.92f),
            strokeWidth = 1.5f * density,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun MenuLinesIcon() {
    Canvas(modifier = Modifier.size(17.dp)) {
        val s = size.minDimension
        val stroke = 1.5f * density
        // three lines of decreasing width
        listOf(0.30f to 0.95f, 0.50f to 0.70f, 0.70f to 0.50f).forEach { (yFrac, wFrac) ->
            drawLine(
                color = LibraryColors.OnSurface,
                start = Offset(s * 0.10f, s * yFrac),
                end   = Offset(s * (0.10f + wFrac * 0.80f), s * yFrac),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
internal fun BarsGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 14.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.minDimension
        // mimic the SVG: M2 8h2v3 M6 5v7 M9 3v10 M12 6v5 M15 8v-2
        val stroke = 1.6f * density
        val cap = StrokeCap.Round
        fun barXY(xf: Float, y1f: Float, y2f: Float) =
            drawLine(
                color = color,
                start = Offset(s * xf / 16f, s * y1f / 16f),
                end   = Offset(s * xf / 16f, s * y2f / 16f),
                strokeWidth = stroke, cap = cap,
            )
        // x=2: h-line then up — keep simple, draw 5 vertical bars
        barXY(3f,  8f, 11f)
        barXY(6f,  5f, 12f)
        barXY(9f,  3f, 13f)
        barXY(12f, 6f, 11f)
        barXY(15f, 6f, 10f)
    }
}

/* ─────────────────────────────────────────────────────────────
 * ChipRail — 가로 스크롤 칩 (모두 / 연습 중 / ★ / 클래식 …)
 * ───────────────────────────────────────────────────────────── */
@Composable
fun ChipRail(
    selected: String,
    activeCount: Int,
    onSelect: (String) -> Unit,
) {
    data class Chip(val label: String, val count: Int?, val accent: Boolean = false)
    val chips = listOf(
        Chip("모두", 290),
        Chip("연습 중", activeCount, accent = true),
        Chip("★", 24),
        Chip("클래식", 38),
        Chip("Pink Floyd", 12),
        Chip("Jazz", 16),
        Chip("가요", 41),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(chips.size) { i ->
            val c = chips[i]
            val isActive = selected == c.label
            val bg = when {
                isActive -> LibraryColors.OnSurface
                c.accent -> LibraryColors.AccentSoft
                else     -> Color.Transparent
            }
            val fg = when {
                isActive -> LibraryColors.OnAccent
                c.accent -> LibraryColors.Accent
                else     -> LibraryColors.OnSurface
            }
            val borderColor = when {
                isActive -> Color.Transparent
                c.accent -> Color(0x4DC7E463)
                else     -> LibraryColors.OutlineFaint
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .background(bg)
                    .then(if (!isActive) Modifier.border(1.dp, borderColor, RoundedCornerShape(100)) else Modifier)
                    .clickable { onSelect(c.label) }
                    .padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(c.label, color = fg, fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium)
                c.count?.let {
                    Text(
                        it.toString(),
                        color = fg.copy(alpha = 0.55f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
fun SortLine() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "최근 연습 순",
            color = LibraryColors.OnSurfaceMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.3.sp,
        )
        Text(
            "▮ 구간 · ▯ 본곡",
            color = LibraryColors.OnSurfaceMuted.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// (density extension removed — DrawScope already exposes Density so 1.5.dp.toPx() is preferred)
