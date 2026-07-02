package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.request.MonthlyKeywordRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.MonthlyKeywordResponse;
import com.nhnacademy.springailibrarycore.library.service.LibraryNaruService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyKeywordAgent {
    private LibraryNaruService libraryNaruService;

    public List<MonthlyKeywordResponse.KeywordInfo> getMonthlyKeywords(MonthlyKeywordRequest request) {
        return libraryNaruService.getMonthlyKeywords(request);
    }
}
