# 구간반복 플레이어 (LoopPlayer)

mp3 등 오디오 파일을 열어서 **A/B 구간을 지정하고 반복 재생**하는 안드로이드 앱.
어학·악기 연습용으로 만든 미니멀한 플레이어임.

## 주요 기능

- mp3, m4a, wav, ogg, flac 등 오디오 파일 열기 (Storage Access Framework)
- **A / B / R 큰 버튼** - 재생 중 위치에서 시작점/끝점을 즉시 잡음
- 구간 인덱싱 - 저장한 구간을 누르면 바로 그 구간만 반복 재생
- **속도 조절** 0.5x ~ 2.0x (퀵 프리셋 + 슬라이더)
- 구간별 **상세 설정** : 반복 횟수, 구간 사이 간격, 구간별 속도
- 구간 정보는 트랙별로 영구 저장 (DataStore)

## 조작 방법 (직관 모드)

1. 상단 폴더 아이콘 → mp3 파일 선택
2. 재생 중 원하는 위치에서 **A** 버튼 → 시작점 지정
3. 끝낼 지점에서 **B** 버튼 → 끝점 지정
4. **저장** 버튼 (A,B 모두 잡히면 R이 저장 버튼으로 바뀜) → 구간 리스트에 추가
5. 리스트에서 항목 탭 → 즉시 그 구간 반복 시작
6. 상단 "반복 중지" 또는 다른 구간을 누르면 전환됨

상세 설정이 필요하면 항목 우측 **톱니(설정) 아이콘** 누르기.

## 빌드 / 실행 방법

### 필요한 환경
- Android Studio Hedgehog (2023.1.1) 이상 권장
- JDK 17 (Android Studio에 내장된 거 써도 됨)
- Android SDK 34

### 처음 열 때
1. Android Studio 실행 → `Open` → `C:\Programming\app_player\앱 플레이어` 폴더 선택
2. Gradle Sync 자동 시작됨
   - 만약 "Gradle wrapper가 없다"고 뜨면, Studio 하단 터미널에서
     `gradle wrapper --gradle-version 8.7` 한 번 실행하거나, AS가 안내하는 "Use Gradle wrapper" 옵션 선택
3. 실기기 또는 에뮬레이터 연결 후 `Run ▶`

### 폴더 경로 주의
경로에 한글/공백(`앱 플레이어`)이 들어있어 일부 환경에서 Gradle 캐시가 꼬일 수 있음.
빌드 에러가 계속 나면 경로를 영문으로 옮기는 게 안전함 (예: `C:\dev\loop_player`).

## 프로젝트 구조

```
app/
└── src/main/
    ├── AndroidManifest.xml
    ├── java/com/coworkapp/loopplayer/
    │   ├── MainActivity.kt         # 진입점, 파일 선택기
    │   ├── PlayerViewModel.kt      # ExoPlayer + 구간 반복 핵심 로직
    │   ├── data/
    │   │   ├── LoopSection.kt      # 구간 데이터 모델
    │   │   └── SectionRepository.kt # DataStore 저장소
    │   └── ui/
    │       ├── PlayerScreen.kt           # 메인 화면 (Compose)
    │       ├── SectionListPanel.kt       # 저장된 구간 리스트
    │       ├── AdvancedSettingsDialog.kt # 상세 설정 다이얼로그
    │       └── theme/Theme.kt
    └── res/
        ├── values/ (strings, colors, themes)
        ├── drawable/, mipmap-anydpi-v26/
        └── xml/ (backup_rules)
```

## 라이브러리

- Jetpack Compose (Material3)
- androidx.media3 ExoPlayer 1.4.1
- androidx.datastore (구간 영구 저장)
- kotlinx.serialization

## 앞으로 추가할 수 있는 기능 (TODO)

- 유튜브 URL 지원 (YouTube IFrame Player 또는 yt-dlp 연동)
- 볼륨 버튼/블루투스 키로 A/B/R 단축키
- 자막(.srt) 동기화
- 백그라운드 재생 (MediaSession + ForegroundService)
- 파형(Waveform) 표시 + 파형 위에서 구간 드래그
