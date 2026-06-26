package com.nhnacademy.springailibrarycore.library.mcp;

import com.nhnacademy.springailibrarycore.library.dto.LibrarySearchResponse.LibraryInfo;
import com.nhnacademy.springailibrarycore.library.service.agent.LibrarySearchCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LibrarySearchTool {

    private final LibrarySearchCoordinator librarySearchCoordinator;

    @Tool(
        description = "도서관 목록 또는 특정 도서관의 상세 정보(운영시간, 주소, 연락처, 휴관일)를 조회합니다."
    )
    public String searchLibraries(
        @ToolParam(description = "검색하거나 상세 정보를 확인할 도서관 이름 (예: 강남도서관, 마포평생학습관)", required = false) String libraryName,
        @ToolParam(description = "시도 지역명 (예: 서울, 경기도, 부산광역시)", required = false) String regionName,
        @ToolParam(description = "시군구 상세지역명 (예: 강남구, 마포구, 분당구)", required = false) String dtlRegionName,
        @ToolParam(description = "조회 대상 6자리 도서관 코드 (도서관 코드를 명확히 아는 경우 직접 지정)", required = false) String libCode
    ) {
        log.info("[Tool] searchLibraries 호출: libraryName={}, regionName={}, dtlRegionName={}, libCode={}", 
                libraryName, regionName, dtlRegionName, libCode);

        try {
            // Coordinator를 호출하여 자연어 매개변수를 코드로 해석하고 검색을 조율합니다.
            List<LibraryInfo> list = librarySearchCoordinator.search(libraryName, regionName, dtlRegionName, libCode);

            if (list.isEmpty()) {
                return "검색된 도서관이 없습니다.";
            }

            // 토큰 절약 및 실시간 출력을 위해 마크다운 문자열로 가공
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("📍 **도서관 검색 결과 (총 %d개):**\n", Math.min(list.size(), 10)));
            
            // 결과를 최대 10개만 슬라이싱하여 노출
            List<LibraryInfo> resultList = list.stream().limit(10).toList();
            for (LibraryInfo info : resultList) {
                sb.append(String.format("- **%s** (도서관 코드: `%s`)\n", info.libName(), info.libCode()));
                sb.append(String.format("  * 주소: %s\n", info.address()));
                if (info.tel() != null && !info.tel().isBlank()) {
                    sb.append(String.format("  * 연락처: %s\n", info.tel()));
                }
                if (info.operatingTime() != null && !info.operatingTime().isBlank()) {
                    sb.append(String.format("  * 운영시간: %s\n", info.operatingTime()));
                }
                if (info.closed() != null && !info.closed().isBlank()) {
                    sb.append(String.format("  * 휴관일: %s\n", info.closed()));
                }
            }
            
            if (list.size() > 10) {
                sb.append(String.format("\n*...외 %d개의 도서관이 더 있습니다. 특정 도서관명을 더 자세히 질문해 주세요.*", list.size() - 10));
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("[Tool] searchLibraries 실패", e);
            return "도서관 정보를 조회하는 도중 에러가 발생했습니다.";
        }
    }
}
