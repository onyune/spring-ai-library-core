package com.nhnacademy.springailibrarycore.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
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
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 일반 캐싱(@Cacheable, @CacheEvict 등)용 Redis 설정입니다.
 *
 * 벡터 캐시({@link RedisVectorStoreConfig})와 다른 커넥션 팩토리를 사용하므로
 * 일반 캐시와 벡터 캐시가 서로 간섭하지 않습니다.
 *
 * 캐시 이름 & TTL
 *
 * bookSearch  - 도서 검색 결과 (10분)
 * bookDetail  - 도서 상세 정보 (1시간)
 * embedding   - 임베딩 벡터 결과 (24시간)
 * 그 외 기본값         - 30분
 * </ul>
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:#{null}}")
    private String redisPassword;

    /** 일반 캐싱용 Redis DB 번호 (벡터 캐시 DB와 분리) */
    @Value("${spring.data.redis.cache.database:0}")
    private int cacheDatabase;

    public static final String CACHE_BOOK_SEARCH  = "bookSearch";
    public static final String CACHE_EMBEDDING    = "embedding";

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
    @Bean
    public CacheManager cacheManager(
            @Qualifier("cacheRedisConnectionFactory")
            RedisConnectionFactory connectionFactory
    ) {
        // JSON 직렬화 설정 (타입 정보 포함 → 역직렬화 시 원래 타입 복원)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
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
        cacheConfigurations.put(CACHE_BOOK_SEARCH,
                defaultConfig.entryTtl(Duration.ofHours(3))); // TODO: 캐시 시간 상의하기
        cacheConfigurations.put(CACHE_EMBEDDING,
                defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
