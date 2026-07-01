package com.nhnacademy.springailibrarycore.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 일반 캐싱(@Cacheable, @CacheEvict 등)용 Redis, Caffein 설정입니다.
 *
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:#{null}}")
    private String redisPassword;

    /** 일반 캐싱용 Redis DB 번호 (벡터 캐시 DB와 분리) */
    @Value("${spring.data.redis.cache.database:0}")
    private int cacheDatabase;

    public static final String CACHE_KEYWORD_SEARCH = "keywordSearch";
    public static final String CACHE_VECTOR_SEARCH  = "vectorSearch";
    public static final String CACHE_HYBRID_SEARCH  = "hybridSearch";
    public static final String CACHE_EMBEDDING    = "embedding_v4";
    public static final String CACHE_QUERY_ANALYSIS = "queryAnalysisCache";

    /**
     * 일반 캐싱 전용
     * spring-boot-starter-data-redis 의 자동 구성 팩토리와 분리하기 위해 직접 생성.
     */
    @Bean(name = "cacheRedisConnectionFactory")
    public RedisConnectionFactory cacheRedisConnectionFactory() {
        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setDatabase(cacheDatabase);
        if (redisPassword != null && !redisPassword.isBlank()) {
            config.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(config);
    }

    /**
     * Spring Cache 추상화에서 사용할 빈.
     *
     * 직렬화: 키는 String, 값은 JSON(Jackson)으로 저장
     * LocalDate 등 Java 8 Time 타입도 JavaTimeModule로 처리
     */
    /**
     * L1 로컬 메모리 캐시 (Caffeine)
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(10)));
        return cacheManager;
    }

    /**
     * L2 원격 분산 캐시 (Redis)
     */
    @Bean
    public CacheManager redisCacheManager(
            @Qualifier("cacheRedisConnectionFactory")
            RedisConnectionFactory connectionFactory
    ) {
        // JSON 직렬화 설정 (타입 정보 포함 → 역직렬화 시 원래 타입 복원)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        
        // PageImpl 역직렬화 이슈 해결을 위한 커스텀 Deserializer 등록
        SimpleModule pageModule = new SimpleModule();
        mapper.registerModule(pageModule);

        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(mapper);

        // 기본 캐시 설정 (TTL 30분, null 값 캐싱 금지)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // 캐시별 TTL 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CACHE_KEYWORD_SEARCH, defaultConfig.entryTtl(Duration.ofHours(3)));
        cacheConfigurations.put(CACHE_VECTOR_SEARCH, defaultConfig.entryTtl(Duration.ofHours(3)));
        cacheConfigurations.put(CACHE_HYBRID_SEARCH, defaultConfig.entryTtl(Duration.ofHours(3)));
        cacheConfigurations.put(CACHE_EMBEDDING, defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put(CACHE_QUERY_ANALYSIS, defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * L1(Caffeine) -> L2(Redis Exact Match) 순차 조회를 위한 Composite Manager
     */
    @Primary
    @Bean
    public CacheManager cacheManager(
            @Qualifier("caffeineCacheManager") CacheManager caffeineCacheManager,
            @Qualifier("redisCacheManager") CacheManager redisCacheManager
    ) {
        return new MultiLevelCacheManager(caffeineCacheManager, redisCacheManager);
    }


}
