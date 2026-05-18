package com.quiz.server;

import java.util.concurrent.ConcurrentHashMap;

// 제안서 스펙: Singleton 패턴을 적용하여 전역에서 단 하나의 객체로 점수를 관리 [cite: 164, 166]
public class ScoreManager {
    // 멀티스레드 환경에서 안전하도록 ConcurrentHashMap 사용 [cite: 137]
    private ConcurrentHashMap<String, Integer> playerScores;

    // 1. 오직 단 하나의 인스턴스만 정적으로 생성 
    private static final ScoreManager instance = new ScoreManager();

    // 2. 외부에서 new 키워드로 객체를 생성하지 못하도록 private 생성자 선언 [cite: 164, 166]
    private ScoreManager() {
        playerScores = new ConcurrentHashMap<>();
    }

    // 3. 외부에서 인스턴스를 가져갈 수 있는 유일한 통로 제공 
    public static ScoreManager getInstance() {
        return instance;
    }

    // 점수 가산 메서드
    public void addScore(String nickname, int points) {
        playerScores.put(nickname, playerScores.getOrDefault(nickname, 0) + points);
    }

    // 최종 점수 조회 메서드
    public int getScore(String nickname) {
        return playerScores.getOrDefault(nickname, 0);
    }

    // 게임 종료 시 메모리 정리
    public void clearPlayer(String nickname) {
        playerScores.remove(nickname);
    }
}