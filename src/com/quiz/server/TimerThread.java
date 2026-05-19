package com.quiz.server;

import java.io.PrintWriter;


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
            
            for (int i = limitSeconds; i >= 0; i--) {
                out.println("TIMER_UPDATE");
                out.println(i);
                if (i == 0) {

                    out.println("TIME_OUT_SIGNAL");
                    System.out.println("⏰ [TIMER] 제한 시간이 만료되어 타임아웃을 발생시킵니다.");
                    break;
                }

                Thread.sleep(1000); 
            }
        } catch (InterruptedException e) {
            System.out.println("[TIMER] 사용자가 정답을 제출하여 타이머 스레드가 안전하게 조기 종료됩니다.");
        } catch (Exception e) {
            System.out.println("[TIMER] 타이머 구동 중 예외 발생");
        }
    }
}