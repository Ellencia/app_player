package com.coworkapp.loopplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas

/**
 * 라이브러리 하단에 떠 있는 미니 플레이어 바.
 *
 * 현재 재생 중인 트랙이 있을 때만 노출. 탭하면 플레이어 화면으로,
 * 우측 토글 버튼으로 재생/일시정지.
 */
@Composable
fun MiniPlayerBar(
    title: String,
    isPlaying: Boolean,
    progress: Float, // 0..1
    onClick: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(LibraryColors.SurfaceElevated)) {
        // 상단 진행 막대 (얇게)
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(LibraryColors.DividerStrong),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(LibraryColors.Accent),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 라임 앱마크 (작게)
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LibraryColors.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val s = size.minDimension
                    val stroke = 1.6f * density
                    fun bar(xf: Float, y1f: Float, y2f: Float) = drawLine(
                        LibraryColors.Accent,
                        Offset(s * xf, s * y1f), Offset(s * xf, s * y2f),
                        stroke, cap = StrokeCap.Round,
                    )
                    bar(0.2f, 0.55f, 0.7f)
                    bar(0.4f, 0.32f, 0.8f)
                    bar(0.6f, 0.2f, 0.85f)
                    bar(0.8f, 0.4f, 0.72f)
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = LibraryColors.OnSurface,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (isPlaying) "재생 중" else "일시정지",
                    color = LibraryColors.OnSurfaceMuted,
                    fontSize = 10.5.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            // 재생/일시정지 토글
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                PlayPauseGlyph(isPlaying = isPlaying)
            }
        }
        HorizontalDivider(color = LibraryColors.Divider)
    }
}

@Composable
private fun PlayPauseGlyph(isPlaying: Boolean) {
    Canvas(Modifier.size(18.dp)) {
        val s = size.minDimension
        if (isPlaying) {
            // 두 막대 (pause)
            val barW = s * 0.22f
            drawRect(LibraryColors.OnSurface, Offset(s * 0.28f - barW / 2, s * 0.2f), Size(barW, s * 0.6f))
            drawRect(LibraryColors.OnSurface, Offset(s * 0.72f - barW / 2, s * 0.2f), Size(barW, s * 0.6f))
        } else {
            // 삼각형 (play)
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(s * 0.3f, s * 0.22f)
                lineTo(s * 0.3f, s * 0.78f)
                lineTo(s * 0.8f, s * 0.5f)
                close()
            }
            drawPath(path, LibraryColors.OnSurface)
        }
    }
}
