package com.quiz.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class QuizServer {
    public static void main(String[] args) {
        int port = 9999;
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("🚀 [SERVER] 퀴즈 서버가 가동되었습니다. 클라이언트를 기다립니다...");

            while (true) {
                // 클라이언트가 접속하면 소켓을 생성하고
                Socket socket = serverSocket.accept();
                System.out.println("📱 [SERVER] 플레이어 접속 완료! (" + socket.getInetAddress() + ")");
                
                // 제안서 스펙: 독립적인 ClientHandler 스레드를 생성하여 게임 로직을 넘깁니다. [cite: 83, 118, 121]
                ClientHandler handler = new ClientHandler(socket);
                handler.start(); // 스레드 시작!
            }
            
        } catch (IOException e) {
            System.out.println("❌ [SERVER] 서버 구동 중 에러 발생");
            e.printStackTrace();
        }
    }
}