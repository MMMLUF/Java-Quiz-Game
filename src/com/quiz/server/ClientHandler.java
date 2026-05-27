package com.quiz.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import com.quiz.common.QuizException;

/**
 * 클라이언트 1명의 전체 게임 세션을 담당하는 핸들러.
 * 퀴즈 로딩, 타이머 제어, 점수 계산, 결과 전송을 순서대로 처리한다.
 */
public class ClientHandler extends Thread {

    /** 한 게임에서 출제할 문제 수 */
    private static final int TOTAL_QUESTIONS    = 5;
    /** 문제당 제한 시간(초) */
    private static final int TIMER_SECONDS      = 15;
    /** 정답 시 기본 점수 */
    private static final int BASE_SCORE         = 100;
    /** 남은 초당 추가 점수 */
    private static final int TIME_BONUS_PER_SEC = 10;
    /** 연속 정답 1회당 추가 콤보 보너스 */
    private static final int COMBO_BONUS        = 50;
    /** 결과 표시 후 다음 문제까지 대기 시간(ms) */
    private static final int RESULT_DELAY_MS    = 1500;

    private Socket socket;
    private ScoreManager scoreManager;

    /**
     * @param socket 클라이언트와 연결된 소켓
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.scoreManager = ScoreManager.getInstance();
    }

    /**
     * 게임 세션 메인 루프.
     * 닉네임·카테고리 수신 → 문제 출제 → 채점 → GAME_OVER 전송 순으로 진행된다.
     */
    @Override
    public void run() {
        String nickname = "Unknown";
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            nickname = in.readLine();
            if (nickname == null) return;
            String category = in.readLine();
            if (category == null) return;
            System.out.println("[SERVER] 플레이어 접속: " + nickname + " (카테고리: " + category + ")");

            JSONArray quizArray;
            try {
                String content = new String(Files.readAllBytes(Paths.get("quizzes.json")), StandardCharsets.UTF_8);
                JSONObject allQuizzes = new JSONObject(content);
                quizArray = allQuizzes.has(category)
                        ? allQuizzes.getJSONArray(category)
                        : allQuizzes.getJSONArray("일반상식");
            } catch (IOException e) {
                System.out.println("[SERVER] quizzes.json 로드 실패. 기본 백업 문제를 로딩합니다.");
                String fallbackJson = "[{\"question\":\"[백업] 자바의 최상위 부모 클래스는?\","
                        + "\"options\":[\"1.String\",\"2.Object\",\"3.System\",\"4.Void\"],\"answer\":2}]";
                quizArray = new JSONArray(fallbackJson);
            }

            List<Integer> indices = new ArrayList<>();
            for (int k = 0; k < quizArray.length(); k++) {
                indices.add(k);
            }
            Collections.shuffle(indices);

            int totalQuestions = Math.min(TOTAL_QUESTIONS, quizArray.length());
            int streak = 0;

            for (int i = 0; i < totalQuestions; i++) {
                int randomIndex = indices.get(i);
                JSONObject quiz = quizArray.getJSONObject(randomIndex);

                String question     = quiz.getString("question");
                JSONArray options   = quiz.getJSONArray("options");
                int correctAnswer   = quiz.getInt("answer");

                out.println("NEXT_QUESTION");
                out.println((i + 1) + " / " + totalQuestions);
                out.println(question);
                out.println(options.toString());

                TimerThread timer = new TimerThread(out, TIMER_SECONDS, this);
                timer.start();

                String clientAnswerStr = in.readLine();
                timer.interrupt();

                if (clientAnswerStr == null) break;

                int clientAnswer;
                try {
                    clientAnswer = Integer.parseInt(clientAnswerStr.trim());
                } catch (NumberFormatException e) {
                    throw new QuizException("숫자가 아닌 응답: " + clientAnswerStr);
                }

                if (clientAnswer < 0 || clientAnswer > 4) {
                    throw new QuizException("잘못된 프로토콜 입력 범위: " + clientAnswer);
                }

                if (clientAnswer == 0) {
                    streak = 0;
                    out.println("RESULT");
                    out.println("timeout 0");
                } else if (clientAnswer == correctAnswer) {
                    streak++;
                    int comboBonus = (streak - 1) * COMBO_BONUS;
                    int points = BASE_SCORE + timer.getRemainingSeconds() * TIME_BONUS_PER_SEC + comboBonus;
                    scoreManager.addScore(nickname, points);
                    out.println("RESULT");
                    out.println("correct " + points + " " + streak);
                } else {
                    streak = 0;
                    out.println("RESULT");
                    out.println("wrong " + correctAnswer);
                }

                try {
                    Thread.sleep(RESULT_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            int finalScore = scoreManager.getScore(nickname);
            out.println("GAME_OVER");
            out.println(finalScore);

            // 게임 종료 후 클라이언트의 랭킹 등록·조회 명령을 처리한다.
            // 테이블 초기화는 QuizServer 부팅 시 1회 수행됨.
            HallOfFameDAO dao = new HallOfFameDAO();

            String cmd;
            while ((cmd = in.readLine()) != null) {
                cmd = cmd.trim();
                if ("REGISTER".equals(cmd)) {
                    try {
                        // 점수 위조 방지: 클라이언트가 보낸 값이 아닌 서버 메모리의 finalScore만 저장
                        dao.insertScore(nickname, finalScore);
                        out.println("REGISTER_OK");
                    } catch (Exception ex) {
                        out.println("REGISTER_FAIL");
                        out.println(ex.getMessage() == null ? "DB 오류" : ex.getMessage());
                    }
                } else if ("TOP10".equals(cmd)) {
                    List<String[]> rows = dao.getTop10();
                    out.println("TOP10");
                    out.println(rows.size());
                    for (String[] r : rows) {
                        out.println(r[0] + "\t" + r[1]);
                    }
                } else if ("QUIT".equals(cmd)) {
                    break;
                }
                // 알 수 없는 명령은 침묵으로 무시
            }

        } catch (QuizException qe) {
            System.out.println("[SERVER] 프로토콜 위반: " + qe.getMessage());
        } catch (IOException e) {
            System.out.println("[SERVER] 오류 발생: " + e.getMessage());
        } finally {
            if (nickname != null) {
                scoreManager.clearPlayer(nickname);
            }
            try {
                socket.close();
            } catch (Exception e) {
                // 소켓 닫기 실패는 무시
            }
        }
    }
}
