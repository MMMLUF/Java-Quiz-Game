package com.quiz.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class QuizServer {
    public static void main(String[] args) {
        int port = 9999;
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SERVER] 퀴즈 서버가 가동되었습니다. 클라이언트를 기다립니다.");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] 플레이어 접속 완료 (" + socket.getInetAddress() + ")");
                
                ClientHandler handler = new ClientHandler(socket);
                handler.start();
            }
            
        } catch (IOException e) {
            System.out.println("[SERVER] 서버 구동 중 에러 발생");
            e.printStackTrace();
        }
    }
}