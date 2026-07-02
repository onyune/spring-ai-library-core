package com.nhnacademy.springailibrarycore.library.service.agent;



import com.nhnacademy.springailibrarycore.library.dto.request.NewArrivalBookRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.NewArrivalBooksResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class NewArrivalBookCoordinator {


    private final LibraryCodeAgent libraryCodeAgent;
    private final NewArrivalBookAgent  newArrivalBookAgent;

    public NewArrivalBooksResponse.ResponseData search(NewArrivalBookRequest rawRequest) {
        if(rawRequest == null) {
            return null;
        }

        log.info("[NewArrivalBookCoordinator] 신착 도서 조회 시작(DTO 입력)");

        //검색 일자 해석
        String resolvedSearchDt = null;
        if(rawRequest.searchDt() != null && !rawRequest.searchDt().isBlank()) {
            resolvedSearchDt = rawRequest.searchDt() != null ? rawRequest.searchDt() : null;
        }

        // 도서관 코드 해석
        String resolvedLibCode = rawRequest.libCode();
        if ((resolvedLibCode == null || resolvedLibCode.isBlank())
                && rawRequest.libName() != null && !rawRequest.libName().isBlank()) {
            resolvedLibCode = libraryCodeAgent.getLibraryCode(rawRequest.libName());
        }

        NewArrivalBookRequest resolvedRequest = NewArrivalBookRequest.builder()
                .libCode(resolvedLibCode)
                .searchDt(resolvedSearchDt)
                .build();

        log.info("[NewArrivalBookCoordinator] 에이전트 해석 완료. NewArrivalBookRequest를 호출 합니다.");
        return newArrivalBookAgent.searchNewArrivalBook(resolvedRequest);

    }

}
