package com.coworkapp.loopplayer.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 좌측 ModalNavigationDrawer 의 컨텐츠.
 * 브랜드 바 · TODAY 카드 · 메인 nav · 폴더 섹션 · 세컨더리 · 푸터(storage)
 */
@Composable
fun NavigationDrawerContent(
    todayMinutes: Int,
    streakDays: Int,
    todaySongs: Int,
    todayLoops: Int,
    counts: DrawerCounts,
    activeDestination: LibraryDestination,
    onNavigate: (LibraryDestination) -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(LibraryColors.Surface)) {
        // Brand bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFC7E463), Color(0xFF6D8F24)))),
                    contentAlignment = Alignment.Center,
                ) { BarsGlyph(color = Color(0xFF0A0B0B)) }
                Column {
                    Text("구간반복 플레이어",
                        color = LibraryColors.OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    )
                    Text("v 2.4.1",
                        color = LibraryColors.OnSurfaceMuted,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.3.sp,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
            CloseButton(onClose)
        }
        HorizontalDivider(color = LibraryColors.Divider)

        // TODAY card
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            TodayCard(
                minutes  = todayMinutes,
                streak   = streakDays,
                songs    = todaySongs,
                loops    = todayLoops,
                weekBars = listOf(0.4f, 0.7f, 0.55f, 0.85f, 1f, 0.3f, 0f),
                todayIdx = 4,
            )
        }

        // Main nav list (scrollable middle area)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
        ) {
            DrawerNavRow("library",  "라이브러리",   counts.libraryCount,
                active = activeDestination == LibraryDestination.Library,
                accentDot = activeDestination == LibraryDestination.Library,
                onClick = { onNavigate(LibraryDestination.Library) })
            DrawerNavRow("mic",      "녹음",          counts.recordings,
                active = activeDestination == LibraryDestination.Recordings,
                onClick = { onNavigate(LibraryDestination.Recordings) })
            DrawerNavRow("playlist", "플레이리스트",   counts.playlists,
                active = activeDestination == LibraryDestination.Playlists,
                onClick = { onNavigate(LibraryDestination.Playlists) })
            DrawerNavRow("star",     "즐겨찾기",      counts.favorites,
                active = activeDestination == LibraryDestination.Favorites,
                onClick = { onNavigate(LibraryDestination.Favorites) })
            DrawerNavRow("clock",    "최근 연습",     null,
                active = activeDestination == LibraryDestination.RecentPractice,
                onClick = { onNavigate(LibraryDestination.RecentPractice) })
            DrawerNavRow("stats",    "연습 통계",     null,
                active = activeDestination == LibraryDestination.Stats,
                onClick = { onNavigate(LibraryDestination.Stats) })

            // FOLDERS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("FOLDERS",
                    color = LibraryColors.OnSurfaceMuted,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Text("${counts.folders.size}",
                    color = LibraryColors.OnSurfaceMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            counts.folders.forEach { (name, n) ->
                FolderRow(name = name, count = n, onClick = { onNavigate(LibraryDestination.Folder) })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                    Text("+", color = LibraryColors.OnSurfaceMuted, fontSize = 16.sp)
                }
                Text("폴더 추가", color = LibraryColors.OnSurfaceMuted, fontSize = 12.sp)
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = LibraryColors.Divider)
            Spacer(Modifier.height(10.dp))

            DrawerNavRow("import",   "파일 가져오기", null,
                onClick = { onNavigate(LibraryDestination.ImportFiles) })
            DrawerNavRow("settings", "설정",         null,
                onClick = { onNavigate(LibraryDestination.Settings) })
            DrawerNavRow("help",     "도움말",       null,
                onClick = { onNavigate(LibraryDestination.Help) })
            DrawerNavRow("info",     "앱 정보",       null,
                onClick = { onNavigate(LibraryDestination.AppInfo) })
        }

        // Footer storage
        HorizontalDivider(color = LibraryColors.Divider)
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("STORAGE",
                    color = LibraryColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.6.sp,
                )
                Text("2.4 / 8 GB",
                    color = LibraryColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x14FFFFFF)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.30f)
                        .background(LibraryColors.Accent),
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * Drawer sub-components
 * ───────────────────────────────────────────────────────────── */

@Composable
private fun CloseButton(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val s = size.minDimension
            val stroke = 1.5.dp.toPx()
            drawLine(LibraryColors.OnSurfaceMuted, Offset(s * 0.2f, s * 0.2f), Offset(s * 0.8f, s * 0.8f), stroke)
            drawLine(LibraryColors.OnSurfaceMuted, Offset(s * 0.8f, s * 0.2f), Offset(s * 0.2f, s * 0.8f), stroke)
        }
    }
}

@Composable
private fun TodayCard(
    minutes: Int,
    streak: Int,
    songs: Int,
    loops: Int,
    weekBars: List<Float>,
    todayIdx: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color(0x1AC7E463), Color(0x06C7E463))))
            .border(1.dp, Color(0x2EC7E463), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("TODAY",
                color = LibraryColors.Accent,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Row {
                Text("연속 ", color = LibraryColors.OnSurfaceMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("$streak", color = LibraryColors.Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text("일", color = LibraryColors.OnSurfaceMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("$minutes",
                color = LibraryColors.OnSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
            Text("분", color = LibraryColors.OnSurfaceMuted, fontSize = 13.sp, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
            Spacer(Modifier.weight(1f))
            Text(
                buildString {
                    append("$songs 곡 · ")
                    append("$loops 회 반복")
                },
                color = LibraryColors.OnSurfaceMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        // 7-day mini bars
        Row(
            modifier = Modifier.fillMaxWidth().height(18.dp).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            weekBars.forEachIndexed { i, v ->
                val color = when {
                    i == todayIdx -> LibraryColors.Accent
                    v == 0f       -> Color(0x14FFFFFF)
                    else          -> Color(0x52C7E463)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(maxOf(0.12f, v))
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("월","화","수","목","금","토","일").forEach {
                Text(it,
                    color = LibraryColors.OnSurfaceFaint,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun DrawerNavRow(
    iconKey: String,
    label: String,
    count: Int?,
    active: Boolean = false,
    accentDot: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) LibraryColors.AccentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            DrawerIcon(iconKey, tint = if (active) LibraryColors.Accent else LibraryColors.OnSurface)
        }
        Text(label,
            color = if (active) LibraryColors.Accent else LibraryColors.OnSurface,
            fontSize = 14.5.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = (-0.2).sp,
            modifier = Modifier.weight(1f),
        )
        if (accentDot) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(LibraryColors.Accent))
        }
        count?.let {
            Text(
                "$it",
                color = LibraryColors.OnSurfaceMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun FolderRow(name: String, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            DrawerIcon("folder", tint = LibraryColors.OnSurfaceMuted)
        }
        Text(name, color = LibraryColors.OnSurface, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        Text("$count", color = LibraryColors.OnSurfaceMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

/**
 * 라이브러리 화면 아이콘 셋 (라인 스타일).
 * 프로젝트에 이미 SVG 아이콘이 있으면 그걸로 교체하세요.
 */
@Composable
private fun DrawerIcon(key: String, tint: Color) {
    Canvas(modifier = Modifier.size(17.dp)) {
        val s = size.minDimension
        val stroke = 1.4.dp.toPx()
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(s * x1, s * y1), Offset(s * x2, s * y2), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        fun circ(cx: Float, cy: Float, r: Float, filled: Boolean = false) =
            drawCircle(tint, radius = s * r, center = Offset(s * cx, s * cy),
                style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else androidx.compose.ui.graphics.drawscope.Stroke(stroke))

        when (key) {
            "library"  -> { line(.15f,.2f,.65f,.2f); line(.15f,.2f,.15f,.8f); line(.15f,.8f,.65f,.8f); line(.65f,.2f,.65f,.8f); line(.7f,.25f,.85f,.25f); line(.7f,.25f,.7f,.8f); circ(.38f,.58f,.08f,true) }
            "mic"      -> { line(.5f,.15f,.5f,.55f); circ(.5f,.55f,.32f); line(.4f,.9f,.6f,.9f) }
            "playlist" -> { line(.15f,.25f,.65f,.25f); line(.15f,.45f,.55f,.45f); line(.15f,.65f,.45f,.65f); circ(.7f,.7f,.15f,false) }
            "star"     -> { circ(.5f,.5f,.4f,false) }
            "clock"    -> { circ(.5f,.5f,.4f,false); line(.5f,.3f,.5f,.5f); line(.5f,.5f,.7f,.6f) }
            "stats"    -> { line(.15f,.8f,.15f,.45f); line(.35f,.8f,.35f,.2f); line(.55f,.8f,.55f,.4f); line(.75f,.8f,.75f,.55f) }
            "folder"   -> { line(.15f,.3f,.4f,.3f); line(.4f,.3f,.5f,.4f); line(.5f,.4f,.85f,.4f); line(.85f,.4f,.85f,.75f); line(.15f,.3f,.15f,.75f); line(.15f,.75f,.85f,.75f) }
            "import"   -> { line(.5f,.1f,.5f,.6f); line(.35f,.45f,.5f,.6f); line(.5f,.6f,.65f,.45f); line(.18f,.8f,.82f,.8f) }
            "settings" -> { circ(.5f,.5f,.14f,false); line(.5f,.1f,.5f,.2f); line(.5f,.8f,.5f,.9f); line(.1f,.5f,.2f,.5f); line(.8f,.5f,.9f,.5f) }
            "help"     -> { circ(.5f,.5f,.4f,false); line(.5f,.78f,.5f,.78f) }
            "info"     -> { circ(.5f,.5f,.4f,false); line(.5f,.45f,.5f,.7f); circ(.5f,.32f,.04f,true) }
            else       -> circ(.5f,.5f,.1f,true)
        }
    }
}
