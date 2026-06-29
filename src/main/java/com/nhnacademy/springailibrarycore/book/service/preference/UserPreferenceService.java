package com.nhnacademy.springailibrarycore.book.service.preference;

import com.nhnacademy.springailibrarycore.book.repository.BookRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceService {
    private final BookRepository bookRepository;
    private final UserPreferenceVectorService userPreferenceVectorService;

    /**
     * 사용자의 선호도와 하이브리드 검색한 책과의 코사인 유사도를 계산
     * @param candidateBookIds 하이브리드 검색한 책 아이디
     * @param chatId 캐싱된 데이터를 가져오기 위한 사용자 식별용 아이디
     * @return 책 아이디 당 유사도
     */
    public Map<Long, Double> getPersonalizationScores(List<Long> candidateBookIds, Long chatId) {
        float[] userVector = userPreferenceVectorService.getUserPreferenceVector(chatId);
        if (userVector == null || userVector.length == 0) {
            return java.util.Collections.emptyMap();
        }

        // DB에서 candidateBookIds에 해당하는 도서 ID와 임베딩(float[])을 Map 형태로 가져옴
        Map<Long, float[]> bookVectorMap = bookRepository.findEmbeddingMapByBookIds(candidateBookIds);

        Map<Long, Double> scores = new HashMap<>();
        for (Map.Entry<Long, float[]> entry : bookVectorMap.entrySet()) {
            Long bookId = entry.getKey();
            float[] bookVector = entry.getValue();

            if (bookVector != null && bookVector.length == userVector.length) {
                double similarity = calculateCosineSimilarity(userVector, bookVector);
                scores.put(bookId, similarity);
            }
        }
        return scores;
    }

    private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
