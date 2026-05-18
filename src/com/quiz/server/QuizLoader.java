package com.quiz.server;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.json.JSONObject;

public class QuizLoader {
    public static void main(String[] args) {
        try {
            // 1. quizzes.json 파일의 전체 내용을 글자(문자열)로 통째로 읽어옵니다.
            String content = new String(Files.readAllBytes(Paths.get("quizzes.json")));
            
            // 2. 읽어온 글자를 JSON 배열([ ] 구조)로 변환합니다.
            JSONArray quizArray = new JSONArray(content);
            
            System.out.println("=========================================");
            System.out.println("🎉 퀴즈 데이터 로딩 성공! 총 " + quizArray.length() + "문제를 불렀습니다.");
            System.out.println("=========================================");
            
            // 3. 반복문을 돌며 문제를 하나씩 꺼내서 콘솔에 출력합니다.
            for (int i = 0; i < quizArray.length(); i++) {
                // 배열 안에서 { } 구조로 된 문제 객체를 하나 꺼냅니다.
                JSONObject quizObj = quizArray.getJSONObject(i);
                
                String question = quizObj.getString("question");
                JSONArray options = quizObj.getJSONArray("options");
                int answer = quizObj.getInt("answer");
                
                // 콘솔창에 예쁘게 출력하기
                System.out.println((i + 1) + "번 문제: " + question);
                for (int j = 0; j < options.length(); j++) {
                    System.out.println("   " + options.getString(j));
                }
                System.out.println("👉 정답 번호: " + answer + "번");
                System.out.println("-----------------------------------------");
            }
            
        } catch (Exception e) {
            System.out.println("❌ 에러 발생! 파일 경로를 확인하거나 라이브러리를 확인하세요.");
            e.printStackTrace();
        }
    }
}