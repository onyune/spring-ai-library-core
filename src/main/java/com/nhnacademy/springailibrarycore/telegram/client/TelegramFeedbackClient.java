package com.nhnacademy.springailibrarycore.telegram.client;

import com.nhnacademy.springailibrarycore.book.dto.FeedbackLikedBooksResponse;
import org.springframework.stereotype.Component;

@Component
public class TelegramFeedbackClient {

    public FeedbackLikedBooksResponse getLikedBooks(Long chatId){
        //TODO: telegram 통신 소통
    }
}
