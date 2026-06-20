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
 * 브랜드 바 · 메인 nav(라이브러리/녹음/즐겨찾기/최근연습) · 폴더 섹션.
 *
 * 실제 동작하는 항목만 노출 (가짜 TODAY/STORAGE 카드, 미구현 메뉴 제거).
 */
@Composable
fun NavigationDrawerContent(
    counts: DrawerCounts,
    activeDestination: LibraryDestination,
    onNavigate: (LibraryDestination) -> Unit,
    onFolderSelect: (String) -> Unit,
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
                Text("구간반복 플레이어",
                    color = LibraryColors.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                )
            }
            CloseButton(onClose)
        }
        HorizontalDivider(color = LibraryColors.Divider)

        // Main nav list (scrollable middle area)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            DrawerNavRow("library",  "라이브러리",   counts.libraryCount,
                active = activeDestination == LibraryDestination.Library,
                accentDot = activeDestination == LibraryDestination.Library,
                onClick = { onNavigate(LibraryDestination.Library) })
            DrawerNavRow("mic",      "녹음",          counts.recordings,
                active = activeDestination == LibraryDestination.Recordings,
                onClick = { onNavigate(LibraryDestination.Recordings) })
            DrawerNavRow("star",     "즐겨찾기",      counts.favorites,
                active = activeDestination == LibraryDestination.Favorites,
                onClick = { onNavigate(LibraryDestination.Favorites) })
            DrawerNavRow("clock",    "최근 연습",     null,
                active = activeDestination == LibraryDestination.RecentPractice,
                onClick = { onNavigate(LibraryDestination.RecentPractice) })

            // FOLDERS (실제 인덱싱된 폴더만, 비어있으면 섹션 자체 숨김)
            if (counts.folders.isNotEmpty()) {
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
                    FolderRow(name = name, count = n, onClick = { onFolderSelect(name) })
                }
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
