package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.common.codes.AgeRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 자연어 연령대 표현(예: 20대, 청소년, 초등학생)을 도서관 정보나루 API 규격 연령대 코드로 변환하는 에이전트.
 */
@Service
@Slf4j
public class AgeAgent {

    /**
     * 자연어 연령대 표현을 받아 해당하는 API 연령대 코드(0, 6, 8, 14, 20, 30, 40, 50, 60)를 반환.
     * 매핑되는 코드가 없을 경우 null을 반환.
     */
    public Integer getAgeCode(String ageName) {
        if (ageName == null || ageName.isBlank()) {
            return null;
        }

        AgeRange ageRange = AgeRange.fromName(ageName);
        if (ageRange != null) {
            log.info("[AgeAgent] 연령대 매핑 성공: '{}' -> '{}' (코드: {})", ageName, ageRange.getName(), ageRange.getCode());
            return ageRange.getCode();
        }

        log.warn("[AgeAgent] 매핑되는 연령대 코드를 찾을 수 없습니다: '{}'", ageName);
        return null;
    }
}
