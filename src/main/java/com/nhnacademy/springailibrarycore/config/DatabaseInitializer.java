package com.nhnacademy.springailibrarycore.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 스프링 부트 애플리케이션 시작 시 데이터베이스 네이티브 구문을 초기화하는 컴포넌트입니다.
 *
 * JPA의 ddl-auto 기능만으로는 pgvector의 HNSW 인덱스나 사용자 정의 함수(ts_match_korean)를
 * 생성할 수 없으므로, 애플리케이션 구동 직후(ApplicationRunner) 이 클래스가 실행되어
 * 데이터베이스 최적화 요소를 적용합니다.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[DatabaseInitializer] 네이티브 DB 최적화 및 함수 초기화를 시작합니다...");

        try {
            // 인덱스 생성 작업
            // books 테이블 인덱스는 대량 데이터 삽입(Batch) 완료 후 성능을 위해 배치 잡에서 생성하도록 위임함.
            // 현재 애플리케이션 기동 시에는 시맨틱 캐시(book_search_cache) 인덱스 1개만 생성하므로 동기적으로 처리합니다.
            createHnswIndexIfNotExists(
                    "idx_book_search_cache_embedding", 
                    "book_search_cache", 
                    "query_embedding"
            );

            // PostgreSQL 한글 전문 검색 매칭 함수 (GIN 인덱스 지원용)
            // CREATE OR REPLACE 구문이므로 이미 있어도 안전하게 최신 버전으로 덮어씁니다.
            String createFunctionSql = """
                CREATE OR REPLACE FUNCTION ts_match_korean(content text, keyword text) RETURNS boolean AS $$
                BEGIN
                    -- null 방지 및 기본 전문 검색 매칭(plainto_tsquery)
                    RETURN to_tsvector('simple', coalesce(content, '')) @@ plainto_tsquery('simple', coalesce(keyword, ''));
                END;
                $$ LANGUAGE plpgsql IMMUTABLE;
                """;
            jdbcTemplate.execute(createFunctionSql);
            log.info("[DatabaseInitializer] ts_match_korean() GIN 검색 지원 함수 구동 완료");

            log.info("[DatabaseInitializer] 모든 네이티브 DB 초기화가 성공적으로 완료되었습니다.");

        } catch (Exception e) {
            log.error("[DatabaseInitializer] 네이티브 DB 초기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * HNSW 인덱스가 존재하지 않을 경우에만 세션 메모리를 조정하여 안전하게 인덱스를 생성합니다.
     * 
     * @param indexName 생성할 인덱스 명
     * @param tableName 대상 테이블 명
     * @param columnName 대상 벡터 컬럼 명
     */
    private void createHnswIndexIfNotExists(String indexName, String tableName, String columnName) {
        // 인덱스가 이미 존재하는지 확인 (pg_indexes 뷰 조회)
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname = ?", 
                Integer.class, 
                indexName
        );

        if (count != null && count > 0) {
            log.info("[DatabaseInitializer] {} 테이블의 {} 인덱스가 이미 존재하여 생성을 건너뜁니다.", tableName, indexName);
            return;
        }

        log.info("[DatabaseInitializer] {} 테이블의 {} 인덱스 생성을 시작합니다. (데이터 양에 따라 시간이 소요될 수 있습니다.)", tableName, indexName);
        
        // 메모리 이슈를 방지하기 위해 단일 세션에 대해 병렬 워커 비활성화 및 메모리 제한을 걸고 인덱스를 생성합니다.
        String sql = String.format(
                "SET max_parallel_maintenance_workers = 0; " +
                "SET maintenance_work_mem = '64MB'; " +
                "CREATE INDEX %s ON %s USING hnsw (%s vector_cosine_ops);",
                indexName, tableName, columnName
        );
        
        jdbcTemplate.execute(sql);
        log.info("[DatabaseInitializer] {} 테이블 HNSW 인덱스 생성 완료!", tableName);
    }
}
