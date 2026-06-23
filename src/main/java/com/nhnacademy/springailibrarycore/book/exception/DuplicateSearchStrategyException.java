package com.nhnacademy.springailibrarycore.book.exception;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;

public class DuplicateSearchStrategyException extends RuntimeException {
    private final SearchType searchType;
    public DuplicateSearchStrategyException(SearchType searchType) {
        super("검색 전략 중복: "+searchType);
        this.searchType=searchType;
    }

    public SearchType getSearchType() {
        return searchType;
    }
}
