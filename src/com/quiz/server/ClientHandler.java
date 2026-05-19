package com.quiz.server;

import java.io.BufferedReader;
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

public class ClientHandler extends Thread {
    private Socket socket;
    private ScoreManager scoreManager;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.scoreManager = ScoreManager.getInstance();
    }

    @Override
    public void run() {
        String nickname = "Unknown";
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            nickname = in.readLine();
            String category = in.readLine();
            System.out.println("[SERVER] 플레이어 접속: " + nickname + " (카테고리: " + category + ")");

            JSONArray quizArray;
            try {
                String content = new String(Files.readAllBytes(Paths.get("quizzes.json")), StandardCharsets.UTF_8);
                JSONObject allQuizzes = new JSONObject(content);
                
                if (allQuizzes.has(category)) {
                    quizArray = allQuizzes.getJSONArray(category);
                } else {
                    quizArray = allQuizzes.getJSONArray("일반상식"); // Fallback 기본 카테고리
                }
            } catch (Exception e) {
                System.out.println("[SERVER] quizzes.json 로드 실패. 기본 백업 문제를 로딩합니다.");
                String fallbackJson = "[{\"question\":\"[백업] 자바의 최상위 부모 클래스는?\",\"options\":[\"1.String\",\"2.Object\",\"3.System\",\"4.Void\"],\"answer\":2}]";
                quizArray = new JSONArray(fallbackJson);
            }
            
            List<Integer> indices = new ArrayList<>();
            for (int k = 0; k < quizArray.length(); k++) {
                indices.add(k);
            }
            Collections.shuffle(indices); // 순서를 완전히 섞음

            int totalQuestions = Math.min(5, quizArray.length());

            for (int i = 0; i < totalQuestions; i++) {
                // 섞인 인덱스 묶음에서 순서대로 하나씩 뽑아 매핑
                int randomIndex = indices.get(i);
                JSONObject quiz = quizArray.getJSONObject(randomIndex);
                
                String question = quiz.getString("question");
                JSONArray options = quiz.getJSONArray("options");
                int correctAnswer = quiz.getInt("answer");

                out.println("NEXT_QUESTION"); 
                out.println((i + 1) + " / " + totalQuestions); 
                out.println(question);
                out.println(options.toString()); 

                TimerThread timer = new TimerThread(out, 15, this);
                timer.start();

                String clientAnswerStr = in.readLine();
                timer.interrupt(); 
                
                if (clientAnswerStr == null) break; 
                
                int clientAnswer = Integer.parseInt(clientAnswerStr.trim());
                
                if (clientAnswer < 0 || clientAnswer > 4) {
                    throw new QuizException("잘못된 프로토콜 입력 범위: " + clientAnswer);
                }

                if (clientAnswer == 0) {
                    out.println("시간 초과! 제한 시간 15초가 지나 오답 처리되었습니다.");
                } else if (clientAnswer == correctAnswer) {
                    scoreManager.addScore(nickname, 200); 
                    out.println("정답입니다! (+200점)");
                } else {
                    out.println("틀렸습니다! 정답은 " + correctAnswer + "번입니다.");
                }
            }

            int finalScore = scoreManager.getScore(nickname);
            out.println("GAME_OVER");
            out.println(finalScore);
            
        } catch (QuizException qe) {
            System.out.println("[SERVER] 프로토콜 위반: " + qe.getMessage());
        } catch (Exception e) {
            System.out.println("[SERVER] 오류 발생");
            e.printStackTrace();
        } finally {
            if (nickname != null) {
                scoreManager.clearPlayer(nickname);
            }
            try {
                socket.close();
            } catch (Exception e) {}
        }
    }
}