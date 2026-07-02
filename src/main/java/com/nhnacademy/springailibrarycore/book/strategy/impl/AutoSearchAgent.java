package com.nhnacademy.springailibrarycore.book.strategy.impl;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

/**
 * AUTO는 ChatClient가 사용자의 질문에 맞춰
 * Keyword 전략을 쓸지, RAG 전략을 쓸 지 결정하여 해당 전략을 실행시킵니다.
 */
@Slf4j
@Service
public class AutoSearchAgent {
    private final ChatClient chatClient;

    public AutoSearchAgent(@Qualifier("ollamaChatClientBuilder") ChatClient.Builder chatClient) {
        String systemPrompt = """
                당신은 도서 검색 요청을 분석하여 적절한 검색 전략(SearchType)을 결정하는 AI 라우터입니다.

                [검색 전략(SearchType) 분류 기준]
                1. KEYWORD: 단순 명사, 단일 단어, 책 제목, 저자 이름 등 (예: 스프링부트, 자바, 홍길동)
                2. RAG: 문장형 질문, 맥락/난이도/상황이 포함된 추천 요청 (예: 입문하기 좋은 책, 어려운 개발 서적, ~하는 법 알려줘)

                [parsedQuery 추출 규칙 - 매우 중요]
                - KEYWORD로 판별된 경우: 불필요한 조사나 기호를 제거하고 핵심 명사(키워드)만 남기세요.
                - RAG로 판별된 경우: 사용자가 입력한 원본 질문을 단어 하나, 조사 하나 빼지 말고 100% 원본 그대로 반환하세요. "입문하기 좋은", "어려운"과 같은 수식어는 추천의 핵심 맥락이므로 절대 요약하거나 삭제하면 안 됩니다.

                [예시]
                - 사용자: "스프링부트" -> searchType: "KEYWORD", parsedQuery: "스프링부트"
                - 사용자: "파이썬 프로그래밍" -> searchType: "KEYWORD", parsedQuery: "파이썬 프로그래밍"
                - 사용자: "자바를 처음 배우는데 입문하기 좋은 쉬운 개발자 책 추천해줘" -> searchType: "RAG", parsedQuery: "자바를 처음 배우는데 입문하기 좋은 쉬운 개발자 책 추천해줘"
                - 사용자: "난이도가 높은 어려운 심화 개발 서적" -> searchType: "RAG", parsedQuery: "난이도가 높은 어려운 심화 개발 서적"
                """;
        this.chatClient = chatClient
                .defaultSystem(systemPrompt)
                .build();
    }

    public SearchIntent search(BookSearchRequest request) {
        return chatClient.prompt()
                .user(request.keyword())
                .call()
                .entity(SearchIntent.class);
    }

    public record SearchIntent(@Description(value = "단순 명사나 제목이면 KEYWORD, 문장형이나 조건(난이도, 상황 등)이 포함된 요청이면 RAG")
                               SearchType searchType,
                               @Description(value = "KEYWORD일 땐 핵심 명사만 추출. RAG일 땐 요약이나 수정 절대 없이 사용자 입력 원본 문자열 100% 그대로 유지")
                               String parsedQuery){}
}
