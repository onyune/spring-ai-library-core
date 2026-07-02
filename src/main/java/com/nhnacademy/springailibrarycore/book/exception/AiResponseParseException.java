package com.nhnacademy.springailibrarycore.book.exception;

public class AiResponseParseException extends RuntimeException {

    public AiResponseParseException(String message) {
        super(message);
    }

    public AiResponseParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
