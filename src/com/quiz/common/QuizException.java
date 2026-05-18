package com.quiz.common;

// 제안서 스펙: 잘못된 프로토콜이나 메시지 포맷을 감지하기 위한 사용자 정의 예외 클래스 
public class QuizException extends Exception {
    public QuizException(String message) {
        super(message);
    }
}