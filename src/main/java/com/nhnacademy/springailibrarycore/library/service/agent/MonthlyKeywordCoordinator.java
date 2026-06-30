package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.request.MonthlyKeywordRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.MonthlyKeywordResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 이달의 키워드 조회 API 호출 흐름을 조율하는 코디네이터입니다.
 *
 * <p>검색월({@code month}) 형식을 검증하고 {@link MonthlyKeywordRequest}를 생성한 뒤
 * {@link MonthlyKeywordAgent}에 조회를 위임합니다.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MonthlyKeywordCoordinator {

    private final MonthlyKeywordAgent monthlyKeywordAgent;
    // 입력 형태 yyyy-MM 형식
    public List<MonthlyKeywordResponse.KeywordInfo> getMonthlyKeywords(String month) {
        if (month != null && !month.isBlank() && !month.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("검색월은 yyyy-MM 형식이어야 합니다.");
        }

        MonthlyKeywordRequest request = MonthlyKeywordRequest.builder()
                .month(month)
                .build();

        return monthlyKeywordAgent.getMonthlyKeywords(request);
    }
}