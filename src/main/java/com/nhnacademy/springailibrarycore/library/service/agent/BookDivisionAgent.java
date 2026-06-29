package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.common.codes.BookDivision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 자연어 도서 구분(예: 큰글씨도서, 국외도서)을 도서관 정보나루 API 규격 구분 코드(big, oversea)로 변환하는 에이전트.
 */
@Service
@Slf4j
public class BookDivisionAgent {

    /**
     * 자연어 도서 구분을 받아 해당하는 API 구분 코드(big: 큰글씨도서, oversea: 국외도서)를 반환합니다.
     * 매핑되는 코드가 없을 경우 null을 반환합니다.
     */
    public String getBookDivision(String divisionQuery) {
        if (divisionQuery == null || divisionQuery.isBlank()) {
            return null;
        }

        BookDivision div = BookDivision.fromName(divisionQuery);
        if (div != null) {
            log.info("[BookDivisionAgent] 도서 구분 매핑 완료: '{}' -> '{}'", divisionQuery, div.getCode());
            return div.getCode();
        }

        log.warn("[BookDivisionAgent] 도서 구분을 매핑할 수 없음: {}", divisionQuery);
        return null;
    }
}
