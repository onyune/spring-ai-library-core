package com.nhnacademy.springailibrarycore.library.mcp;

import com.nhnacademy.springailibrarycore.library.dto.response.MonthlyKeywordResponse;
import com.nhnacademy.springailibrarycore.library.service.agent.MonthlyKeywordCoordinator;
import com.nhnacademy.springailibrarycore.telegram.tool.ToolResultContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyKeywordTool {

    private final MonthlyKeywordCoordinator monthlyKeywordCoordinator;
    private final ToolResultContext toolResultContext;

    @Tool(description = "검색월 기준 전체 도서 대출 데이터에서 추출한 이달의 핵심 키워드 목록을 조회합니다.")
    public String getMonthlyKeywords(
            @ToolParam(
                    description = "조회할 검색월입니다. yyyy-MM 형식으로 입력합니다. 선택값이며, 비워두면 API에서 제공하는 최신 집계월 기준 키워드를 조회합니다.",
                    required = false
            )
            String month
      ) {
        log.info("[Tool] getMonthlyKeywords 호출: month={}", month);

        try {
            List<MonthlyKeywordResponse.KeywordInfo> keywords =
                    monthlyKeywordCoordinator.getMonthlyKeywords(month);

            if (keywords == null || keywords.isEmpty()) {
                return "FAIL: 이달의 키워드 정보를 찾을 수 없습니다.";
            }

            StringBuilder sb = new StringBuilder();

            if (month != null && !month.isBlank()) {
                sb.append(String.format("%s 기준 이달의 키워드입니다.%n", month));
            } else {
                sb.append("최신 집계 기준 이달의 키워드입니다.\n");
            }

            keywords.stream().limit(20).forEach(keyword -> {
                sb.append(String.format("- %s (가중치: %.2f)%n",
                        keyword.word(), keyword.weight()));
            });

            String report = sb.toString();

            // 실제 데이터를 RequestScope 컨텍스트에 임시 저장
            toolResultContext.addResult(report);

            return "SUCCESS: 이달의 핵심 키워드 조회가 완료되었습니다.";

        } catch (Exception e) {
            log.error("[Tool] getMonthlyKeywords 실패", e);
            return "FAIL: 이달의 키워드를 조회하는 중 오류가 발생했습니다.";
        }
    }
}
