package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.request.BookDetailRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.NaruBookDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 도서 상세 조회 API 호출 흐름을 조율하는 코디네이터입니다.
 *
 * <p>ISBN 필수값을 검증하고, 대출상세정보 제공 여부({@code loaninfoYN})의 기본값을 보정한 뒤
 * {@link BookDetailRequest}를 생성하여 {@link BookDetailAgent}에 조회를 위임합니다.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BookDetailCoordinator {
    private final BookDetailAgent detailAgent;

    public NaruBookDetailResponse.ResponseData getBookDetail(
            String isbn13,
            String loaninfoYN, // 대출상세정보 제공여부
            String displayInfo // 대충 정보 조회 대상
    ) {
        if (isbn13 == null || isbn13.isBlank()) {
            throw new IllegalArgumentException("ISBN은 필수입니다.");
        }

        // loaninfo가 없는 경우 default = N
        if (loaninfoYN == null || loaninfoYN.isBlank()) {
            loaninfoYN = "N";
        }
        // loaninfo 형식 검증
        loaninfoYN = loaninfoYN.toUpperCase();

        if (!loaninfoYN.equals("Y") && !loaninfoYN.equals("N")) {
            throw new IllegalArgumentException("loaninfoYN은 Y 또는 N만 입력할 수 있습니다.");
        }
        // loaninfoYN=Y인 경우 displayInfo는 gender, age, region 중 하나만 허용합니다.
        if (displayInfo != null && !displayInfo.isBlank()
                && !displayInfo.equals("gender")
                && !displayInfo.equals("age")
                && !displayInfo.equals("region")) {
            throw new IllegalArgumentException("displayInfo는 gender, age, region 중 하나여야 합니다.");
        }
        // loaninfoYN = Y 일때만 제공
        if (loaninfoYN.equals("N")) {
            displayInfo = null;
        }

        BookDetailRequest request = BookDetailRequest.builder()
                .isbn13(isbn13)
                .loaninfoYN(loaninfoYN)
                .displayInfo(displayInfo)
                .build();

        return detailAgent.getBookDetail(request);
    }
}