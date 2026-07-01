package com.nhnacademy.springailibrarycore.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatClientConfig {

    @Bean
    @Primary
    public ChatClient.Builder ollamaChatClientBuilder(@Qualifier("ollamaChatModel")ChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor());
    }

    @Bean
    public ChatClient.Builder geminiChatClientBuilder(@Qualifier("googleGenAiChatModel") ChatModel geminiChatModel) {
        return ChatClient.builder(geminiChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor());
    }
    @Bean
    public ChatClient.Builder geminiJsonChatClientBuilder(
            @Qualifier("googleGenAiChatModel") ChatModel geminiChatModel) {

        // 기존 geminiChatModel을 그대로 쓰되, defaultOptions로 모델만 오버라이드
        GoogleGenAiChatOptions liteOptions = GoogleGenAiChatOptions.builder()
                .model("gemini-2.0-flash")
                .build();

        return ChatClient.builder(geminiChatModel)
                .defaultOptions(liteOptions)
                .defaultAdvisors(new SimpleLoggerAdvisor());
    }
}
