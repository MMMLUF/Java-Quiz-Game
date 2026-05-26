# Real-Time Network Quiz Game System
> **Technical Specification and Documentation** > *Developed as an undergraduate software engineering project exploring Concurrent Programming and Network Architecture.*

The Real-Time Network Quiz Game System is a robust, single-player online desktop application built on a client-server architecture. The project demonstrates the practical integration of Java Swing GUI, multi-threaded socket communication, and embedded database management to deliver a fault-tolerant, high-performance distributed application.

---

## 1. Key Engineering Features

### 1.1 Multi-Threaded Session Management
The core server deploys an independent `ClientHandler` thread for each incoming client connection. This decouples the network listening layer from the active game logic, allowing the server to handle concurrent user sessions asynchronously without blocking system resources.

### 1.2 Dual-Threaded Synchronization for Game Loops
Each quiz question is governed by an independent `TimerThread` that executes a 15-second countdown in the background. It communicates in real-time with the main client application via byte-stream signaling. Upon user input submission, the server immediately interrupts the timer thread to finalize the score, preventing resource leakage.

### 1.3 Adaptive Categorization & Randomization
The database consists of distinct technical and general knowledge questions partitioned across 4 specialized categories. The system utilizes index-shuffling algorithms (`Collections.shuffle`) to extract 5 non-repeating, randomized questions per session to ensure dynamic user engagement.

### 1.4 Persistent Data Layer & Grid Alignment
User scores are written to an embedded SQLite database (`ranking.db`). The client interface implements a `JTable` rendering matrix instead of primitive text areas. This guarantees absolute vertical alignment and strict grid formatting for the Top 10 Leaderboard, regardless of character length or font variation.

### 1.5 Dynamic Scoring Engine with Combo Multiplier
The scoring system rewards both speed and consistency. Each correct answer yields a base score of 100 points, augmented by a time bonus (`remaining seconds × 10`). Consecutive correct answers activate a combo bonus (`+50 per streak`), incentivizing sustained accuracy throughout the session.

| Condition | Points Formula |
|-----------|---------------|
| Correct answer | `100 + (remaining seconds × 10) + ((streak − 1) × 50)` |
| Wrong answer | 0 |
| Time expired | 0, streak resets |

### 1.6 Category-Aware UI Theming
A centralized `Theme` class provides shared color constants and font hierarchy across all screens. Each quiz category is assigned a distinct accent color applied consistently to the progress bar, timer label, button hover states, and header borders.

| Category | Accent Color |
|----------|-------------|
| Java 코드 | Amber `#F59E0B` |
| SKKU 퀴즈 | Blue `#2563EB` |
| 일반상식 | Emerald `#10B981` |
| 넌센스 | Violet `#8B5CF6` |

### 1.7 Real-Time Feedback System
After each answer submission or timeout, a feedback banner renders inline within the quiz screen with one of three states: **정답** (correct), **오답** (wrong, revealing the correct option), or **시간 초과** (timeout). Correct and wrong answer buttons are simultaneously color-coded to reinforce the result visually.

---

## 2. Technical Stack

* **Programming Language**: Java 11 (or higher)
* **GUI Framework**: Java Swing (Native System Look & Feel Integration)
* **Network Protocol**: TCP/IP via Java Socket API with line-based text protocol
* **Database Management**: Embedded SQLite via JDBC Driver
* **Data Interchange Format**: JSON (via `org.json` library encapsulation)

---

## 3. System Architecture & Fault-Tolerance (Fail-Safe Strategy)

The system enforces a rigorous exception handling mechanism to ensure high availability and application stability:

* **Server Disconnection (ConnectException)**: If the backend server is offline, the client triggers a structural retry loop through a modal confirmation dialog, preventing immediate application crash.
* **Mid-Game Disconnection**: If the server connection drops during an active quiz session, the client detects the broken stream, displays an error dialog, and automatically navigates back to the login screen.
* **Data Corruption / Loss (FileNotFoundException)**: In the event that the external `quizzes.json` file is missing, the server catches the I/O exception and automatically loads a hardcoded, memory-resident "Fallback Question Set" to maintain uninterrupted gameplay.
* **Database Connectivity Failure (SQLException)**: If the SQLite connection is severed, the program gracefully locks and isolates the leaderboard registration button (`btnRegister`) while allowing the user to safely complete the current active game loop.
* **Protocol Violation (QuizException)**: Malformed or out-of-range responses from the client are intercepted by a custom exception handler on the server, isolating the fault to the individual session without affecting the server process.
* **Input Validation**: Nickname entries are validated for length (2–8 characters) and character set (Korean, alphanumeric only) before a socket connection is established, preventing protocol corruption from special characters or whitespace.

---

## 4. Execution Guide

To ensure proper network binding, the **Server application must be initiated prior to the Client application.**

### Step 1: Resource Verification
Verify that `quizzes.json` and the JDBC driver (`lib/sqlite-jdbc.jar`) are located within the project root directory.

### Step 2: Initialize Quiz Server
* **Source Code Execution**: Run `com.quiz.server.QuizServer` inside the IDE.
* **Verification**: Ensure the console displays: `[SERVER] 퀴즈 서버가 가동되었습니다. 클라이언트를 기다립니다.`

### Step 3: Launch Client Interface
* **Source Code Execution**: Run `com.quiz.client.LoginScreen`.

---

## 5. Project Directory Structure

```text
📂 QuizGame
 ┣ 📂 src
 ┃ ┣ 📂 com.quiz.common
 ┃ ┃ ┗ 📜 QuizException.java       # Custom exception for protocol validation
 ┃ ┣ 📂 com.quiz.client
 ┃ ┃ ┣ 📜 LoginScreen.java         # Login, nickname validation, and server connection
 ┃ ┃ ┣ 📜 QuizScreen.java          # Real-time quiz GUI with timer, feedback, and combo display
 ┃ ┃ ┣ 📜 ResultScreen.java        # JTable-based leaderboard dashboard with play-again support
 ┃ ┃ ┗ 📜 Theme.java               # Shared color constants, fonts, and category accent colors
 ┃ ┗ 📂 com.quiz.server
 ┃ ┃ ┣ 📜 QuizServer.java          # Server socket initialization (Port 9999)
 ┃ ┃ ┣ 📜 ClientHandler.java       # Multi-threaded session manager with scoring logic
 ┃ ┃ ┣ 📜 TimerThread.java         # 15-second background countdown thread
 ┃ ┃ ┣ 📜 ScoreManager.java        # Singleton in-memory score repository (ConcurrentHashMap)
 ┃ ┃ ┣ 📜 HallOfFameDAO.java       # SQLite Database Access Object (DAO pattern)
 ┃ ┃ ┗ 📜 QuizLoader.java          # Utility for verifying quizzes.json contents
 ┣ 📂 lib
 ┃ ┣ 📜 org.json.jar               # JSON parsing library
 ┃ ┗ 📜 sqlite-jdbc.jar            # SQLite JDBC driver
 ┣ 📜 quizzes.json                 # Category-based master question repository
 ┗ 📜 ranking.db                   # Auto-generated local SQLite database file
```

---

## 6. Client-Server Communication Protocol

All messages are transmitted as UTF-8 encoded line-delimited strings over a persistent TCP socket. The server drives the protocol; the client responds only when prompted.

| Direction | Signal | Payload (next line) | Description |
|-----------|--------|---------------------|-------------|
| S → C | `NEXT_QUESTION` | progress, question text, JSON options array | Delivers the next question |
| S → C | `TIMER_UPDATE` | remaining seconds (int) | Sent every second during countdown |
| S → C | `TIME_OUT_SIGNAL` | *(none)* | Notifies client that time has expired |
| S → C | `RESULT` | `correct {pts} {streak}` / `wrong {ans}` / `timeout 0` | Sends scoring result |
| S → C | `GAME_OVER` | final score (int) | Marks end of quiz; client transitions to ResultScreen |
| C → S | *(int)* | — | Answer submission: `1`–`4` for selection, `0` for timeout |
| C → S | `REGISTER` | *(none)* | Requests server to persist this session's score to `ranking.db` |
| S → C | `REGISTER_OK` | *(none)* | Score persisted successfully |
| S → C | `REGISTER_FAIL` | error message (1 line) | DB error; client surfaces the reason in a modal |
| C → S | `TOP10` | *(none)* | Requests Top 10 leaderboard |
| S → C | `TOP10` | row count (int), then N lines of `{username}\t{score}` | Leaderboard response |
| C → S | `QUIT` | *(none)* | Ends the post-game session; server closes the socket |
