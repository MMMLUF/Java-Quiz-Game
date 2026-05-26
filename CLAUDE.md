# CLAUDE.md

> Java-Quiz-Game 프로젝트에서 Claude Code(또는 신규 팀원)가 코드를 만지기 전에 알아야 할 운영 지식 모음입니다.
> 외부 발표용 상세 스펙은 [`README.md`](./README.md)를 참고하세요. 이 문서는 README와 중복되는 설명은 피하고, **빌드/실행 방법, 진입점, 그리고 README에 안 적힌 함정**에 집중합니다.

---

## 1. 프로젝트 개요

자바 스윙 클라이언트 + TCP 소켓 멀티스레드 서버 + SQLite 영속 계층으로 구성된 **단일 플레이어 실시간 퀴즈 게임**입니다. 사용자는 4개 카테고리 중 하나를 선택해 5문제를 풀고, 점수는 SQLite(`ranking.db`)에 저장되어 Top 10 랭킹으로 표시됩니다. 자세한 기능/아키텍처 설명은 README 참조.

---

## 2. 빌드 & 실행

프로젝트 루트에서 [`scripts/run.ps1`](./scripts/run.ps1)을 참고해 실행한다. 옵션·진입점·클래스패스 등 디테일은 모두 그 파일에 있다.

---

## 3. 진입점 & 포트

| 종류 | 클래스 | 비고 |
|------|--------|------|
| 서버 | `com.quiz.server.QuizServer` | `ServerSocket` on TCP port **9999** (`QuizServer.PORT`) |
| 클라이언트 | `com.quiz.client.LoginScreen` | `localhost:9999`로 접속 (하드코딩, `LoginScreen.java`) |
| 디버그 유틸 | `com.quiz.server.QuizLoader` | `quizzes.json` 내용 콘솔 덤프 |

원격 호스트 접속이 필요하면 `LoginScreen.java`의 `new Socket("localhost", 9999)` 부분을 직접 수정해야 합니다.

---

## 4. 패키지 구조

```
com.quiz.common
 └─ QuizException          # 프로토콜 위반용 커스텀 예외 (서버 측에서 던짐)

com.quiz.server
 ├─ QuizServer             # ServerSocket accept 루프
 ├─ ClientHandler          # 세션당 1 스레드, 퀴즈 로딩·채점·결과 송신
 ├─ TimerThread            # 문제당 1 스레드, 15초 카운트다운
 ├─ ScoreManager           # ConcurrentHashMap 기반 싱글톤, 세션 점수 누적
 ├─ HallOfFameDAO          # SQLite DAO (ranking.db, RANKING 테이블)
 └─ QuizLoader             # quizzes.json 파싱 유틸 / 디버그용 main

com.quiz.client
 ├─ LoginScreen            # 닉네임·카테고리 입력, 서버 연결 시작
 ├─ QuizScreen             # 메인 게임 UI + ReceiveTask(서버 신호 수신 스레드)
 ├─ ResultScreen           # 최종 점수, JTable 랭킹, 재시작/종료
 └─ Theme                  # 색상·폰트 상수 (맑은 고딕), 카테고리 액센트 컬러
```

---

## 5. 통신 프로토콜

서버 주도, 라인 기반 UTF-8 텍스트 프로토콜. 전체 신호 표는 [`README.md` §6](./README.md#6-client-server-communication-protocol) 참조. 자주 보게 될 신호:

- **인게임 (S → C)**: `NEXT_QUESTION`, `TIMER_UPDATE`, `TIME_OUT_SIGNAL`, `RESULT`, `GAME_OVER`
- **인게임 (C → S)**: 정수 한 줄 — `1`~`4`(선택), `0`(타임아웃)
- **게임 종료 후 (C → S)**: `REGISTER`, `TOP10`, `QUIT` — ResultScreen에서 사용
- **게임 종료 후 (S → C)**: `REGISTER_OK` / `REGISTER_FAIL` + 사유 / `TOP10` + 행 수 + `{username}\t{score}` × N

`RESULT` 라인 페이로드 포맷:
- `correct {pts} {streak}`
- `wrong {정답번호}`
- `timeout 0`

`GAME_OVER` 이후 서버는 즉시 소켓을 닫지 않고 `ClientHandler`의 post-game 루프에서 `REGISTER`/`TOP10`/`QUIT` 명령을 받는다. 클라가 `QUIT`을 보내거나 스트림이 닫힐 때까지 세션이 유지된다.

---

## 6. 스레딩 모델

- **서버**
  - `QuizServer` accept 루프 → 클라이언트당 `ClientHandler` 스레드 1개 생성
  - `ClientHandler`는 문제마다 `TimerThread` 1개를 spawn. 클라이언트 응답이 도착하면 `timer.interrupt()`로 즉시 종료
  - `ScoreManager`는 `ConcurrentHashMap` 기반 싱글톤이므로 멀티 세션 동시 접근 안전
  - `HallOfFameDAO`는 호출마다 새 Connection을 여는 단순 구조 (현재 동시성 문제 없음)
- **클라이언트**
  - `QuizScreen`이 `ReceiveTask`(내부 클래스) 백그라운드 스레드로 서버 신호를 수신
  - **UI 갱신은 반드시 `SwingUtilities.invokeLater`**로 EDT에 마샬링해야 함. ReceiveTask 안에서 직접 컴포넌트를 건드리지 말 것
  - `volatile` 필드들(`TimerThread.currentSeconds` 등)을 수정할 때는 thread-safety 영향 검토

---

## 7. 데이터 파일

| 파일 | 위치 | 역할 |
|------|------|------|
| `quizzes.json` | 프로젝트 루트 (CWD 기준) | 카테고리별 문제 마스터. 최상위 키 4개: `"Java 코드"`, `"SKKU 퀴즈"`, `"일반상식"`, `"넌센스"` |
| `ranking.db` | 프로젝트 루트 (CWD 기준, 자동 생성) | SQLite, `RANKING(id, username, score)` 테이블 |
| `lib/org.json.jar` | `lib/` | JSON 파싱 (외부 의존성) |
| `lib/sqlite-jdbc.jar` | `lib/` | SQLite JDBC 드라이버 (외부 의존성) |

`quizzes.json`의 문제 객체 스키마:
```json
{ "question": "...", "options": ["1. ...", "2. ...", "3. ...", "4. ..."], "answer": 2 }
```
**`answer`는 1-indexed** (1~4), 클라이언트가 보내는 선택 번호와 동일 체계.

---

## 8. 채점 공식

| 조건 | 점수 |
|------|------|
| 정답 | `100 + (남은 초 × 10) + ((streak − 1) × 50)` |
| 오답 | 0, streak 리셋 |
| 시간 초과 | 0, streak 리셋 |

상수는 `ClientHandler` 상단(`BASE_SCORE`, `TIME_BONUS_PER_SEC`, `COMBO_BONUS`, `TIMER_SECONDS`, `TOTAL_QUESTIONS`)에 모여 있습니다. 균형 조정 시 여기를 만지세요.

---

## 9. 함정 / 주의사항 (이 섹션이 핵심)

1. **카테고리 문자열 강결합**
   `quizzes.json` 최상위 키 / `LoginScreen.categories` 배열 / `Theme.categoryColor()` switch — **세 곳의 문자열이 글자 단위로 정확히 일치**해야 합니다. 카테고리 추가·이름 변경 시 세 곳 모두 수정 필요. 어긋나면 색 테마가 기본값으로 빠지거나, 서버가 `"일반상식"` 폴백으로 분기합니다 (`ClientHandler.java:70-72`).

2. **타이머 상수 클라/서버 분리**
   서버 `ClientHandler.TIMER_SECONDS = 15`와 클라 `QuizScreen`의 타이머 최대값(`TIMER_MAX_SECONDS`)이 **별도로 정의**되어 있습니다. 한쪽만 바꾸면 진행바·시간 보너스가 어긋납니다. 항상 두 곳을 같이 수정하세요.

3. **`ranking.db`는 서버 측 CWD 기준 상대경로**
   `HallOfFameDAO`만 `jdbc:sqlite:ranking.db`를 사용하고 클라이언트는 더 이상 DB에 직접 접근하지 않습니다 (모든 등록·조회는 서버 경유). 따라서 서버를 어느 디렉터리에서 띄우느냐에 따라 DB 파일 위치가 달라지며, 클라이언트는 자기 CWD나 `sqlite-jdbc.jar` 유무와 무관하게 동작합니다.

4. **`quizzes.json` 누락 시 폴백**
   `ClientHandler`는 파일 로드에 실패하면 1문제짜리 하드코딩 백업(`[백업] 자바의 최상위 부모 클래스는?`)으로 전환합니다. 테스트 중 갑자기 한 문제만 출제된다면 파일 누락/경로 문제를 먼저 의심하세요.

5. **닉네임 검증은 클라이언트 단에서만**
   `LoginScreen`이 2~8자, 한/영/숫자 정규식(`[가-힣a-zA-Z0-9]+`)으로 검증합니다. 서버(`ClientHandler`)는 닉네임을 그대로 신뢰합니다. 별도 클라이언트로 직접 9999에 붙으면 임의 문자열로 점수를 등록할 수 있습니다 — 평가 시연 외 노출 금지.

6. **`bin/`은 빌드 산출물**
   현재 `git status`에 untracked로 잡혀 있습니다. `.gitignore`에 `bin/`을 추가해 커밋되지 않도록 권장.

7. **UTF-8 강제**
   서버·클라 모두 `StandardCharsets.UTF_8`로 스트림을 감쌉니다. 새로 I/O 코드를 추가할 때 인코딩을 생략하면 한글이 깨질 수 있으니 동일 패턴을 따르세요.

---

## 10. 코딩 컨벤션

- 표준 자바 CamelCase. 한국어 Javadoc/주석 사용.
- 폰트는 `Theme`에서 **"맑은 고딕"** (Malgun Gothic)을 가정합니다. 비한글 OS에서 돌릴 때는 폰트 폴백 처리 필요.
- UI 문자열은 한국어 그대로 둡니다 (i18n 미지원).
- 새 신호를 추가하면 README §6 프로토콜 표도 같이 갱신.

---

## 11. 문서 우선순위

- **상세 스펙·기능 설명**: [`README.md`](./README.md)
- **운영 지식·함정**: 본 파일 (`CLAUDE.md`)
- 사용자 향 설명/기능 변경 시 README를, 내부 동작·실행 방법·주의사항 변경 시 CLAUDE.md를 갱신하세요.
