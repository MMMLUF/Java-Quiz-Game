package com.quiz.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 퀴즈 게임 서버의 진입점.
 * 지정된 포트에서 클라이언트 접속을 대기하고,
 * 접속마다 {@link ClientHandler} 스레드를 생성하여 독립적으로 처리한다.
 */
public class QuizServer {

    /** 서버가 수신 대기하는 TCP 포트 번호 */
    private static final int PORT = 9999;

    /**
     * 서버를 시작하고 클라이언트 접속을 무한 대기한다.
     *
     * @param args 사용하지 않음
     */
    public static void main(String[] args) {
        new HallOfFameDAO().createTable();
        System.out.println("[SERVER] 랭킹 DB 초기화 완료");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
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
