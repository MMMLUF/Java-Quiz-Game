package com.quiz.server;

import java.io.PrintWriter;

/**
 * 문제 제한 시간을 카운트다운하고, 매 초마다 클라이언트에 TIMER_UPDATE 신호를 전송하는 스레드.
 * 시간이 만료되면 TIME_OUT_SIGNAL을 전송한다.
 */
public class TimerThread extends Thread {

    /** 1초를 밀리초로 환산한 값 */
    private static final int TICK_MS = 1000;

    private PrintWriter out;
    private int limitSeconds;
    private volatile int currentSeconds;

    /**
     * @param out          클라이언트로 신호를 전송할 출력 스트림
     * @param limitSeconds 카운트다운 시작 초
     * @param handler      (미사용) 핸들러 참조 — 확장성을 위해 유지
     */
    public TimerThread(PrintWriter out, int limitSeconds, ClientHandler handler) {
        this.out = out;
        this.limitSeconds = limitSeconds;
    }

    /**
     * 현재 남은 시간(초)을 반환한다.
     * volatile 필드를 통해 다른 스레드에서 안전하게 읽을 수 있다.
     *
     * @return 남은 시간(초)
     */
    public int getRemainingSeconds() {
        return currentSeconds;
    }

    /**
     * 카운트다운 루프를 실행한다.
     * 매 초 TIMER_UPDATE와 남은 초를 전송하며, 0초 도달 시 TIME_OUT_SIGNAL을 전송한다.
     * {@link Thread#interrupt()} 호출 시 안전하게 종료된다.
     */
    @Override
    public void run() {
        try {
            for (int i = limitSeconds; i >= 0; i--) {
                currentSeconds = i;
                out.println("TIMER_UPDATE");
                out.println(i);
                if (i == 0) {
                    out.println("TIME_OUT_SIGNAL");
                    System.out.println("⏰ [TIMER] 제한 시간이 만료되어 타임아웃을 발생시킵니다.");
                    break;
                }
                Thread.sleep(TICK_MS);
            }
        } catch (InterruptedException e) {
            System.out.println("[TIMER] 사용자가 정답을 제출하여 타이머 스레드가 안전하게 조기 종료됩니다.");
        } catch (Exception e) {
            System.out.println("[TIMER] 타이머 구동 중 예외 발생");
        }
    }
}
