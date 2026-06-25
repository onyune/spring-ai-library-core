package com.nhnacademy.springailibrarycore.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
 * bookSearch  - 도서 검색 결과 (10분)
 * bookDetail  - 도서 상세 정보 (1시간)
 * embedding   - 임베딩 벡터 결과 (24시간)
 * 그 외 기본값         - 30분
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
    public static final String CACHE_EMBEDDING    = "embedding_v1";

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
        
        // PageImpl 역직렬화 이슈 해결을 위한 커스텀 Deserializer 등록
        SimpleModule pageModule = new SimpleModule();
        pageModule.addDeserializer(PageImpl.class, new PageImplDeserializer());
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
        cacheConfigurations.put(CACHE_BOOK_SEARCH, defaultConfig.entryTtl(Duration.ofHours(3))); // TODO: 캐시 시간 상의하기
        cacheConfigurations.put(CACHE_EMBEDDING, defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

}

/**
 * Jackson이 Spring Data의 PageImpl을 역직렬화하지 못하는 현상을 해결하기 위한 커스텀 Deserializer.
 */
class PageImplDeserializer extends JsonDeserializer<PageImpl<?>> {
    @Override
    public PageImpl<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        // content 목록 역직렬화
        List<Object> content = new ArrayList<>();
        JsonNode contentNode = node.get("content");
        if (contentNode != null && contentNode.isArray()) {
            JsonNode actualElements = contentNode;
            if (contentNode.size() == 2 && contentNode.get(0).isTextual() && contentNode.get(1).isArray()) {
                actualElements = contentNode.get(1);
            }
            for (JsonNode elem : actualElements) {
                // DefaultTyping이 켜져 있으므로 Object.class 지정 시 타입 메타데이터를 파싱하여 알맞은 객체로 환원됨
                content.add(mapper.treeToValue(elem, Object.class));
            }
        }

        // 페이지 메타데이터 파싱
        int number = node.has("number") ? node.get("number").asInt() : 0;
        int size = node.has("size") ? node.get("size").asInt() : (content.isEmpty() ? 10 : content.size());
        long totalElements = node.has("totalElements") ? node.get("totalElements").asLong() : content.size();

        return new PageImpl<>(content, PageRequest.of(number, size), totalElements);
    }
}
