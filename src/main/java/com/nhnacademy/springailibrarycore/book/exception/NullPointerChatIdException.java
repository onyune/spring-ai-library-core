package com.nhnacademy.springailibrarycore.book.exception;

public class NullPointerChatIdException extends RuntimeException {
    public NullPointerChatIdException() {
        super("chatId가 null 입니다.");
    }
}
