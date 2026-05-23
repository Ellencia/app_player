package com.coworkapp.loopplayer.ui

import com.coworkapp.loopplayer.data.LoopSection

/**
 * PlayerScreen 에서 호출하는 모든 사용자 액션을 한 곳에 모음.
 * - 콜백 prop drilling 줄이기
 * - 새 액션 추가 시 한 군데만 수정
 */
data class PlayerActions(
    val onOpenFile: () -> Unit,
    val onTogglePlay: () -> Unit,
    val onSeekTo: (Long) -> Unit,
    val onSeekRelative: (Long) -> Unit,
    val onMarkStart: () -> Unit,
    val onMarkEnd: () -> Unit,
    val onSaveTempSection: (String?) -> Unit,
    val onClearTemp: () -> Unit,
    val onSelectSection: (LoopSection) -> Unit,
    val onStopLoop: () -> Unit,
    val onRestartActive: () -> Unit,
    val onSetSpeed: (Float) -> Unit,
    val onUpdateSection: (LoopSection) -> Unit,
    val onDeleteSection: (String) -> Unit,
    val onRestoreSection: (LoopSection, Int) -> Unit,
)
