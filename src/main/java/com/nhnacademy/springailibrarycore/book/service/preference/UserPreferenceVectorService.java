package com.nhnacademy.springailibrarycore.book.service.preference;

import com.nhnacademy.springailibrarycore.book.dto.FeedbackLikedBooksResponse;
import com.nhnacademy.springailibrarycore.book.repository.BookRepository;
import com.nhnacademy.springailibrarycore.telegram.client.TelegramFeedbackClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceVectorService {
    private final TelegramFeedbackClient telegramFeedbackClient;
    private final BookRepository bookRepository;

    /**
     * 사용자의 선호도 벡터를 캐싱하거나 조회합니다.
     * @param chatId 사용자의 채팅방 Id
     * @return 사용자의 선호도 벡터
     */
    @Cacheable(cacheNames = "userPreferenceVector", key = "#chatId")
    public float[] getUserPreferenceVector(Long chatId) {
        log.info("캐시 miss: chatId {}의 선호도 벡터를 새로 계산합니다.", chatId);
        FeedbackLikedBooksResponse response = telegramFeedbackClient.getLikedBooks(chatId);

        List<Long> likedBookIds = response.bookIds();
        if (likedBookIds == null || likedBookIds.isEmpty()) {
            return new float[0];
        }

        List<float[]> bookVectors = bookRepository.findEmbeddingByBookIds(likedBookIds);

        if (bookVectors.isEmpty()) {
            return new float[0];
        }

        return calculateAverageVector(bookVectors);
    }
    private float[] calculateAverageVector(List<float[]> bookVectors){
        int dimensions = bookVectors.get(0).length;
        float[] average = new float[dimensions];

        for(float[] vector : bookVectors){
            for(int i = 0 ; i<dimensions;i++){
                average[i] += vector[i];
            }
        }

        for(int i =0; i<dimensions;i++){
            average[i] /= bookVectors.size();
        }
        return average;
    }
}
