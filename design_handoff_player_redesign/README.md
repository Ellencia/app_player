# 인수인계: 구간반복 플레이어 · A안 (Polished Material 3) 리디자인

## 1. 개요

기존 안드로이드 앱 **app_player (구간반복 플레이어 / LoopPlayer)** 의 **메인 플레이어 화면**을 시각·UX 측면에서 다듬은 리디자인입니다.
기능 추가/변경은 없습니다. 모든 기존 동작 (`PlayerViewModel.kt` + `MainActivity.kt`) 은 그대로이고, 컴포저블 트리만 교체됩니다.

대상 화면: `com.coworkapp.loopplayer.ui.PlayerScreen` 하나입니다.

### 스크린샷 (5개 상태)

`reference/screenshots/` 안에 5장의 픽셀 정확한 캡처가 있습니다:

| 파일 | 상태 |
|---|---|
| `01-idle.png` | 마커도 활성 루프도 없음 (깨끗한 시작 상태) |
| `02-ab-captured.png` | A·B 마커 둘 다 잡혀서 "저장" 버튼이 활성화된 상태 |
| `03-active-loop.png` | "후렴 A" 구간 반복 재생 중 (2/5 회) |
| `04-playing-free.png` | 일반 재생 중, 마커 없음 |
| `05-speed-and-sections.png` | 하단 — 속도 카드 + 저장된 구간 리스트 |

본 README 의 §6 사양과 함께 보면 픽셀 단위로 무엇이 들어가야 하는지 확실해집니다.

---

## 2. 디자인 파일 안내

이 패키지의 시각 자료는 **HTML 로 만든 디자인 레퍼런스** 입니다 — 의도된 룩앤필을 보여주는 프로토타입이지, 그대로 가져다 쓸 프로덕션 코드가 아닙니다.

**해야 할 일은 이 디자인을 기존 안드로이드 Kotlin + Jetpack Compose 환경에 다시 만들어 넣는 것** 입니다. 이미 프로젝트에 정착된 패턴 (`com.coworkapp.loopplayer.*` 패키지, Material 3, ExoPlayer 1.4.1, DataStore) 을 그대로 사용하세요. 새 라이브러리는 추가하지 마세요.

| 폴더 | 용도 |
|---|---|
| `reference/variant-a-preview.html` | A안 메인 화면 단일 파일 프리뷰. 브라우저로 열어서 비주얼/인터랙션 확인용. **이대로 옮기는 게 아님**. |
| `kotlin_starter/` | 같은 디자인을 Compose 로 옮긴 **참조 구현**. 그대로 안 들어가는 부분은 본 README 의 §6 사양으로 보정. |

---

## 3. 충실도

**Hi-fidelity (hifi).** 색상 hex, dp 단위 치수, 폰트 패밀리, 라운드 반경, 그림자, 모든 카피까지 픽셀 정확도로 명세돼 있습니다. **그대로 따라 구현**하세요.

---

## 4. 화면 — 메인 플레이어 (`PlayerScreen`)

### 4.1 위→아래 레이아웃 순서

```
┌────────────────────────────────────────────────┐
│ ① TopAppBar    (트랙 제목 + 폴더 아이콘)        │  56dp
├────────────────────────────────────────────────┤
│ ② Waveform Card                                │
│    ├ 현재시간 (큰 모노)  ───  총길이 (작은 모노)  │
│    ├ 미러링 막대 파형 (88dp)                    │
│    └ A칩 · B칩 · 반복중칩                       │
├────────────────────────────────────────────────┤
│ ③ Transport Row                                │  52~64dp
│    [-5초]  [▶ Play 64dp]  [+5초]                │
├────────────────────────────────────────────────┤
│ ④ Big A/B/R Row                                │  76dp
│    [A 시작점]  [B 끝점]  [R 처음으로 / 저장]     │
├────────────────────────────────────────────────┤
│ ⑤ Speed Card                                   │
│    "속도"          0.85x (모노, primary)        │
│    ━━━━━━━━━○━━━━━ 슬라이더                     │
│    [0.75x] [1.00x] [1.25x] [1.50x] 프리셋       │
├────────────────────────────────────────────────┤
│ ⑥ Section List                                 │
│    "저장된 구간 · 4"          [■ 반복 중지]      │
│    ┌─ 인트로     0:06 → 0:22 · 1.00x · ∞   ⚙ × │
│    ├─ 후렴 A     0:58 → 1:22 · 0.85x · ×5  ⚙ × │  ← 활성
│    └─ ...                                       │
└────────────────────────────────────────────────┘
```

수직 간격: `Arrangement.spacedBy(12.dp)`. 좌우 패딩: `horizontal = 12.dp`. 최하단 `Spacer(40.dp)`.

---

## 5. 디자인 토큰

### 5.1 색상 (라이트 테마, 다크는 §5.4)

| 토큰 | Hex | Material3 슬롯 매핑 |
|---|---|---|
| Background | `#FEF7FF` | `background` |
| Surface (카드) | `#FFFFFF` | `surface` |
| SurfaceContainer | `#F3EDF7` | `surfaceVariant` |
| SurfaceVariant (배지 트랙) | `#E7E0EC` | `outlineVariant` |
| Outline | `#CAC4D0` | `outline` |
| **Primary** | `#6750A4` | `primary` |
| OnPrimary | `#FFFFFF` | `onPrimary` |
| PrimaryContainer (활성 구간 카드) | `#EADDFF` | `primaryContainer` |
| OnPrimaryContainer | `#21005D` | `onPrimaryContainer` |
| Secondary (R 버튼) | `#625B71` | `secondary` |
| **Tertiary** (A/B 칩 · 마커 · 저장 버튼) | `#7D5260` | `tertiary` |
| TertiaryContainer (마커칩 배경) | `#FFD8E4` | `tertiaryContainer` |
| Ink (본문) | `#1D1B20` | `onSurface` |
| InkSoft (보조 텍스트) | `#49454F` | `onSurfaceVariant` |
| InkFaint (캡션) | `#79747E` | — |

### 5.2 파형(Waveform) 컬러 시맨틱

| 상태 | 색 | 용도 |
|---|---|---|
| 안 지나간 부분 | `outlineVariant` = `#CAC4D0` | 기본 막대 |
| 지나간 부분 | `onSurfaceVariant` = `#49454F` | 재생 헤드 왼쪽 |
| 활성 반복 구간 | `primary` = `#6750A4` | 진행여부 무관, 항상 강조 |
| A-B 임시 구간 | `tertiary` = `#7D5260` | 아직 저장 안 한 구간 후보 |
| 현재 재생 위치 (playhead) | `primary` 세로선 2dp + 흰 테두리 원 점 | 라인 + 동그라미 |
| A / B 마커 라벨칩 | `tertiary` 둥근 사각 + 흰 글자 | 18×14dp · 라운드 4dp |

**우선순위(겹칠 때):** 활성 반복 > A-B 임시 > 진행도/안지남.

### 5.3 타이포

- **본문 패밀리**: Roboto + Noto Sans KR fallback (시스템 기본)
- **모노 패밀리**: `FontFamily.Monospace` (시안과 더 가까우려면 GoogleFont JetBrains Mono — 의무 아님)
- **모노로 표시할 것들**: 시간(0:00.00 형식), 속도(0.85x), 구간 카드 안 시간 표기

| 용도 | size | weight | family |
|---|---|---|---|
| 큰 현재시간 | 24sp | SemiBold | Mono |
| 총 길이 | 14sp | Normal | Mono |
| 구간 카드 시작→끝 | 12sp | Normal | Mono |
| 속도 표시 | 18sp | Medium | Mono |
| 속도 프리셋 버튼 | 13sp | Medium | Mono |
| 트랙 제목 (AppBar) | 20sp | Medium | Sans |
| 섹션 헤더 ("저장된 구간 · 4") | 16sp | SemiBold | Sans |
| 구간 카드 이름 | 15sp | SemiBold | Sans |

### 5.4 라운드 / 간격

| 요소 | 값 |
|---|---|
| Waveform Card | `RoundedCornerShape(24.dp)` |
| Speed Card | `RoundedCornerShape(20.dp)` |
| 구간 카드 (활성·비활성) | `RoundedCornerShape(16.dp)` |
| Big A/B/R 버튼 | `RoundedCornerShape(18.dp)` |
| Play 사각 IconButton | `RoundedCornerShape(20.dp)` |
| ±5초 알약 OutlinedButton | `RoundedCornerShape(100.dp)` (= 풀라운드) |
| 속도 프리셋 / 마커칩 / 반복중칩 | `RoundedCornerShape(100.dp)` |
| 카드 내부 패딩 (Waveform·Speed) | `padding(16.dp)` / `padding(horizontal=16, vertical=14)` |
| 화면 좌우 패딩 | `horizontal = 12.dp` |
| 세로 간격 사이 | 12.dp |
| Waveform 막대 사이 갭 | `1.dp.toPx()` |

### 5.5 카드 elevation

`ElevatedCard` 1dp. 그림자 진하지 않게.

### 5.6 다크 테마

다크 컬러스키마는 `kotlin_starter/ui/theme/Theme.kt` 의 `DarkColors` 그대로 사용. 새로 디자인된 값 아님 — M3 표준 다크 톤.

---

## 6. 컴포넌트 사양 (정밀)

### 6.1 TopAppBar

- 컨테이너 색: `background` (= `#FEF7FF`)
- 좌측 leading 없음
- 타이틀: `state.trackTitle` (비어있을 때는 "구간반복 플레이어"). 1줄 + ellipsize.
- actions 오른쪽: `Icons.Default.FolderOpen` IconButton, `onClick = onOpenFile`. contentDescription "파일 열기".

### 6.2 Waveform Card

`ElevatedCard(shape = RoundedCornerShape(24.dp), containerColor = surface, elevation = 1.dp)`

내부 컬럼 (`padding(16.dp)`):
1. **상단 Row** (`SpaceBetween`, `Alignment.Bottom`)
   - 왼쪽: 현재시간 24sp mono SemiBold, 컬러 `onSurface`
   - 오른쪽: 총 길이 14sp mono, 컬러 `onSurfaceVariant`
2. **`Spacer(6.dp)`**
3. **`WaveformProgressBar`** (다음 항목 §6.3) — 높이 88dp, `padding(vertical=4.dp)`
4. **`Spacer(10.dp)`**
5. **칩 Row** (`spacedBy(6.dp)`)
   - A 칩 (마커칩): `tertiaryContainer` 배경 + "A" Bold + 모노 시간(`mm:ss`, 없으면 "─")
   - B 칩 (마커칩): 동일하게
   - 활성 구간이 있으면 `AssistChip` 추가: leadingIcon = `Icons.Default.Loop`, label = `"반복중 · ${active.label} · ${idx}/${count}"` (count 가 0 이면 `${idx}회`)

#### 마커칩(`MarkerChip`) 사양
- `Surface(shape = RoundedCornerShape(100.dp), color = tertiaryContainer)`
- 내부 Row, `padding(horizontal=12, vertical=4)`
- "A" Bold + `Spacer(6.dp)` + 모노 12sp 시간

### 6.3 WaveformProgressBar (커스텀 Canvas)

**가장 중요한 컴포넌트.** 신규 파일 `WaveformProgressBar.kt` 로 분리. 시그니처:

```kotlin
@Composable
fun WaveformProgressBar(
    positionMs: Long,
    durationMs: Long,
    tempStartMs: Long?,    // A 마커 (없으면 null)
    tempEndMs: Long?,      // B 마커
    activeStartMs: Long?,  // 활성 반복 구간 시작
    activeEndMs: Long?,    // 활성 반복 구간 끝
    waveform: List<Float>, // 0..1 진폭, 보통 200개
    waveformLoading: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
)
```

#### 6.3.1 미러링 막대 (waveform 비어있지 않을 때)

- 캔버스 가운데 라인을 기준으로 위·아래 대칭으로 막대를 그림.
- `barCount = waveform.size`
- `barTotal = size.width / barCount`
- `barGap = 1.dp.toPx()`
- `barWidth = (barTotal - barGap).coerceAtLeast(1f)`
- `maxHalf = size.height * 0.42f`
- `minHalf = 1.dp.toPx()` (너무 작으면 0 픽셀 되지 않게)
- 각 막대 i:
  - `barX = i * barTotal`
  - `t = ((i + 0.5f) / barCount * durationMs).toLong()` — 막대 중심의 시간
  - `amp = waveform[i].coerceIn(0f, 1f)`
  - `half = (amp * maxHalf).coerceAtLeast(minHalf)`
  - `drawRoundRect(color, topLeft=(barX+0.4f, centerY-half), size=(barWidth, half*2), cornerRadius=CornerRadius(barWidth/3f))`
  - color 결정 — §5.2 우선순위 적용:
    1. activeStartMs..activeEndMs 안 → `primary`
    2. tempLo..tempHi 안 → `tertiary`
    3. `t <= positionMs` → `onSurfaceVariant`
    4. 그 외 → `outlineVariant`

#### 6.3.2 Fallback (waveform 비어있을 때)

높이 8dp 트랙. unplayed 라운드 사각 + played(0~positionMs) primary 라운드 사각. 활성 구간이 있으면 14dp 두께 primary 트랙 위에 겹쳐 그림. (원본 동작과 호환.)

#### 6.3.3 A / B 마커 라벨칩

각각:
1. 세로 점선 아닌 **실선** (1.5dp) 위→아래로 그림. 색 = `tertiary`.
2. 상단에 **둥근 사각 라벨** 그림:
   - 너비 18dp, 높이 14dp, 라운드 4dp
   - `topLeft = (x - 9dp, 0)`
   - 색 = `tertiary`
3. 라벨 안에 "A" / "B" 글자:
   - `rememberTextMeasurer()` 로 측정
   - 폰트 사이즈 9sp Bold, 색 = `onTertiary` (= 흰색)
   - 라벨 박스 정중앙에 배치

#### 6.3.4 Playhead

1. 세로 실선 2dp, 색 = `primary`
2. 헤드 점: 흰 원 r=6dp + primary 원 r=5dp (테두리 효과)

#### 6.3.5 인터랙션

- **탭** (`detectTapGestures(onTap = ...)`): `x → ms` 변환 후 `onSeekTo(ms)`
- **드래그** (`detectDragGestures(onDrag = ...)`): change.position.x 로 같은 변환
- 시간 변환:
  ```kotlin
  val ratio = (x / widthPx).coerceIn(0f, 1f)
  return (ratio * durationMs).toLong()
  ```

#### 6.3.6 로딩 폴백 UI

`waveformLoading && waveform.isEmpty()` 일 때 캔버스 위에 "파형 분석 중…" 텍스트 (`labelSmall`, `onSurfaceVariant`) 중앙 정렬.

### 6.4 Transport Row

`Row(spacedBy = 8.dp)`. 좌→우:

- **−5초 버튼**: `OutlinedButton`, `Modifier.weight(1f).height(52.dp)`, `shape = RoundedCornerShape(100.dp)`. 내부: `Icon(Icons.Default.Replay5)` + `Spacer(4.dp)` + Text("5초"). content color = primary.
- **Play/Pause**: `FilledIconButton(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(20.dp))`. 아이콘 32dp. isPlaying 에 따라 `Icons.Default.Pause` / `Icons.Default.PlayArrow`. tint = onPrimary.
- **+5초 버튼**: −5초와 동일하지만 Text + `Icons.Default.Forward5` 순서.

### 6.5 Big A/B/R Row

`Row(modifier = Modifier.fillMaxWidth().height(76.dp), spacedBy = 8.dp)`. 셋 다 `weight(1f)`.

- **A**: 라벨 "A", 서브 "시작점", `color = primary`, `onClick = onMarkStart`
- **B**: 라벨 "B", 서브 "끝점", `color = primary`, `onClick = onMarkEnd`
- **세번째 (조건부 변형)**:
  | 상태 | 라벨 | 서브 | 색 | 동작 |
  |---|---|---|---|---|
  | tempA && tempB 둘 다 있음 | "저장" | "구간추가" | `tertiary` | onSaveTemp |
  | activeLoop 있음 | "R" | "처음으로" | `secondary` | onRestartActive |
  | 그 외 | "R" | "─" | `secondary` | onClearTemp |

**BigButton 자체:**

```kotlin
Surface(
    modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp)),
    color = color,
    onClick = onClick,
) {
    Column(verticalArrangement = Center, horizontalAlignment = CenterH) {
        Text(label, fontSize = 22.sp, fontWeight = Bold, color = onPrimary)
        Spacer(Modifier.height(2.dp))
        Text(sub, fontSize = 11.sp, color = onPrimary.copy(alpha = 0.9f))
    }
}
```

활성(임시 마커가 잡힌) 상태일 때 — A/B 가 눌려서 시간이 잡혀있는 상태 — 시안에서는 **링 강조** (배경색-같은-톤 두께 3dp + color 두께 5dp) 가 들어가 있습니다. Compose 로 쉽지 않으니 다음 둘 중 선택:
- (a) 그대로 두기 (구현 안 함) — 기능은 동등
- (b) `BorderStroke(2.dp, color.copy(alpha=0.5f))` 정도로 약식

### 6.6 Speed Card

`ElevatedCard(shape = RoundedCornerShape(20.dp))`, padding `horizontal=16.dp, vertical=14.dp`.

1. 상단 Row(SpaceBetween):
   - "속도" `titleSmall`, `onSurface`
   - "%.2fx".format(speed), 18sp mono Medium, **primary 색**
2. `Slider(value=speed, valueRange=0.5f..2.0f, steps=14)` — 0.1 단위
3. 프리셋 Row (`spacedBy = 6.dp`), 4개: 0.75 / 1.00 / 1.25 / 1.50
   - 현재 선택된 값이면 `FilledTonalButton`, container = `primary`, content = `onPrimary`
   - 아니면 `OutlinedButton`
   - 둘 다 `shape = RoundedCornerShape(100.dp)`, `contentPadding = PaddingValues(vertical=6.dp)`
   - 라벨: "%.2fx".format(v), 13sp mono

### 6.7 Section List Panel

`Column(spacedBy = 6.dp)`. 별도 카드 컨테이너 없음.

1. 헤더 Row(`SpaceBetween`):
   - 왼쪽: "저장된 구간 · ${size}" `titleMedium` SemiBold, `onSurface`
   - 오른쪽 (활성 구간 있을 때만): `TextButton(onClick=onStop)` → `Icons.Default.Stop` 16dp + "반복 중지" 13sp
2. sections 비어있으면: `ElevatedCard` + 안내문 "A → B 로 시작/끝을 잡고 ‘저장’을 누르면 여기에 추가됩니다." 16dp 패딩, `onSurfaceVariant` 색.
3. sections 있으면: 각각 `SectionRow` 렌더.

#### SectionRow
- `Card(shape = RoundedCornerShape(16.dp), onClick = onSelect)`
- container color:
  - 활성: `primaryContainer`
  - 비활성: `surface`
- 내부 Row(`padding(12.dp)`, `CenterVertically`):
  1. **아이콘 배지** `Surface(size=36.dp, shape=circle, color = if(active) primary else surfaceVariant)` 안에 `Icons.Default.Loop` (active) 또는 `Icons.Default.PlayArrow` (비active) 20dp.
     - tint: active → `onPrimary`, 비active → `onSurfaceVariant`
  2. `Spacer(12.dp)`
  3. **Column(weight 1f)**:
     - 라인 1: `section.label`, 15sp SemiBold, `onSurface`
     - 라인 2: `"${start} → ${end}  ·  ${speed}x  ·  ${×count|∞}"`, 12sp **mono**, `onSurfaceVariant`
     - 라인 3 (활성일 때만): `"▶ 반복중 · ${idx}/${count}"` 또는 `"${idx}회 진행"`, `labelSmall`, `primary`
  4. `IconButton(Icons.Default.Tune)` — 설정. `onSurfaceVariant`
  5. `IconButton(Icons.Default.DeleteOutline)` — 삭제. `onSurfaceVariant`

### 6.8 Empty State (trackUri == null 일 때)

`Column(padding(top=80.dp), CenterH)`:
- `Icon(Icons.Default.LibraryMusic, size = 96.dp, tint = primary)`
- `Spacer(16.dp)`
- `Text("mp3 파일을 열어주세요", titleLarge)`
- `Spacer(16.dp)`
- `Button(onClick = onOpenFile)` → `Icons.Default.FolderOpen` + " " + "파일 선택"

### 6.9 다이얼로그 두 개

원본 코드의 다이얼로그 그대로:
- **구간 저장 다이얼로그**: 이름 입력 후 `onSaveTempSection(name)`
- **AdvancedSettingsDialog**: 손대지 않음. 기존 `AdvancedSettingsDialog.kt` 그대로.

---

## 7. 인터랙션 / 동작

기존 `PlayerViewModel` API 그대로. 새로운 콜백 없음.

| UI 액션 | 콜백 |
|---|---|
| 폴더 아이콘 탭 | `onOpenFile()` |
| 파형 탭/드래그 | `onSeekTo(ms)` |
| ▶/⏸ 탭 | `onTogglePlay()` |
| −5초 / +5초 | `onSeekRelative(±5000L)` |
| A 큰버튼 | `onMarkStart()` |
| B 큰버튼 | `onMarkEnd()` |
| 저장 큰버튼 (조건부) | save 다이얼로그 열기 → 확인 시 `onSaveTempSection(name)` |
| R 큰버튼 (조건부) | `onRestartActive()` 또는 `onClearTemp()` |
| 속도 슬라이더 / 프리셋 | `onSetSpeed(v)` |
| 구간 카드 탭 | `onSelectSection(section)` |
| 구간 카드 ⚙ | 로컬 state `sectionBeingEdited` 셋 → AdvancedSettingsDialog |
| 구간 카드 × | `onDeleteSection(id)` |
| 반복 중지 | `onStopLoop()` |

상태는 **모두 `PlayerViewModel`에서 옴**. PlayerScreen 안에 두는 로컬 state 는 다음 두 개뿐:
```kotlin
var sectionBeingEdited by remember { mutableStateOf<LoopSection?>(null) }
var showSaveDialog by remember { mutableStateOf(false) }
var newSectionLabel by remember { mutableStateOf("") }
```

---

## 8. 기존 코드와의 매핑 (이게 핵심)

### 8.1 파일별 어떻게 할 것인지

| 기존 파일 | 작업 |
|---|---|
| `MainActivity.kt` | **그대로** |
| `PlayerViewModel.kt` | **그대로** |
| `data/LoopSection.kt` | **그대로** |
| `data/SectionRepository.kt` | **그대로** |
| `ui/AdvancedSettingsDialog.kt` | **그대로** |
| `ui/theme/Theme.kt` | **교체** — §5.1 의 컬러스키마로 |
| `ui/PlayerScreen.kt` | **교체** — §6.1~§6.6 사양 |
| `ui/SectionListPanel.kt` | **교체** — §6.7 사양 |
| ─ `ui/WaveformProgressBar.kt` | **신규** — §6.3 사양 |
| ─ `ui/theme/Color.kt` | **신규** (선택) — 색상 상수 분리 |
| ─ `ui/theme/Type.kt` | **신규** (선택) — 모노 TextStyle 분리 |

### 8.2 기존 PlayerScreen 의 구조와 비교

**원본 `PlayerScreen.kt` 의 LoopProgressBar(623줄 안에 있던 inner function)** 가 가장 크게 바뀝니다:
- 원본: 막대가 위→아래 한방향으로 채우는 형태 + A/B 라벨 없음 (단순 세로선)
- A안: 중앙 미러링 + A/B 라벨이 들어간 둥근 사각 칩이 세로선 위에 박힘
- 이걸 `WaveformProgressBar.kt` 로 빼내고, 라벨 칩은 Canvas 안에서 `drawRoundRect` + `rememberTextMeasurer()` 로 그립니다.

**그 외 변경 요점:**

| 위치 | 원본 → A안 |
|---|---|
| Waveform Card | `Card` → `ElevatedCard(shape=24dp)` |
| 시간 텍스트 | 일반 → **`FontFamily.Monospace`** |
| ±5초 버튼 | 56dp 직사각 → 52dp 알약(`RoundedCornerShape(100.dp)`) |
| Play 버튼 | `FilledIconButton` (기본 원형) → 같은 컴포넌트지만 `shape = RoundedCornerShape(20.dp)` 명시 |
| Big A/B/R | `Surface(shape=14dp)` → `Surface(shape=18dp)` + label 22sp/sub 11sp |
| Speed Card | `Card` → `ElevatedCard(shape=20dp)` |
| 속도 프리셋 | 4개 모두 OutlinedButton → 선택된 1개만 `FilledTonalButton(primary)` |
| 구간 카드 | `Card(shape=12dp, surfaceVariant)` → `Card(shape=16dp, surface)` |
| 활성 구간 카드 | `primaryContainer` (변경 없음) |
| 헤더 텍스트 | "저장된 구간 (4)" → "저장된 구간 · 4" |

---

## 9. 들어있는 파일

```
design_handoff_player_redesign/
├── README.md                              ← 이 문서
├── reference/
│   ├── variant-a-preview.html             ← 단일 파일 비주얼 레퍼런스 (브라우저에서 열기)
│   └── screenshots/
│       ├── 01-idle.png
│       ├── 02-ab-captured.png
│       ├── 03-active-loop.png
│       ├── 04-playing-free.png
│       └── 05-speed-and-sections.png
└── kotlin_starter/
    ├── README.md                          ← 코드 측 적용 메모
    └── ui/
        ├── theme/
        │   ├── Color.kt                   ← LoopColors 상수
        │   ├── Theme.kt                   ← LoopPlayerTheme + Light/Dark scheme
        │   └── Type.kt                    ← Typography + 모노 TextStyle
        ├── PlayerScreen.kt                ← 메인 화면
        ├── WaveformProgressBar.kt         ← 신규 컴포넌트
        └── SectionListPanel.kt            ← 구간 리스트
```

`kotlin_starter/ui/` 안의 파일들을 그대로 안드로이드 프로젝트 `app/src/main/java/com/coworkapp/loopplayer/ui/` 에 복사·덮어쓰기 한 다음, 본 README 의 §6 사양과 비교해 가며 보정하면 됩니다.

---

## 10. 흔히 막히는 곳

### 10.1 `rememberTextMeasurer` 임포트 안 됨
```kotlin
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.ExperimentalTextApi   // <- 필요
```
Composable 위에 `@OptIn(ExperimentalTextApi::class)` 붙이세요. Compose 1.5+ 부터 가능.

### 10.2 `drawText` 가 캔버스 스코프에 없음
`DrawScope.drawText(textLayoutResult, topLeft)` 시그니처를 씁니다 — `androidx.compose.ui.graphics.drawscope.DrawScope` 에 확장으로 들어있습니다. Compose Foundation 1.5+ 필요.

### 10.3 `FilledIconButton.shape` 파라미터 없음
Material3 1.2.0 미만에서는 `shape` 파라미터가 노출 안 됨. 그 경우 `shape` 빼고 기본값 사용 — 시각적으로 거의 같음. 가능하면 `androidx.compose.material3:material3:1.2.1+` 로 올리세요.

### 10.4 `LoopPlayerTheme` 가 새 컬러를 안 받음
`Theme.kt` 의 `lightColorScheme(...)` 슬롯에 §5.1 의 모든 컬러를 다 채워야 합니다. 한두 개 빠뜨리면 fallback 보라가 나옵니다.

### 10.5 시간 폰트가 일반 sans 로 나옴
`Text(..., style = ...)` 에 `fontFamily = FontFamily.Monospace` 가 들어간 `TextStyle` 을 명시해야 합니다. `Theme.kt` 의 Typography 만으로는 안 됨 — `TimeMainStyle`/`TimeListStyle` 같은 별도 스타일을 정의해서 직접 넘기세요.

### 10.6 다크 모드에서 보라가 너무 진함
`DarkColors` 의 `primary` 는 M3 표준 `#D0BCFF` 입니다. 어색하면 라이트와 똑같이 `#6750A4` 로 통일해도 됩니다 — 디자인 의도는 라이트 톤 우선.

---

## 11. 검수 체크리스트

구현 완료 후 다음을 비교 (브라우저로 `reference/variant-a-preview.html` 띄워놓고):

- [ ] Waveform 막대가 중앙선 위·아래 대칭으로 그려진다
- [ ] A 마커, B 마커 자리에 둥근 사각 라벨(흰 글자 "A"/"B")이 박혀있다
- [ ] 활성 반복 구간 안의 막대는 보라(`#6750A4`)
- [ ] A-B 임시 구간 안의 막대는 rust(`#7D5260`)
- [ ] 현재시간이 mono 24sp · 총길이가 mono 14sp
- [ ] −5초/+5초 버튼이 완전한 알약(`RoundedCornerShape(100.dp)`)
- [ ] Play 버튼이 모서리 라운드 20dp 사각
- [ ] Big A/B/R 가 76dp · 라운드 18dp
- [ ] 속도 프리셋 중 현재값만 채워진 보라 버튼, 나머지는 outline
- [ ] 활성 구간 카드 배경이 `primaryContainer` (`#EADDFF`)
- [ ] 구간 카드의 시간 표기가 mono 12sp
- [ ] 파형 탭/드래그로 seek 동작
- [ ] A/B 버튼 누르면 마커가 현재 playhead 위치에 잡힘
- [ ] tempA + tempB 둘 다 있으면 R 자리가 "저장"(tertiary 컬러)으로 바뀜
