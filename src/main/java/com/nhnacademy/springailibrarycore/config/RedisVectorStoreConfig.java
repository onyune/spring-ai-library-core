package com.nhnacademy.springailibrarycore.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

/**
 * Redis Stack을 벡터 캐시로 사용하기 위한 설정입니다.
 * pgVectorStore는 spring.ai.vectorstore.pgvector.* 자동 구성으로 등록되므로
 * 여기서는 Redis 쪽만 수동으로 등록합니다.
 */
@Configuration
public class RedisVectorStoreConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:#{null}}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * Redis Vector Store 빈 등록 (캐시 전용)
     */
    @Bean(name = "redisVectorStore")
    public RedisVectorStore redisVectorStore(EmbeddingModel embeddingModel) {
        DefaultJedisClientConfig.Builder configBuilder = DefaultJedisClientConfig.builder()
                .database(redisDatabase);  // DB 번호 지정

        if (redisPassword != null && !redisPassword.isBlank()) {
            configBuilder.password(redisPassword);
        }

        JedisPooled jedisPooled = new JedisPooled(
                new HostAndPort(redisHost, redisPort),
                configBuilder.build()
        );

        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("book-index")  // Redis Search 인덱스 이름
                .prefix("book:")          // Redis Key 프리픽스
                .initializeSchema(true)   // 인덱스 없으면 자동 생성
                .build();
    }
}
