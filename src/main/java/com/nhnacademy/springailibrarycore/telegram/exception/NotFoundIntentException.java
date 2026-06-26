package com.nhnacademy.springailibrarycore.telegram.exception;

public class NotFoundIntentException extends RuntimeException {
    public NotFoundIntentException() {
        super("질문의 의도를 파악하지 못했습니다. 책 제목이나 도서관에 대해 다시 질문해 주시겠어요?");
    }
}
