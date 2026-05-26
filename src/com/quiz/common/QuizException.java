package com.quiz.common;

/**
 * 클라이언트-서버 간 프로토콜 위반이나 게임 로직 오류를 나타내는 커스텀 예외 클래스.
 * {@link ClientHandler}에서 잘못된 응답값이 수신될 때 발생시킨다.
 */
public class QuizException extends Exception {

    /**
     * @param message 예외 원인을 설명하는 메시지
     */
    public QuizException(String message) {
        super(message);
    }
}
