package com.nhnacademy.springailibrarycore.book.service.cache.scheduler;

import com.nhnacademy.springailibrarycore.book.repository.BookSearchCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostgreSQL에 남아 있는 만료 시맨틱 캐시를 주기적으로 정리합니다.
 *
 * 캐시 조회 경로에서 삭제 작업을 분리하여 사용자 요청마다 전체 만료 삭제 쿼리가
 * 실행되지 않도록 합니다.
 */
@Component
@ConditionalOnProperty(
        name = "cache.semantic.cleanup-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
@Slf4j
public class SemanticCacheCleanupScheduler {

    private final BookSearchCacheRepository cacheRepository;
    // 단위 ms
    @Scheduled(
            fixedDelayString = "${cache.semantic.cleanup-delay-ms:300000}"
    )
    @Transactional
    public void deleteExpiredCaches() {
        int deletedCount = cacheRepository.deleteExpired();
        if (deletedCount > 0) {
            log.info("[시맨틱 캐시 정리] 만료 캐시 {}건 삭제", deletedCount);
        }
    }
}
