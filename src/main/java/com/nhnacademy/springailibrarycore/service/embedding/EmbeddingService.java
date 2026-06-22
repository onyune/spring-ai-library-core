package com.nhnacademy.springailibrarycore.service.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

/**
 * 텍스트를 float[] 임베딩 벡터로 변환하는 서비스입니다.
 * Spring AI의 {@link EmbeddingModel}을 래핑합니다.
 */
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * 주어진 텍스트를 임베딩하여 float[] 벡터를 반환합니다.
     *
     * @param text 임베딩할 텍스트
     * @return 임베딩 벡터
     */
    public float[] getEmbedding(String text) {
        return embeddingModel.embed(text);
    }
}
