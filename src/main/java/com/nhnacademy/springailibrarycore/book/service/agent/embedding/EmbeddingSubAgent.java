package com.nhnacademy.springailibrarycore.book.service.agent.embedding;

import static com.nhnacademy.springailibrarycore.config.RedisCacheConfig.CACHE_EMBEDDING;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 텍스트를 float[] 임베딩 벡터로 변환하는 서비스입니다.
 * Spring AI의 EmbeddingModel을 래핑합니다.
 */
@Service
public class EmbeddingSubAgent {

    private final EmbeddingModel embeddingModel;

    public EmbeddingSubAgent(
            @Qualifier("openAiEmbeddingModel")
            EmbeddingModel embeddingModel
    ) {
        this.embeddingModel = embeddingModel;
    }
    /**
     * 주어진 텍스트를 임베딩하여 float[] 벡터를 반환합니다.
     *
     * @param text 임베딩할 텍스트
     * @return 임베딩 벡터
     */
    @Cacheable(value = CACHE_EMBEDDING, key = "#text")
    public float[] getEmbedding(String text) {
        return embeddingModel.embed(text);
    }
}
