package com.nhnacademy.springailibrarycore.book.service.preference;

import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResult;
import com.nhnacademy.springailibrarycore.book.exception.NullPointerChatIdException;
import com.nhnacademy.springailibrarycore.book.repository.BookRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonalizedRecommendationService {
    private final UserPreferenceVectorService userPreferenceVectorService;
    private final BookRepository bookRepository;

    public BookSearchResult getPersonalizedRecommendationBooks(Long chatId){
        if(chatId == null){
            throw new NullPointerChatIdException();
        }
        // 사용자 선호도 벡터 조회 (최신 20권 평균 벡터)
        float[] vector = userPreferenceVectorService.getUserPreferenceVector(chatId).getVector();
        log.info("[PersonalizedRecommendationService] 사용자 선호 벡터: {}", vector);
        // 2. Threshold 없이 가장 유사한 3권 조회 (평균 벡터 특성상 유사도가 낮음)
        List<BookSearchResponse> list = bookRepository.findPersonalizedBooks(vector, 3);
        log.info("[PersonalizedRecommendationService] DB에서 조회된 도서 개수: {}", list.size());
        if(!list.isEmpty()){
            log.info("[PersonalizedRecommendationService] 첫번째 도서: {}", list.get(0).getTitle());
        }
        
        // 3. 결과를 Page 타입으로 매핑 후 반환
        Pageable pageable = PageRequest.of(0, 3);
        Page<BookSearchResponse> page = new PageImpl<>(
                list, 
                pageable, 
                list.size()
        );

        return new BookSearchResult(page);
    }
}
