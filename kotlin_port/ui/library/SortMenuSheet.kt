package com.coworkapp.loopplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 우상단 햄버거 탭 → 모달 바텀 시트 컨텐츠.
 * 섹션: SORT · GROUP · QUICK FILTER · VIEW · ACTIONS
 *   + 하단 sticky 푸터 (적용/닫기)
 */
@Composable
fun SortMenuSheetContent(
    state: LibraryUiState,
    onSort: (LibrarySort) -> Unit,
    onGroup: (LibraryGroup) -> Unit,
    onFilter: (LibraryQuickFilter) -> Unit,
    onView: (LibraryViewOptions) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text("보기 & 정렬",
                    color = LibraryColors.OnSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                )
                Text(
                    "${state.totalCount} TRACKS · ${state.activeCount} ACTIVE",
                    color = LibraryColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.2.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Text("초기화",
                color = LibraryColors.OnSurfaceMuted,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onReset() },
            )
        }

        // Scrollable body
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            SectionHeader("SORT")
            LibrarySort.values().forEach { s ->
                SortRow(
                    label = s.label,
                    selected = state.sort == s,
                    descending = state.sortDescending,
                    onClick  = { onSort(s) },
                )
            }
            SectionDivider()

            SectionHeader("GROUP")
            SegmentedRow(
                options = LibraryGroup.values().toList(),
                selected = state.group,
                onSelect = onGroup,
                labelOf = { it.label },
            )
            SectionDivider()

            SectionHeader("QUICK FILTER")
            FlowChips(
                options = LibraryQuickFilter.values().toList(),
                isOn    = { it in state.filters },
                onToggle = onFilter,
                labelOf = { it.label },
            )
            SectionDivider()

            SectionHeader("VIEW")
            ToggleRow("웨이브폼 표시", state.viewOptions.showWaveform) {
                onView(state.viewOptions.copy(showWaveform = it))
            }
            ToggleRow("BPM · 키 표시", state.viewOptions.showBpmKey) {
                onView(state.viewOptions.copy(showBpmKey = it))
            }
            ToggleRow("컬러 썸네일", state.viewOptions.showThumbnail) {
                onView(state.viewOptions.copy(showThumbnail = it))
            }
            ToggleRow("연습 중인 곡 상단 고정", state.viewOptions.pinActiveOnTop) {
                onView(state.viewOptions.copy(pinActiveOnTop = it))
            }
            SectionDivider()

            SectionHeader("ACTIONS")
            ActionRow("일괄 선택", hint = "롱프레스로도 시작") {}
            ActionRow("폴더에서 가져오기") {}
            ActionRow("연습 통계 보기") {}
            ActionRow("설정") {}

            Spacer(Modifier.height(4.dp))
        }

        // Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LibraryColors.SurfaceElevated)
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onApply,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(100),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LibraryColors.Accent,
                    contentColor   = LibraryColors.OnAccent,
                ),
            ) { Text("적용", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.height(46.dp),
                shape = RoundedCornerShape(100),
                border = androidx.compose.foundation.BorderStroke(1.dp, LibraryColors.OutlineFaint),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = LibraryColors.OnSurface,
                ),
            ) { Text("닫기", fontSize = 14.sp) }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * Sheet sub-components
 * ───────────────────────────────────────────────────────────── */

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = LibraryColors.OnSurfaceMuted,
        fontSize = 10.5.sp,
        letterSpacing = 1.4.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        modifier = modifier.padding(top = 14.dp, bottom = 8.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = LibraryColors.DividerStrong, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun SortRow(label: String, selected: Boolean, descending: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioDot(selected = selected)
        Text(
            label,
            color = LibraryColors.OnSurface.copy(alpha = if (selected) 1f else 0.85f),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text(
                if (descending) "↓" else "↑",
                color = LibraryColors.OnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(if (selected) LibraryColors.Accent else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (selected) LibraryColors.Accent else Color(0x38FFFFFF),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Text("✓", color = LibraryColors.OnAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelOf: (T) -> String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100))
            .background(Color(0x0AFFFFFF))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { opt ->
            val isActive = opt == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(100))
                    .background(if (isActive) LibraryColors.OnSurface else Color.Transparent)
                    .clickable { onSelect(opt) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    labelOf(opt),
                    color = if (isActive) LibraryColors.OnAccent else LibraryColors.OnSurface,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun <T> FlowChips(
    options: List<T>,
    isOn: (T) -> Boolean,
    onToggle: (T) -> Unit,
    labelOf: (T) -> String,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { opt ->
            val on = isOn(opt)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .background(if (on) LibraryColors.Accent else Color.Transparent)
                    .then(if (!on) Modifier.border(1.dp, LibraryColors.OutlineFaint, RoundedCornerShape(100)) else Modifier)
                    .clickable { onToggle(opt) }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            ) {
                Text(
                    labelOf(opt),
                    color = if (on) LibraryColors.OnAccent else LibraryColors.OnSurface,
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!on) }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = LibraryColors.OnSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = on,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = LibraryColors.OnAccent,
                checkedTrackColor   = LibraryColors.Accent,
                uncheckedThumbColor = LibraryColors.OnSurface,
                uncheckedTrackColor = Color(0x26FFFFFF),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionRow(label: String, hint: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // simple bullet placeholder; project's iconography goes here
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(LibraryColors.OnSurface.copy(alpha = 0.6f)))
        }
        Text(label, color = LibraryColors.OnSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (hint != null) {
            Text(hint, color = LibraryColors.OnSurfaceMuted, fontSize = 10.5.sp)
        }
        Text("›", color = LibraryColors.OnSurfaceFaint, fontSize = 16.sp)
    }
}
