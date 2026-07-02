package com.nhnacademy.springailibrarycore.book.dto;

import java.util.List;

public record FeedbackLikedBooksResponse(Long chatId, List<Long> bookIds) {
}
