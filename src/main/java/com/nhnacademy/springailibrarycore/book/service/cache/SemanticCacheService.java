package com.nhnacademy.springailibrarycore.book.service.cache;

import com.nhnacademy.springailibrarycore.book.domain.BookSearchCache;
import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.repository.BookSearchCacheRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 질문 임베딩을 기준으로 RAG 추천 결과의 시맨틱 캐시를 조회하고 저장하는 서비스입니다.
 *
 * pgvector 저장소에 유사도 검색을 요청하고, 캐시 Hit 시 JSON 결과 복원과 접근 통계
 * 갱신을 수행합니다. 캐시 Miss 결과는 TTL과 함께 PostgreSQL에 저장합니다.
 */
@Service
@Slf4j
public class SemanticCacheService {

    @Value("${semantic.cache.similarity.threshold:0.87}")
    private Double cacheThreshold;
    @Value("${semantic.cache.similarity.ttl:1800}") //30*60
    private Integer cacheTtl;

    private final BookSearchCacheRepository cacheRepository;
    private final RecommendationCacheCodec cacheCodec;
    private final Clock clock;

    @Autowired
    public SemanticCacheService(
            BookSearchCacheRepository cacheRepository,
            RecommendationCacheCodec cacheCodec
    ) {
        this(cacheRepository, cacheCodec, Clock.systemUTC());
    }

    SemanticCacheService(
            BookSearchCacheRepository cacheRepository,
            RecommendationCacheCodec cacheCodec,
            Clock clock
    ) {
        this.cacheRepository = cacheRepository;
        this.cacheCodec = cacheCodec;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<List<BookSearchResponse>> findCachedResult(
            SearchType searchType,
            String question,
            float[] questionVector
    ) {
        // 레포지터리에서 질문과 유사한 것들 찾기
        Optional<BookSearchCache> matchedCache =
                cacheRepository.findBestMatch(
                        searchType,
                        questionVector,
                        cacheThreshold
                );

        // 비어있으면 캐시 미스로 빈값 리턴
        if (matchedCache.isEmpty()) {
            log.info("[시맨틱 캐시 Miss SearchType= {}] 질문={}", searchType,question);
            return Optional.empty();
        }

        BookSearchCache cache = matchedCache.get();
        // JSON -> DTO 변환
        List<BookSearchResponse> cachedRecommendations = cacheCodec.decode(cache.getResult());

        // DTO가 비어있으면 캐시 삭제
        if (cachedRecommendations.isEmpty()) {
            cacheRepository.delete(cache);
            log.info("[시맨틱 캐시 무효화 SearchType= {}] 빈 추천 결과 삭제: 질문={}", searchType, cache.getQuery());
            return Optional.empty();
        }

        // 캐시 접근 기록 갱신
        cache.recordAccess(OffsetDateTime.now(clock)); //Dirty checking

        log.info("[시맨틱 캐시 Hit SearchType= {}] 질문={}, 캐시 질문={}, 기준 유사도={}", searchType,question, cache.getQuery(), cacheThreshold);
        // 추천 캐시 결과물 반환
        return Optional.of(cachedRecommendations);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(
            SearchType searchType,
            String question,
            float[] questionVector,
            List<BookSearchResponse> recommendations
    ) {
        // 캐시 생성
        BookSearchCache cache = BookSearchCache.create(
                question,
                searchType,
                questionVector,
                cacheCodec.encode(recommendations),
                OffsetDateTime.now(clock),
                cacheTtl
        );

        cacheRepository.save(cache);
    }
}
