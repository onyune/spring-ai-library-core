package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.request.ManiaRecommendationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookManiaCoordinator {
    private final BookManiaAgent bookManiaAgent;

    public List<NaruBookInfo> getManiaRecommendations(String isbn13) {
        if (isbn13 == null || isbn13.isBlank()) {
            throw new IllegalArgumentException("ISBN은 필수입니다.");
        }
        // 입력 구분 ;
        String[] isbnList = isbn13.split(";");
        if (isbnList.length > 5) {
            throw new IllegalArgumentException("ISBN은 세미콜론(;)으로 구분하여 최대 5개까지 입력할 수 있습니다.");
        }
        // ISBN 10 ~ 13자리
        for (String isbn : isbnList) {
            String trimmedIsbn = isbn.trim();
            if (!trimmedIsbn.matches("\\d{10}|\\d{13}")) {
                throw new IllegalArgumentException("ISBN은 10자리 또는 13자리 숫자여야 합니다.");
            }
        }
        ManiaRecommendationRequest request = ManiaRecommendationRequest.builder()
                .isbn13(isbn13)
                .type("mania")
                .build();
        return bookManiaAgent.getManiaRecommendations(request);
    }
}
