package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.common.codes.IsbnAddCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 자연어 ISBN 부가기호 표현(예: 아동도서, 실용도서, 교양도서)을 API 규격 부가기호 코드(첫째 자리 숫자)로 변환하는 에이전트.
 */
@Service
@Slf4j
public class IsbnAddCodeAgent {

    /**
     * 자연어 ISBN 부가기호 표현을 받아 해당하는 API 부가기호 코드(0~8 등)를 반환합니다.
     * 매핑되는 코드가 없을 경우 null을 반환합니다.
     */
    public String getAddCode(String addCodeQuery) {
        if (addCodeQuery == null || addCodeQuery.isBlank()) {
            return null;
        }

        List<String> codes = new ArrayList<>();
        String[] parts = addCodeQuery.split("[,;및과와\\s+]+");

        for (String part : parts) {
            IsbnAddCode code = IsbnAddCode.fromName(part);
            if (code != null && !codes.contains(code.getCode())) {
                codes.add(code.getCode());
            }
        }

        if (codes.isEmpty()) {
            // 직접 숫자가 입력된 경우 파싱 시도 (예: "부가기호 5번", "0;1")
            Matcher m = Pattern.compile("[0-9]").matcher(addCodeQuery.replaceAll("\\s+", ""));
            while (m.find()) {
                if (!codes.contains(m.group())) {
                    codes.add(m.group());
                }
            }
        }

        if (codes.isEmpty()) {
            log.warn("[IsbnAddCodeAgent] 부가기호를 매핑할 수 없음: {}", addCodeQuery);
            return null;
        }

        String result = String.join(";", codes);
        log.info("[IsbnAddCodeAgent] 부가기호 매핑 완료: '{}' -> '{}'", addCodeQuery, result);
        return result;
    }
}
