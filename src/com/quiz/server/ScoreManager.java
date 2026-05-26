package com.quiz.server;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 현재 진행 중인 게임 세션의 점수를 메모리에서 관리하는 싱글톤 클래스.
 * 여러 {@link ClientHandler} 스레드가 동시에 접근할 수 있으므로
 * {@link ConcurrentHashMap}을 사용하여 스레드 안전성을 보장한다.
 */
public class ScoreManager {

    private ConcurrentHashMap<String, Integer> playerScores;

    private static final ScoreManager instance = new ScoreManager();

    private ScoreManager() {
        playerScores = new ConcurrentHashMap<>();
    }

    /**
     * ScoreManager의 싱글톤 인스턴스를 반환한다.
     *
     * @return 싱글톤 인스턴스
     */
    public static ScoreManager getInstance() {
        return instance;
    }

    /**
     * 지정한 플레이어의 점수를 누적한다.
     *
     * @param nickname 플레이어 닉네임
     * @param points   추가할 점수
     */
    public void addScore(String nickname, int points) {
        playerScores.put(nickname, playerScores.getOrDefault(nickname, 0) + points);
    }

    /**
     * 지정한 플레이어의 현재 총점을 반환한다.
     *
     * @param nickname 플레이어 닉네임
     * @return 총점 (등록된 기록이 없으면 0)
     */
    public int getScore(String nickname) {
        return playerScores.getOrDefault(nickname, 0);
    }

    /**
     * 게임 종료 후 플레이어의 점수 기록을 메모리에서 제거한다.
     *
     * @param nickname 플레이어 닉네임
     */
    public void clearPlayer(String nickname) {
        playerScores.remove(nickname);
    }
}
