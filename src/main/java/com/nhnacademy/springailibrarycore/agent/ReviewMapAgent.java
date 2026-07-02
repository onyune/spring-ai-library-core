package com.nhnacademy.springailibrarycore.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReviewMapAgent {
    private final ChatClient client;

    public ReviewMapAgent(@Qualifier("geminiChatClientBuilder") ChatClient.Builder clientBuilder) {
        this.client = clientBuilder.build();
    }

    public String summarizeChunk(String chunkText) {
        String mapPrompt = """
                제시된 도서 리뷰 목록의 핵심 내용을 요약하여 긍정적인 평과 부정적인 평을 각각 2~3줄 내외로 요점만 정리하세요.
                리뷰 외의 잡담이나 메타 발언은 금지합니다.
                """;

        return client.prompt()
                .system(mapPrompt)
                .user(chunkText)
                .call()
                .content();
    }
}
