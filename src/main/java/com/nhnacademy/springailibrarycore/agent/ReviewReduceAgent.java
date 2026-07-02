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
                당신은 도서 리뷰 요약 전문가입니다. 제시된 리뷰 요약 내용들을 종합하여, 이 책에 대한 최종 종합 의견을 반드시 2~3줄의 한국어 일반 텍스트(Plain Text)로만 작성해 주세요.
                
                [필수 규칙]
                1. 마크다운 기호(예: #, *, -, ###, 👍 등)를 절대 사용하지 마십시오. 오직 순수한 텍스트 문자만 출력해야 합니다.
                2. '장점', '단점' 등의 소제목이나 번호 매기기, 항목 분리를 절대 하지 마십시오.
                3. 전체 분량은 반드시 2줄 내지 3줄 이내로 매우 간결해야 합니다.
                4. 줄바꿈을 사용해 전체 내용을 가독성있게 작성하십시오.
                """;
    
            return client.prompt()
                    .system(reducePrompt)
                    .user(combinedSummaries)
                    .call()
                    .content();
        }
    }