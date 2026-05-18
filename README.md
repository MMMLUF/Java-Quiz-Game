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
The database consists of 80 distinct technical and general knowledge questions partitioned across 4 specialized categories. The system utilizes index-shuffling algorithms (`Collections.shuffle`) to extract 5 non-repeating, randomized questions per session to ensure dynamic user engagement.

### 1.4 Persistent Data Layer & Grid Alignment
User scores are written to an embedded SQLite database (`ranking.db`). The client interface implements a `JTable` rendering matrix instead of primitive text areas. This guarantees absolute vertical alignment and strict grid formatting for the Top 10 Leaderboard, regardless of character length or font variation.

---

## 2. Technical Stack

* **Programming Language**: Java 11 (or higher)
* **GUI Framework**: Java Swing (Native System Look & Feel Integration)
* **Network Protocol**: TCP/IP via Java Socket API
* **Database Management**: Embedded SQLite via JDBC Driver
* **Data Interchange Format**: JSON (via `org.json` library encapsulation)

---

## 3. System Architecture & Fault-Tolerance (Fail-Safe Strategy)

The system enforces a rigorous exception handling mechanism to ensure high availability and application stability:

* **Server Disconnection (ConnectException)**: If the backend server is offline, the client triggers a structural retry loop through a modal confirmation dialog, preventing immediate application crash.
* **Data Corruption / Loss (FileNotFoundException)**: In the event that the external `quizzes.json` file is missing, the server catches the I/O exception and automatically loads a hardcoded, memory-resident "Fallback Question Set" to maintain uninterrupted gameplay.
* **Database Connectivity Failure (SQLException)**: If the SQLite connection is severed, the program gracefully locks and isolates the leaderboard registration button (`btnRegister`) while allowing the user to safely complete the current active game loop.

---

## 4. Execution Guide

To ensure proper network binding, the **Server application must be initiated prior to the Client application.**

### Step 1: Resource Verification
Verify that `quizzes.json` and the JDBC driver are located within the root directory of the compiled executables.

### Step 2: Initialize Quiz Server
* **Source Code Execution**: Run `com.quiz.server.QuizServer.java` inside the IDE.
* **Binary Execution**: Run `QuizServer.jar` or execute `서버_가동.bat`.
* **Verification**: Ensure the console displays: `[SERVER] Quiz Server is running on port 9999. Awaiting client connection...`

### Step 3: Launch Client Interface
* **Source Code Execution**: Run `com.quiz.client.LoginScreen.java`.
* **Binary Execution**: Run `QuizGame.jar` or execute `게임_시작.bat`.

---

## 5. Project Directory Structure

```text
📂 QuizGame
 ┣ 📂 src
 ┃ ┣ 📂 com.quiz.common
 ┃ ┃ ┗ 📜 QuizException.java       # Custom Exception for protocol validation
 ┃ ┣ 📂 com.quiz.client
 ┃ ┃ ┣ 📜 LoginScreen.java         # Login and Nickname validation GUI layer
 ┃ ┃ ┣ 📜 QuizScreen.java          # Real-time background network streaming GUI
 ┃ ┃ ┗ 📜 ResultScreen.java        # JTable-based Leaderboard Dashboard GUI
 ┃ ┗ 📂 com.quiz.server
 ┃ ┃ ┣ 📜 QuizServer.java          # Server Socket initialization (Port 9999)
 ┃ ┃ ┣ 📜 ClientHandler.java       # Multi-threaded session manager
 ┃ ┃ ┣ 📜 TimerThread.java         # 15-second background countdown thread
 ┃ ┃ ┣ 📜 ScoreManager.java        # Singleton pattern global score repository
 ┃ ┃ ┗ 📜 HallOfFameDAO.java       # SQLite Database Access Object
 ┣ 📜 quizzes.json                 # Category-based Master Question Repository (80 Items)
 ┗ 📜 ranking.db                   # Auto-generating local SQLite Database file
