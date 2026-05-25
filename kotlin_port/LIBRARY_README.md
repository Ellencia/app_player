# Variant E → Compose 포팅 (Library screen)

웹 시안의 **E안 (썸네일 + 웨이브폼 + 좌측 드로어 + 하단 시트)** 을 안드로이드 Compose 로 옮긴 모듈입니다.
플레이어 화면(Variant A) 옆에 그대로 떨어뜨릴 수 있게 패키지·테마·시그니처를 맞춰뒀습니다.

```
com.coworkapp.loopplayer.ui.library
├── LibraryColors.kt              — hex 토큰 (다크 전용)
├── LibraryModels.kt              — Song / Sort / Group / Filter / UiState
├── LibraryScreen.kt              — 진입 컴포저블 (Drawer + Body + Sheet)
├── LibraryHeader.kt              — 헤더 / 칩 rail / sort line / 앱마크
├── SongRow.kt                    — 행 + 컬러 썸네일 + 미니 웨이브폼
├── SortMenuSheet.kt              — 우상단 햄버거 → 바텀시트 (정렬·그룹·필터·보기)
├── NavigationDrawerContent.kt    — 좌측 드로어 (TODAY 카드 + nav + storage)
└── LibraryScreenSampleHost.kt    — ViewModel 없이 띄울 수 있는 self-hosting 데모
```

## 가장 빠르게 띄우기

```kotlin
setContent {
  LoopPlayerTheme(useDarkTheme = true) {
    LibraryScreenSampleHost()
  }
}
```

## ViewModel 과 연결하기

`LibraryScreen` 은 stateless 컴포저블입니다. ViewModel 이 `LibraryUiState` 한 덩어리만 expose
하면 됩니다.

```kotlin
@Composable
fun LibraryRoute(vm: LibraryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LibraryScreen(
        state             = state,
        onSearchClick     = vm::onSearchClick,
        onSortChange      = vm::setSort,
        onGroupChange     = vm::setGroup,
        onFilterToggle    = vm::toggleFilter,
        onViewOptionChange= vm::setViewOptions,
        onChipSelect      = vm::selectChip,
        onResetSheet      = vm::resetSheet,
        onApplySheet      = vm::applySheet,
        onSongClick       = vm::openSong,
        onSongLongPress   = vm::enterMultiSelect,
        onNavigate        = vm::navigate,
    )
}
```

## 디자인 토큰 (`LibraryColors`)

| 토큰              | 값                | 용도 |
|------------------|------------------|------|
| Background       | `#0D0E0E`        | 화면 베이스 |
| Surface          | `#131414`        | 드로어 패널 |
| SurfaceElevated  | `#171818`        | 바텀시트 |
| OnSurface        | `#ECECEA`        | 제목/본문 |
| OnSurfaceMuted   | `#8E9194`        | 아티스트/메타 |
| OnSurfaceFaint   | `#52555A`        | 구분 점 |
| Divider          | `rgba(255,5%)`   | 행 구분선 |
| OutlineFaint     | `rgba(255,14%)`  | 칩 보더 |
| Accent           | `#C7E463`        | 연습 중 / 진행 / CTA |
| AccentSoft       | `accent @ 10%`   | active 행 / 드로어 active 칸 배경 |
| AccentSofter     | `accent @ 3.5%`  | active 행 tint |
| OnAccent         | `#0D0E0E`        | 라임 위 텍스트 |

> **라임은 "연습 중" 상태에만 쓰세요.** 일반 UI 보조색으로 확장하면 active 신호가 약해집니다.

## 폰트

- 본문/제목: 기존 프로젝트의 Pretendard
- 메타·카운터·모노 라벨: `FontFamily.Monospace` (실 빌드에는 JetBrains Mono ttf 권장)

## 상호작용 매핑

| 트리거 | 결과 | 코드 |
|--------|------|------|
| 헤더 좌측 앱마크 탭 | 좌측 드로어 열림 | `drawerState.open()` |
| 좌측 엣지 스와이프-라이트 | 좌측 드로어 열림 | `ModalNavigationDrawer` 기본 제스처 |
| 헤더 우측 햄버거 탭 | 하단 시트 열림 (정렬·그룹·필터·보기) | `sheetOpen = true` |
| 헤더 우측 돋보기 탭 | `onSearchClick()` | 호스트가 검색 라우트로 |
| 행 탭 | `onSongClick(song)` | 플레이어로 이동 |
| 행 롱프레스 | `onSongLongPress(song)` | multi-select 진입 |
| 시트 "초기화" | `onResetSheet()` | 모든 정렬/필터/뷰 옵션 default |
| 시트 "적용" / 드래그-다운 / 닫기 | 시트 닫힘 + 옵션 반영 | `onApplySheet()` |

## 커스텀 그리기

- **컬러 썸네일** — `hsl()` 유틸로 `Color.hsl` 의 표준 sRGB 매핑을 직접 구현. 곡의 `hueDeg`
  하나만 들어가면 컬러 페어를 만들 수 있게.
- **미니 웨이브폼** — `generateMiniWave(seed, 56)` 로 seeded RNG envelope 생성. `distributeSections()`
  로 N개 구간을 [0..1] 폭에 분배해 보더 + 채움. 막대 색은 (구간 안쪽 / 바깥) × (active / 일반) 4가지.
  실제 오디오에서 뽑은 PCM peaks 가 있으면 `generateMiniWave` 만 갈아끼우면 됨.
- **드로어 아이콘** — 임시로 `Canvas` 로 라인 아이콘을 그렸습니다 (`DrawerIcon(key)`).
  프로젝트의 vector drawables / Material Icons 로 교체 권장.

## TODO (포팅 한계)

1. **드로어 아이콘** Canvas 그림 → 실 vector asset
2. **검색 모드** trigger 만 노출 — 검색 UI 는 별도 컴포저블/라우트로
3. **다중 선택 액션바** — `onSongLongPress` 진입점만 있음. 액션바 UI 추가 필요
4. **시트 ACTIONS 아이콘** placeholder 점 — 프로젝트 아이콘으로 교체
5. **그룹 헤더** (LibraryGroup != None 일 때 LazyColumn `stickyHeader`) — 필요 시 추가
