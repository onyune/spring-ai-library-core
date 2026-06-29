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
                당신은 도서 리뷰 분석 전문가입니다. 분할되어 요약된 아래의 부분 리뷰 보고서들을 분석하여,
                장점과 단점을 포함한 도서의 최종 종합 의견을 반드시 2~3줄로만 요약해 주세요.
                (장점:, 단점: 등 항목을 나누지 말고, 오직 2~3줄 길이의 텍스트 하나만 출력할 것)
                """;
    
            return client.prompt()
                    .system(reducePrompt)
                    .user(combinedSummaries)
                    .call()
                    .content();
        }
    }