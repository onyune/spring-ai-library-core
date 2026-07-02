package com.nhnacademy.springailibrarycore.book.exception;

public class NotFoundBookException extends RuntimeException {
    public NotFoundBookException(Long bookId) {
        super("존재하지않은 Book입니다 BookId: "+ bookId);
    }
}
