 package com.nhnacademy.springailibrarycore.library.mcp;
    
    import com.nhnacademy.springailibrarycore.library.service.agent.MultipleBookLoanCoordinator;
    import java.util.Arrays;
    import java.util.List;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.ai.tool.annotation.Tool;
    import org.springframework.ai.tool.annotation.ToolParam;
    import org.springframework.stereotype.Component;
    
    @Component
    @RequiredArgsConstructor
    @Slf4j
    public class MultipleBookLoanAvailabilityTool {
    
        private final MultipleBookLoanCoordinator multipleBookLoanCoordinator;
    
        @Tool(
            description = "하나 또는 여러 권의 책에 대해 특정 도서관명 또는 특정 지역(시도/시군구) 내 대출 가능한 도서관 목록을 일괄 조회합니다."
        )
        public String checkMultipleBooksLoanAvailability(
                @ToolParam(description = "대출 상태를 확인할 도서 제목 목록 (쉼표로 구분, 예: '토비의 스프링, 클린 코드')") String bookTitles,
                @ToolParam(description = "조회 대상 특정 도서관 명칭 (예: '강남도서관', '마포평생학습관')", required = false) String libName,
                @ToolParam(description = "시도 행정구역 지역 명칭 (예: '서울', '경기도')", required = false) String regionName,
                @ToolParam(description = "시군구 상세지역 명칭 (선택, 예: '강남구', '분당구')", required = false) String dtlRegionName
        ) {
            log.info("[Tool] checkMultipleBooksLoanAvailability 호출: bookTitles={}, libName={}, regionName={}, dtlRegionName={}", 
                    bookTitles, libName, regionName, dtlRegionName);
    
            try {
                if (bookTitles == null || bookTitles.isBlank()) {
                    return "조회할 책 제목 목록이 필요합니다.";
                }
    
                List<String> titleList = Arrays.stream(bookTitles.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .toList();

                return multipleBookLoanCoordinator.checkMultipleBooksAvailability(titleList, libName, regionName, dtlRegionName);

            } catch (Exception e) {
                log.error("[Tool] checkMultipleBooksLoanAvailability 실패", e);
                return "도서 대출 정보를 조회하는 도중 에러가 발생했습니다.";
            }
        }
    }