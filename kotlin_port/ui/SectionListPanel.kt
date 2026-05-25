package com.coworkapp.loopplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coworkapp.loopplayer.data.LoopSection
import com.coworkapp.loopplayer.ui.theme.TimeListStyle

/**
 * Variant A 의 저장된 구간 패널.
 * - 활성 카드: primaryContainer 배경 + 둥근 아이콘 배지 primary
 * - 비활성 카드: surface 위 보라톤 surfaceContainer
 * - 시간/속도/×반복 회수는 모노 폰트
 */
@Composable
fun SectionListPanel(
    sections: List<LoopSection>,
    activeId: String?,
    currentLoopIndex: Int,
    onSelect: (LoopSection) -> Unit,
    onStop: () -> Unit,
    onEdit: (LoopSection) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "저장된 구간 · ${sections.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (activeId != null) {
                TextButton(onClick = onStop, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("반복 중지", fontSize = 13.sp)
                }
            }
        }

        if (sections.isEmpty()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Text(
                    "A → B 로 시작/끝을 잡고 ‘저장’을 누르면 여기에 추가됩니다.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            sections.forEach { sec ->
                SectionRow(
                    section = sec,
                    isActive = sec.id == activeId,
                    currentLoopIndex = currentLoopIndex,
                    onSelect = { onSelect(sec) },
                    onEdit = { onEdit(sec) },
                    onDelete = { onDelete(sec.id) },
                )
            }
        }
    }
}

@Composable
private fun SectionRow(
    section: LoopSection,
    isActive: Boolean,
    currentLoopIndex: Int,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer
             else MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        onClick = onSelect,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 아이콘 배지
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(100.dp),
                color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isActive) Icons.Default.Loop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    section.label,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${formatTimeShort(section.startMs)} → ${formatTimeShort(section.endMs)}  ·  " +
                        "%.2fx".format(section.speed) + "  ·  " +
                        if (section.loopCount > 0) "×${section.loopCount}" else "∞",
                    style = TimeListStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isActive) {
                    val rep =
                        if (section.loopCount > 0) "$currentLoopIndex/${section.loopCount}"
                        else "${currentLoopIndex}회 진행"
                    Text(
                        "▶ 반복중 · $rep",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Tune, contentDescription = "상세 설정",
                     tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "삭제",
                     tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
