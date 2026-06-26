package com.nhnacademy.springailibrarycore.telegram.domain;

/**
 * 사용자의 질문 의도를 분류하는 Enum
 */
public enum AssistantIntent {
    /** 도서관 이용 시간, 위치, 규칙 등 도서관 관련 정보 문의 */
    LIBRARY_INFO,
    
    /** 책 추천, 도서 검색, 특정 주제의 책 찾기 등 */
    BOOK_RECOMMENDATION,

    /** LIBRARY_INFO + BOOK_RECOMMENDATION */
    LIBRARY_BOOK_RECOMMENDATION,
    
    /** 일상 대화 또는 기타 알 수 없는 질문 */
    GENERAL_CHAT
}
