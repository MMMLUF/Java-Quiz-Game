package com.quiz.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import org.json.JSONArray;

public class QuizClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 9999;
        
        // 키보드 입력을 위한 스캐너 준비
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {
            
            System.out.println("✅ [CLIENT] 서버 접속 완료!");

            // 1. 서버가 보내는 신호 확인
            String signal = in.readLine();
            if ("START_QUIZ".equals(signal)) {
                // 2. 문제와 보기를 서버로부터 수신
                String question = in.readLine();
                String optionsStr = in.readLine();
                JSONArray options = new JSONArray(optionsStr);

                // 3. 콘솔창에 퀴즈 출력 
                System.out.println("\n=========================================");
                System.out.println("🎯 퀴즈 문제: " + question);
                System.out.println("-----------------------------------------");
                for (int i = 0; i < options.length(); i++) {
                    System.out.println(options.getString(i));
                }
                System.out.println("=========================================");
                
                // 4. 유저에게 키보드로 정답 입력 받기
                System.out.print("👉 정답 번호를 입력하세요 (1~4): ");
                int myAnswer = scanner.nextInt();

                // 5. 서버로 정답 전송
                out.println(myAnswer);

                // 6. 결과 받아오기
                String result = in.readLine();
                System.out.println("\n" + result);
            }

        } catch (Exception e) {
            System.out.println("❌ [CLIENT] 에러 발생");
            e.printStackTrace();
        }
    }
}