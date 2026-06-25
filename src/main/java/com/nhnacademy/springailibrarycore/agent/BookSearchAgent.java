package com.nhnacademy.springailibrarycore.agent;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResult;
import com.nhnacademy.springailibrarycore.book.exception.DuplicateSearchStrategyException;
import com.nhnacademy.springailibrarycore.book.exception.NotFoundSearchStrategyException;
import com.nhnacademy.springailibrarycore.book.strategy.SearchStrategy;

import com.nhnacademy.springailibrarycore.book.strategy.impl.AutoSearchAgent;
import com.nhnacademy.springailibrarycore.book.strategy.impl.AutoSearchAgent.SearchIntent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 요청된 검색 유형에 맞는 전랷을 선택하는 검색 지입점
 * 키워드, 벡터, 하이브리드, RAG, AUTO 검색의 구체적인 구현을 Strategy에 위임
 */
@Service
@Transactional(readOnly = true)
public class BookSearchAgent {
    private final Map<SearchType, SearchStrategy> strategyMap;
    private final AutoSearchAgent autoSearchAgent;

    public BookSearchAgent(List<SearchStrategy> strategies, AutoSearchAgent autoSearchAgent) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        SearchStrategy::supports, //Key: Enum 타입
                        Function.identity(), // Value: 전략 객체 자신
                        (existing, replacement)->{ // 중복 키 발생 시 처리 전략
                            throw new DuplicateSearchStrategyException(existing.supports());
                        },
                        ()-> new EnumMap<>(SearchType.class) // 최종 결과 EnumMap화
                ));
        this.autoSearchAgent = autoSearchAgent;
    }

    /**
     * SearchStrategy를 결정합니다
     * AUTO일 경우 ChatClient한테 사용자의 질문의 searchType을 파악하여 KEYWORD인지, RAG인지 결정하게 하고
     * KEYWORD, RAG SearchStrategy를 실행시킵니다.
     * @param pageable 페이지
     * @param bookSearchRequest 사용자의 search 요청
     * @return record BookSearchResult(Page<BookSearchResponse> books)
     */
    public BookSearchResult searchBooks(Pageable pageable, BookSearchRequest bookSearchRequest){
        SearchType targetSearchType = bookSearchRequest.searchType();
        String targetKeyword = bookSearchRequest.keyword();

        if(targetSearchType.equals(SearchType.AUTO)){
            SearchIntent searchIntent = autoSearchAgent.search(bookSearchRequest);
            targetSearchType = searchIntent.searchType();
            targetKeyword = searchIntent.parsedQuery();
        }

        SearchStrategy strategy = strategyMap.get(targetSearchType);
        if(strategy==null){
            throw new NotFoundSearchStrategyException(bookSearchRequest.searchType());
        }

        BookSearchRequest refinedRequest = new BookSearchRequest(
                targetKeyword,
                bookSearchRequest.isbn(),
                targetSearchType,
                bookSearchRequest.vector(),
                bookSearchRequest.warmUp()
        );

        Page<BookSearchResponse> books = strategy.search(pageable,refinedRequest);
        return new BookSearchResult(books);
    }
}
