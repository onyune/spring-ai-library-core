package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.common.codes.GenderCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 자연어 성별 표현(예: 남성, 여성, 남성과 여성)을 도서관 정보나루 API 규격 성별 코드(0: 남성, 1: 여성)로 변환하는 에이전트.
 */
@Service
@Slf4j
public class GenderCodeAgent {

    /**
     * 자연어 성별 표현을 받아 해당하는 API 성별 코드(0, 1, 또는 다중 선택시 0;1)를 반환합니다.
     * 매핑되는 코드가 없을 경우 null을 반환합니다.
     */
    public String getGenderCode(String genderQuery) {
        if (genderQuery == null || genderQuery.isBlank()) {
            return null;
        }

        List<String> codes = new ArrayList<>();
        String[] parts = genderQuery.split("[,;및과와\\s+]+");

        for (String part : parts) {
            GenderCode gender = GenderCode.fromName(part);
            if (gender != null && !codes.contains(gender.getCode())) {
                codes.add(gender.getCode());
            }
        }

        if (codes.isEmpty()) {
            log.warn("[GenderCodeAgent] 성별 코드를 매핑할 수 없음: {}", genderQuery);
            return null;
        }

        String result = String.join(";", codes);
        log.info("[GenderCodeAgent] 성별 코드 매핑 완료: '{}' -> '{}'", genderQuery, result);
        return result;
    }
}
