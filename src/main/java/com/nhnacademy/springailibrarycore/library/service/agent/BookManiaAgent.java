package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.request.ManiaRecommendationRequest;
import com.nhnacademy.springailibrarycore.library.service.LibraryNaruService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 마니아 추천도서 조회 실행 담당 에이전트
 *
 * <p>{@link ManiaRecommendationRequest}를 받아
 * {@link LibraryNaruService#getManiaRecommendations(ManiaRecommendationRequest)} 호출을 위임합니다.</p>
 * 파라미터 검증과 기본값 설정은 Coordinator에서 처리
 * 서비스 호출만 담당
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BookManiaAgent {
    private final LibraryNaruService libraryNaruService;
    public List<NaruBookInfo> getManiaRecommendations(ManiaRecommendationRequest request){
        return libraryNaruService.getManiaRecommendations(request);
    }
}
