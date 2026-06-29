package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.common.codes.DetailKdcCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 자연어 KDC 세부 분류 표현(예: 심리학, 경제학, 한국문학)을 API 규격 KDC 세부분류 코드(2~3자리 숫자)로 변환하는 에이전트.
 */
@Service
@Slf4j
public class DetailKdcCodeAgent {

    /**
     * 자연어 KDC 세부 분류 표현을 받아 해당하는 API 세부분류 코드를 반환합니다.
     * 매핑되는 코드가 없을 경우 null을 반환합니다.
     */
    public String getDtlKdcCode(String dtlKdcQuery) {
        if (dtlKdcQuery == null || dtlKdcQuery.isBlank()) {
            return null;
        }

        List<String> codes = new ArrayList<>();
        String[] parts = dtlKdcQuery.split("[,;및과와\\s+]+");

        for (String part : parts) {
            DetailKdcCode code = DetailKdcCode.fromName(part);
            if (code != null && !codes.contains(code.getCode())) {
                codes.add(code.getCode());
            }
        }

        if (codes.isEmpty()) {
            // 직접 숫자가 입력된 경우 파싱 시도 (예: "811", "81;82")
            Matcher m = Pattern.compile("[0-9]+").matcher(dtlKdcQuery.replaceAll("\\s+", ""));
            while (m.find()) {
                if (!codes.contains(m.group())) {
                    codes.add(m.group());
                }
            }
        }

        if (codes.isEmpty()) {
            log.warn("[DetailKdcCodeAgent] 세부 KDC 코드를 매핑할 수 없음: {}", dtlKdcQuery);
            return null;
        }

        String result = String.join(";", codes);
        log.info("[DetailKdcCodeAgent] 세부 KDC 코드 매핑 완료: '{}' -> '{}'", dtlKdcQuery, result);
        return result;
    }
}
