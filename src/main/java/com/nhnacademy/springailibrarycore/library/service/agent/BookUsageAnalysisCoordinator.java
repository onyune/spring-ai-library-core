package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.request.BookUsageAnalysisRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.NaruBookUsageAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 도서별 이용 분석 조회 API 호출 흐름을 조율하는 코디네이터입니다.
 *
 * <p>ISBN 필수값을 검증하고 {@link BookUsageAnalysisRequest}를 생성한 뒤
 * {@link BookUsageAnalysisAgent}에 조회를 위임합니다.</p>
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class BookUsageAnalysisCoordinator {
    private final BookUsageAnalysisAgent bookUsageAnalysisAgent;
    public NaruBookUsageAnalysisResponse.ResponseData getBookUsageAnalysis(String isbn13) {
        if (isbn13 == null || isbn13.isBlank()) {
            throw new IllegalArgumentException("ISBN 은 필수입니다.");
        }
        if (!isbn13.matches("\\d{10}|\\d{13}")) {
            throw new IllegalArgumentException("ISBN은 10자리 또는 13자리 숫자여야 합니다.");
        }
        BookUsageAnalysisRequest request = BookUsageAnalysisRequest.builder()
                .isbn13(isbn13)
                .build();
        return bookUsageAnalysisAgent.getBookUsageAnalysis(request);
    }
}
