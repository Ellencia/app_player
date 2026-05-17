package com.coworkapp.loopplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coworkapp.loopplayer.data.LoopSection

/**
 * 한 구간의 상세 설정을 편집하는 다이얼로그
 * - 이름
 * - 시작/끝 시간 (밀리초 단위 텍스트 입력)
 * - 반복 횟수 (0 = 무한)
 * - 속도 (0.5 ~ 2.0)
 * - 구간 사이 간격 (ms)
 */
@Composable
fun AdvancedSettingsDialog(
    section: LoopSection,
    duration: Long,
    onDismiss: () -> Unit,
    onSave: (LoopSection) -> Unit,
) {
    var label by remember { mutableStateOf(section.label) }
    var startMs by remember { mutableStateOf(section.startMs) }
    var endMs by remember { mutableStateOf(section.endMs) }
    var loopCount by remember { mutableStateOf(section.loopCount.toString()) }
    var speed by remember { mutableStateOf(section.speed) }
    var gapMs by remember { mutableStateOf(section.gapMs.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("구간 상세 설정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 시작 시간
                Text("시작: ${formatTime(startMs)}", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = startMs.toFloat(),
                    valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                    onValueChange = {
                        startMs = it.toLong()
                        if (endMs < startMs) endMs = startMs
                    },
                )

                // 끝 시간
                Text("끝: ${formatTime(endMs)}", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = endMs.toFloat(),
                    valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                    onValueChange = {
                        endMs = it.toLong()
                        if (startMs > endMs) startMs = endMs
                    },
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = loopCount,
                        onValueChange = { v -> loopCount = v.filter { it.isDigit() } },
                        label = { Text("반복 횟수 (0=무한)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = gapMs,
                        onValueChange = { v -> gapMs = v.filter { it.isDigit() } },
                        label = { Text("간격(ms)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text("속도: %.2fx".format(speed), fontWeight = FontWeight.SemiBold)
                Slider(
                    value = speed,
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    onValueChange = { speed = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    section.copy(
                        label = label.ifBlank { section.label },
                        startMs = startMs.coerceAtLeast(0L),
                        endMs = endMs.coerceAtLeast(startMs + 100L),
                        loopCount = loopCount.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                        speed = speed,
                        gapMs = gapMs.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                    )
                )
            }) { Text("저장") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
