package com.nhnacademy.springailibrarycore.library.mcp;

import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.common.NaruWrapper;
import com.nhnacademy.springailibrarycore.library.dto.request.NewArrivalBookRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.NewArrivalBooksResponse;
import com.nhnacademy.springailibrarycore.library.service.agent.NewArrivalBookCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewArrivalBookTool {
    private final NewArrivalBookCoordinator newArrivalBookCoordinator;



    @Tool(description = "특정 도서관의 신착 도서 목록을 조회합니다. 도서관명 또는 도서관 코드를 받아 신착 도서명, 저자, 출판사, 출판년도, ISBN을 제공합니다.")
    public String newArrivalBookRequest(
            @ToolParam(description = "조회 대상 6자리 도서관 코드 (도서관 코드를 명확히 아는 경우 직접 지정)", required = false) String libCode,
            @ToolParam(description = "검색하거나 상세 정보를 확인할 도서관 이름 (예: 강남도서관, 마포평생학습관)", required = false) String libName,
            @ToolParam(description = "검색 일자 (예: '이번 달', '최근 한 달', '2026년 3월', 'yyyy-MM')", required = false) String searchDate

    ) {
        log.info("[Tool] NewArrivalBook 호출 : libCode={}, libName={}, searchDate={}", libCode, libName, searchDate);

        try{
            NewArrivalBookRequest request = NewArrivalBookRequest.builder()
                    .libCode(libCode)
                    .libName(libName)
                    .searchDt(searchDate)
                    .build();


            NewArrivalBooksResponse.ResponseData response = newArrivalBookCoordinator.search(request);
            List<NaruBookInfo> list = response != null && response.docs() != null
                    ? response.docs().stream()
                    .map(NaruWrapper::getContent)
                    .toList()
                    : List.of();


            if(list.isEmpty()){
                return "조건에 만족하는 신착 도서가 없습니다.";
            }

            StringBuilder result = getNewResult(response,list);

            return result.toString();

        }catch (Exception e){
            log.error("[Tool] NewArrivalBook 실패", e);
            return "신착 도서 목록을 조회하는 도중 예외가 발생했습니다: " + e.getMessage();
        }
    }

    @NotNull
    private StringBuilder getNewResult(NewArrivalBooksResponse.ResponseData response, List<NaruBookInfo> list){
        StringBuilder sb = new StringBuilder();

        sb.append("📚 **신착 도서 목록 검색 결과:**\n\n");

        if (response != null && response.libNm() != null && !response.libNm().isBlank()) {
            sb.append("도서관 이름: ").append(response.libNm()).append("\n\n");
        }

        for(NaruBookInfo book : list){

            sb.append("신착 도서 정보입니다.\n");
            sb.append(String.format("- 도서명: %s%n", book.bookname()));
            sb.append(String.format("- 저자: %s%n", book.authors()));
            sb.append(String.format("- 출판사: %s%n", book.publisher()));
            sb.append(String.format("- 출판년도: %s%n", book.publicationYear()));
            sb.append(String.format("- ISBN: %s%n", book.isbn13()));
            sb.append("\n");
        }

        return sb;

    }
}
