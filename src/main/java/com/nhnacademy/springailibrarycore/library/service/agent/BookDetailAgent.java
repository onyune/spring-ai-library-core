package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.request.BookDetailRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.NaruBookDetailResponse;
import com.nhnacademy.springailibrarycore.library.service.LibraryNaruService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 도서 상세 조회 실행을 담당하는 에이전트입니다.
 *
 * <p>{@link BookDetailRequest}를 받아
 * {@link LibraryNaruService#getBookDetail(BookDetailRequest)} 호출을 위임합니다.</p>
 *
 * ISBN 필수값 검증, 대출상세정보 옵션 보정 등 요청 조율은 Coordinator에서 처리하고,
 * 서비스 호출 담당
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class BookDetailAgent {
    private final LibraryNaruService libraryNaruService;

    public NaruBookDetailResponse.ResponseData getBookDetail(BookDetailRequest request) {
        return libraryNaruService.getBookDetail(request);
    }
}
