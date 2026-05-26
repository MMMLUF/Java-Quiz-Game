package com.quiz.server;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.json.JSONObject;

public class QuizLoader {
    public static void main(String[] args) {
        try {
            String content = new String(Files.readAllBytes(Paths.get("quizzes.json")));

            JSONObject allQuizzes = new JSONObject(content);
            int total = 0;

            for (String category : allQuizzes.keySet()) {
                JSONArray quizArray = allQuizzes.getJSONArray(category);
                System.out.println("\n[카테고리: " + category + " / " + quizArray.length() + "문제]");
                System.out.println("-----------------------------------------");

                for (int i = 0; i < quizArray.length(); i++) {
                    JSONObject quizObj = quizArray.getJSONObject(i);
                    String question = quizObj.getString("question");
                    JSONArray options = quizObj.getJSONArray("options");
                    int answer = quizObj.getInt("answer");

                    System.out.println((i + 1) + "번 문제: " + question);
                    for (int j = 0; j < options.length(); j++) {
                        System.out.println("   " + options.getString(j));
                    }
                    System.out.println("정답 번호: " + answer + "번");
                    System.out.println("-----------------------------------------");
                }
                total += quizArray.length();
            }

            System.out.println("\n퀴즈 데이터 로딩 완료. 총 " + total + "문제를 불렀습니다.");

        } catch (Exception e) {
            System.out.println("Error: 파일 경로를 확인하거나 라이브러리를 확인하세요.");
            e.printStackTrace();
        }
    }
}