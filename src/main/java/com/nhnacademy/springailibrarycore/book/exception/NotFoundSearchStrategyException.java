package com.nhnacademy.springailibrarycore.book.exception;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;

public class NotFoundSearchStrategyException extends RuntimeException {
    public NotFoundSearchStrategyException(SearchType searchType) {
        super("지원하지않은 Search전략입니다. searchType: "+ searchType);
    }
}
