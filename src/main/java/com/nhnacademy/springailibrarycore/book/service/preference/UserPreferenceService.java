package com.nhnacademy.springailibrarycore.book.service.preference;

import com.nhnacademy.springailibrarycore.book.dto.FeedbackLikedBooksResponse;
import com.nhnacademy.springailibrarycore.telegram.client.TelegramFeedbackClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceService {
    private final TelegramFeedbackClient telegramFeedbackClient;

    @Cacheable(cacheNames = "likedBookIds", key = "#chatId")
    public List<Long> getRecentLikedBookIds(Long chatId) {
        // 캐시에 없을 때만 이 로그가 찍히고 API가 호출됩니다.
        log.info("캐시 미스! Telegram API로부터 좋아요 목록을 조회합니다. chatId: {}", chatId);

        FeedbackLikedBooksResponse response = telegramFeedbackClient.getLikedBooks(chatId);

        // 도서 ID 리스트 20개 추출해서 반환
        return response.bookIds();
    }
}
