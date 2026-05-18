package com.quiz.server;

import java.io.PrintWriter;

// 제안서 스펙: extends Thread를 사용해 문제별 제한 시간 카운트다운을 전담하는 스레드
public class TimerThread extends Thread {
    private PrintWriter out;
    private int limitSeconds;
    private ClientHandler handler;

    public TimerThread(PrintWriter out, int limitSeconds, ClientHandler handler) {
        this.out = out;
        this.limitSeconds = limitSeconds;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            // 제한 시간(15초)부터 0초까지 1초씩 줄여가며 클라이언트에 실시간 브로드캐스팅
            for (int i = limitSeconds; i >= 0; i--) {
                // 클라이언트에게 실시간 타이머 신호를 보냅니다.
                out.println("TIMER_UPDATE");
                out.println(i); // 남은 초 전송 (예: 15, 14, 13...)

                if (i == 0) {
                    // 0초가 되면 타임아웃 신호를 보내고 종료합니다.
                    out.println("TIME_OUT_SIGNAL");
                    System.out.println("⏰ [TIMER] 제한 시간이 만료되어 타임아웃을 발생시킵니다.");
                    break;
                }

                // 1초 동안 스레드를 잠재웁니다. (제안서 스펙 준수)
                Thread.sleep(1000); 
            }
        } catch (InterruptedException e) {
            // 유저가 시간 내에 버튼을 누르면 ClientHandler가 이 스레드를 interrupt()하여 깨웁니다.
            System.out.println("✨ [TIMER] 사용자가 정답을 제출하여 타이머 스레드가 안전하게 조기 종료됩니다.");
        } catch (Exception e) {
            System.out.println("❌ [TIMER] 타이머 구동 중 예외 발생");
        }
    }
}