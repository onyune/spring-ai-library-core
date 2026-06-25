package com.nhnacademy.springailibrarycore.library.dto.request;

import lombok.Builder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 마니아를 위한 추천도서 조회 API (recommandList) 호출을 위한 요청 DTO 클래스
 * 빅데이터 플랫폼을 통해 특정 도서들과 함께 읽힐 확률이 높은 도서 목록을 추천합니다.
 *
 * @param isbn13 추천의 기준이 되는 도서의 13자리 ISBN 번호
 *               세미콜론(;)을 구분자로 사용하여 최대 5개의 ISBN을 동시에 입력할 수 있으며, 이 경우 각 도서의 연관 추천 결과를 조합하여 반환
 *               (예: "9788983922571;9788983921475")
 * @param type   추천 분석 유형 코드입니다. 마니아를 위한 추천도서의 경우 기본적으로 "mania" 상수를 사용 (기본값: "mania")
 */
@Builder
public record ManiaRecommendationRequest(
        String isbn13,
        String type // 기본값 "mania"
) {
    public ManiaRecommendationRequest {
        if (type == null || type.isBlank()) {
            type = "mania";
        }
    }

    public MultiValueMap<String, String> toQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        if (isbn13 != null && !isbn13.isBlank()) params.add("isbn13", isbn13);
        if (type != null && !type.isBlank()) params.add("type", type);

        return params;
    }
}
