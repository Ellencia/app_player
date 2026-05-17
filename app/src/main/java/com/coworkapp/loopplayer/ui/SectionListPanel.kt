package com.coworkapp.loopplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coworkapp.loopplayer.data.LoopSection

/**
 * 저장된 구간 리스트 패널
 * - 항목을 누르면 즉시 그 구간 반복 시작
 * - 활성 구간은 강조 표시
 * - 우측 톱니버튼 → 상세 설정, X → 삭제
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
    Column {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "저장된 구간 (${sections.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (activeId != null) {
                TextButton(onClick = onStop) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(Modifier.width(4.dp))
                    Text("반복 중지")
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (sections.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "A → B 로 시작/끝을 잡고 '저장'을 누르면 여기에 추가됩니다.",
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
                Spacer(Modifier.height(8.dp))
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onSelect,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isActive) Icons.Default.Loop else Icons.Default.PlayCircle,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    section.label,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${formatTime(section.startMs)} → ${formatTime(section.endMs)}  " +
                        "· ${"%.2fx".format(section.speed)}" +
                        if (section.loopCount > 0) "  · ×${section.loopCount}회" else "  · ∞",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isActive) {
                    val rep = if (section.loopCount > 0) "$currentLoopIndex/${section.loopCount}"
                    else "${currentLoopIndex}회 진행"
                    Text(
                        "▶ 반복 중 · $rep",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Tune, contentDescription = "상세 설정")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "삭제")
            }
        }
    }
}
