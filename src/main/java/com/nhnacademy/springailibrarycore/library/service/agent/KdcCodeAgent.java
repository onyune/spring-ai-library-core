package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.common.codes.KdcCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 자연어 KDC 대분류 표현(예: 철학, 종교, 사회과학, 역사)을 API 규격 KDC 코드(0~9)로 변환하는 에이전트.
 */
@Service
@Slf4j
public class KdcCodeAgent {

    /**
     * 자연어 KDC 대분류 표현을 받아 해당하는 API KDC 코드(0~9)를 반환합니다.
     * 매핑되는 코드가 없을 경우 null을 반환합니다.
     */
    public String getKdcCode(String kdcQuery) {
        if (kdcQuery == null || kdcQuery.isBlank()) {
            return null;
        }

        List<String> codes = new ArrayList<>();
        String[] parts = kdcQuery.split("[,;및과와\\s+]+");

        for (String part : parts) {
            KdcCode code = KdcCode.fromName(part);
            if (code != null && !codes.contains(code.getCode())) {
                codes.add(code.getCode());
            }
        }

        if (codes.isEmpty()) {
            // 직접 숫자가 입력된 경우 파싱 시도 (예: "kdc 8", "8;9")
            Matcher m = Pattern.compile("[0-9]").matcher(kdcQuery.replaceAll("\\s+", ""));
            while (m.find()) {
                if (!codes.contains(m.group())) {
                    codes.add(m.group());
                }
            }
        }

        if (codes.isEmpty()) {
            log.warn("[KdcCodeAgent] KDC 분류코드를 매핑할 수 없음: {}", kdcQuery);
            return null;
        }

        String result = String.join(";", codes);
        log.info("[KdcCodeAgent] KDC 분류코드 매핑 완료: '{}' -> '{}'", kdcQuery, result);
        return result;
    }
}
