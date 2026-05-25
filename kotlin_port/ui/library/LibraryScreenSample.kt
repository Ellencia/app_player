package com.coworkapp.loopplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * 데이터 + 상태를 묶는 자급자족 호스트.
 * 실 ViewModel 이 생기기 전까지 이 호스트로 화면을 띄우면 됨.
 */
@androidx.compose.runtime.Composable
fun LibraryScreenSampleHost() {
    var state by remember { mutableStateOf(sampleLibraryState()) }
    Box(modifier = Modifier.fillMaxSize().background(LibraryColors.Background)) {
        LibraryScreen(
            state             = state,
            onSearchClick     = { /* TODO */ },
            onSortChange      = { state = state.copy(sort = it) },
            onGroupChange     = { state = state.copy(group = it) },
            onFilterToggle    = { f ->
                val cur = state.filters
                state = state.copy(filters = if (f in cur) cur - f else cur + f)
            },
            onViewOptionChange = { state = state.copy(viewOptions = it) },
            onChipSelect       = { state = state.copy(selectedChip = it) },
            onResetSheet       = { state = sampleLibraryState() },
            onApplySheet       = { /* persist or refresh list */ },
            onSongClick        = { /* navigate to player */ },
            onSongLongPress    = { /* enter multi-select mode */ },
            onNavigate         = { /* host route */ },
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@androidx.compose.runtime.Composable
private fun LibraryScreenPreview() {
    LibraryScreenSampleHost()
}

/** 시안에서 쓴 18곡 sample data. */
internal fun sampleLibraryState(): LibraryUiState {
    val now = System.currentTimeMillis()
    fun daysAgo(d: Int) = now - d * 24L * 3600 * 1000
    val songs = listOf(
        LibrarySong("s01","쇼팽 녹턴 Op.9 No.2",       "Chopin · Arthur Rubinstein", 274_000L, 5, daysAgo(0),  38, 60,  "Eb", "클래식", 12,  favorite=true,  isActive=true),
        LibrarySong("s02","Vincent",                   "Don McLean",                  242_000L, 2, daysAgo(1),  12, 74,  "A",  "Pop",    188,                  isActive=true),
        LibrarySong("s03","가까이하기엔 너무먼 당신",     "이광조",                       239_000L, 1, daysAgo(4),   4, 88,  "Dm", "가요",   318),
        LibrarySong("s04","Shine On You Crazy Diamond (Pts. 6-9)","Pink Floyd",       747_000L, 3, daysAgo(2),   7, 64,  "Gm", "Floyd",  158, favorite=true),
        LibrarySong("s05","Shine On You Crazy Diamond (Pts. 1-5)","Pink Floyd",       811_000L, 6, daysAgo(7),  22, 64,  "Gm", "Floyd",  158, favorite=true),
        LibrarySong("s06","Hey You",                   "Pink Floyd",                  278_000L, 4, daysAgo(0),  19, 78,  "Em", "Floyd",  158, isActive=true),
        LibrarySong("s07","If You Leave Me Now",       "Chicago",                     275_000L, 2, daysAgo(14),  3, 92,  "C",  "Pop",     32),
        LibrarySong("s08","Wish You Were Here",        "Pink Floyd",                  293_000L, 6, daysAgo(0),  56, 60,  "G",  "Floyd",  158, favorite=true, isActive=true),
        LibrarySong("s09","Spain",                     "Chick Corea",                 522_000L, 8, daysAgo(3),  41, 128, "Bm", "Jazz",   268, favorite=true),
        LibrarySong("s10","Autumn Leaves",             "Bill Evans Trio",             371_000L, 4, daysAgo(9),  14, 124, "Em", "Jazz",   268),
        LibrarySong("s11","비도 오고 그래서",             "헤이즈",                       212_000L, 2, daysAgo(21),  6, 96,  "C",  "가요",   348),
        LibrarySong("s12","Black Bird",                "The Beatles",                 138_000L, 3, daysAgo(1),  28, 95,  "G",  "Pop",     48, favorite=true, isActive=true),
        LibrarySong("s13","River Flows in You",        "Yiruma",                      190_000L, 4, daysAgo(5),  17, 64,  "A",  "클래식", 200),
        LibrarySong("s14","Comptine d'un autre été",   "Yann Tiersen",                141_000L, 5, daysAgo(0),  64, 90,  "Em", "클래식", 200, favorite=true, isActive=true),
        LibrarySong("s15","So What",                   "Miles Davis",                 562_000L, 7, daysAgo(30),  2, 136, "Dm", "Jazz",   268),
        LibrarySong("s16","봄이 좋냐",                   "10cm",                        248_000L, 1, daysAgo(2),   5, 108, "D",  "가요",   348),
        LibrarySong("s17","Clair de Lune",             "Debussy",                     312_000L, 6, daysAgo(11),  9, 66,  "Db", "클래식", 200, favorite=true),
        LibrarySong("s18","Take Five",                 "Dave Brubeck",                324_000L, 3, daysAgo(6),  11, 172, "Ebm","Jazz",   268),
    )
    return LibraryUiState(
        songs        = songs,
        totalCount   = 290,
        totalLoops   = 1_247,
        totalMinutes = 132 * 60,
        activeCount  = songs.count { it.isActive },
    )
}
