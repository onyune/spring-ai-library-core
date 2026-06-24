package com.nhnacademy.springailibrarycore.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
    public class ReviewReduceAgent {
    
        private final ChatClient client;
    
        public ReviewReduceAgent(@Qualifier("geminiChatClientBuilder") ChatClient.Builder chatClientBuilder) {
            this.client = chatClientBuilder.build();
        }
    
        /**
         * 부분 요약 보고서 조각들을 병합하여 깔끔한 마크다운 리포트로 최종 종합 요약합니다.
         */
        public String reduceSummaries(String combinedSummaries) {
            String reducePrompt = """
                당신은 도서 평평 분석 전문가입니다. 분할되어 요약된 아래의 부분 리뷰 보고서들을 분석하여,
                이 도서에 대한 전반적인 장점(Good Points)과 단점(Bad Points)을 정돈하고, 최종 종합 의견을 마크다운 문법으로 깔끔하게 작성해 주세요.
                """;
    
            return client.prompt()
                    .system(reducePrompt)
                    .user(combinedSummaries)
                    .call()
                    .content();
        }
    }