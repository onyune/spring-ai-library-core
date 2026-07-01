package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.request.BookUsageAnalysisRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.NaruBookUsageAnalysisResponse;
import com.nhnacademy.springailibrarycore.library.service.LibraryNaruService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 도서별 이용 분석 조회 실행을 담당하는 에이전트입니다.
 *
 * <p>{@link BookUsageAnalysisRequest}를 받아
 * {@link LibraryNaruService#getBookUsageAnalysis(BookUsageAnalysisRequest)} 호출을 위임합니다.</p>
 *
 * ISBN 필수값 검증 등 요청 조율은 Coordinator에서 처리
 * 실제 서비스 호출 담당
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class BookUsageAnalysisAgent {
    private final LibraryNaruService libraryNaruService;
    public NaruBookUsageAnalysisResponse.ResponseData getBookUsageAnalysis(BookUsageAnalysisRequest request) {
        return libraryNaruService.getBookUsageAnalysis(request);
    }
}
