# Variant A → Compose 포팅

웹 시안의 **A안 (Polished Material 3 Purple)** 을 기존 안드로이드 코드베이스에 그대로 떨어뜨릴 수 있게 옮긴 파일들입니다.
패키지/시그니처는 원본 그대로 (`com.coworkapp.loopplayer.ui.*`) 유지했습니다.

## 들어있는 파일

```
kotlin_port/
└── ui/
    ├── theme/
    │   ├── Color.kt              # LoopColors — 시안의 hex 그대로
    │   ├── Theme.kt              # 라이트/다크 ColorScheme + LoopPlayerTheme
    │   └── Type.kt               # Typography + 시간/속도용 모노 TextStyle
    ├── PlayerScreen.kt           # 메인 화면 (Scaffold + 5개 섹션)
    ├── WaveformProgressBar.kt    # 미러링 막대 파형 + A/B 라벨 칩
    └── SectionListPanel.kt       # 저장된 구간 리스트
```

## 적용 방법

1. `kotlin_port/ui/` 안의 파일들을 **기존 `app/src/main/java/com/coworkapp/loopplayer/ui/` 폴더에 덮어쓰기** 하세요.
   - `theme/Theme.kt`, `theme/Color.kt`, `theme/Type.kt`
   - `PlayerScreen.kt`, `SectionListPanel.kt`
   - 새 파일: `WaveformProgressBar.kt`
2. `AdvancedSettingsDialog.kt` 는 손대지 않았습니다 — 그대로 두면 됩니다.
3. `PlayerScreen` 의 시그니처는 원본과 동일하므로 `MainActivity.kt`/`PlayerViewModel.kt` 는 수정 불필요합니다.

## A안에서 바뀐 부분 (원본 대비)

| 영역 | 원본 | A안 (포팅 결과) |
|---|---|---|
| 파형 | 막대가 위→아래 한 방향 채움 | **중앙 기준 미러링** (위·아래 대칭) |
| A/B 마커 | 얇은 세로선 1개 | 세로선 + **상단에 둥근 라벨 칩 (A/B)** |
| 시간 표시 | 일반 폰트 | **monospace + tabular-numerals** |
| 카드 모양 | 8dp 라운드 | **24dp / 20dp / 16dp** 톤다운 라운드 |
| Play 버튼 | 둥근 IconButton | **20dp 라운드 사각** (M3 표현) |
| ±5초 버튼 | 56dp 직사각 | **52dp 알약형 OutlinedButton** |
| 속도 프리셋 | 4개 동일 outlined | **선택된 것만 채움**, 나머지는 outline |
| 구간 카드 | 12dp 라운드 | **16dp + 활성 시 primaryContainer** |
| 폰트 시스템 | 기본 | `Typography` + `TimeMainStyle` 등 명시 |

## 의존성

새로 추가되는 라이브러리는 **없습니다**. `androidx.compose.material3`, `compose.ui` 만 쓰입니다.
모노 폰트는 `FontFamily.Monospace` 시스템 기본을 씁니다 — 필요 시 `androidx.compose.ui.text.googlefonts` 로 JetBrains Mono 같은 걸 끼우면 시안과 더 가까워집니다.

## 동작 호환성

- `WaveformProgressBar` 는 `tap` / `drag` 둘 다 받습니다 (`onSeekTo` 콜백).
- `waveform` 이 비어있고 `waveformLoading=true` 일 때 "파형 분석 중…" 폴백을 똑같이 표시합니다.
- 활성 반복 구간이 있으면 `WaveformCard` 안 칩으로 `반복중 · 라벨 · n/N` 을 표시 — 원본 로직 그대로.

## 빌드 전 체크

- `compileSdk` / `targetSdk` 34
- Compose BOM 2024.05.00 이상 권장 (Material3 1.2.x)
- 기존 `build.gradle.kts` 그대로 빌드 됩니다 — 추가 의존성 X
