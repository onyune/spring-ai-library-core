package com.nhnacademy.springailibrarycore.book.service.agent.search;

import static com.nhnacademy.springailibrarycore.config.CacheConfig.CACHE_QUERY_ANALYSIS;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사용자 자연어 질의에서 불용어를 제거하고 핵심 키워드를 추출하는 분석 에이전트.
 */
@Component
public class QueryAnalyzerSubAgent {

    // FULL 모델로 코모란 초기화 (서버 구동 시 한 번만 로드)
    private final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

    // 도서 검색에 방해되는 자체 불용어 사전 구축
    private final Set<String> stopWords = Set.of(
            "책", "도서", "관련", "추천", "찾아", "알려", "검색", "대해", "주세요", "부탁"
    );

    /**
     * 사용자 자연어 질의에서 핵심 키워드(명사)만 추출합니다.
     * 추출 결과는 캐싱되어 반복된 질의 시 즉각 응답합니다.
     *
     * 형태소 분석기는 CPU연산이므로 캐싱하는게 유리..!
     *
     * @param sentence 원본 자연어 질의
     * @return 정제된 핵심 키워드 문자열
     */
    @Cacheable(value = CACHE_QUERY_ANALYSIS, key = "#sentence")
    public String extractKeywords(String sentence) {
        if (sentence == null || sentence.isBlank()) return "";

        // 코모란을 통해 형태소 분석 후 명사만 추출
        List<String> nouns = komoran.analyze(sentence).getNouns();

        // 불용어 사전에 있는 단어 제거 후 띄어쓰기로 결합
        String extracted = nouns.stream()
                .filter(noun -> !stopWords.contains(noun))
                .collect(Collectors.joining(" "));
                
        // 만약 명사를 하나도 추출하지 못했다면 (예: 사용자가 "찾아줘"만 입력), 빈 문장보단 원본을 반환하는 것이 안전
        return extracted.isBlank() ? sentence : extracted;
    }
}
