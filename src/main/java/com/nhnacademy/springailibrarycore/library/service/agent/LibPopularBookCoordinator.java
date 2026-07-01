package com.nhnacademy.springailibrarycore.library.service.agent;


import com.nhnacademy.springailibrarycore.library.dto.request.LibPopularBooksRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.LibPopularBooksResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class LibPopularBookCoordinator {


    private final LibraryCodeAgent libraryCodeAgent;
    private final RegionCodeAgent regionCodeAgent;
    private final DetailRegionCodeAgent detailRegionCodeAgent;
    private final GenderCodeAgent genderCodeAgent;
    private final IsbnAddCodeAgent isbnAddCodeAgent;
    private final KdcCodeAgent kdcCodeAgent;
    private final DetailKdcCodeAgent detailKdcCodeAgent;
    private final BookDivisionAgent bookDivisionAgent;
    private final LibPopularBookAgent libPopularBookAgent;
    private final LoanDateRangeAgent loanDateRangeAgent;
    private final AgeAgent ageAgent;

    public LibPopularBooksResponse.ResponseData search(LibPopularBooksRequest rawRequest) {
        if (rawRequest == null) {
            return null;
        }

        log.info("[LipPopularBookCoordinator] 도서관 / 지역별 인기 대출 도서 조회 시작(DTO 입력)");


        // 대출 기간 해석 ( 시작 일자 / 종료 일자)
        String resolvedStart = rawRequest.startDt();
        String resolvedEnd = rawRequest.endDt();
        if(rawRequest.startDt() != null && !rawRequest.startDt().isBlank()) {
            String[] range = loanDateRangeAgent.getDateRange(rawRequest.startDt());
            if(range[0] != null){
                resolvedStart = range[0];
                resolvedEnd = range[1];
            }
        }

        // 도서관 코드 해석
        String resolvedLibCode = rawRequest.libCode();
        if ((resolvedLibCode == null || resolvedLibCode.isBlank())
                && rawRequest.libName() != null && !rawRequest.libName().isBlank()) {
            resolvedLibCode = libraryCodeAgent.getLibraryCode(rawRequest.libName());
        }

        // 지역 및 세부 지역 해석
        Integer regionCode = null;
        String resolvedRegion = null;
        if(rawRequest.region() != null && !rawRequest.region().isBlank()) {
            regionCode = regionCodeAgent.getRegionCode(rawRequest.region());
            resolvedRegion = regionCode != null ? String.valueOf(regionCode) : null;
        }

        String resolvedDetailRegion = null;
        if(rawRequest.dtlRegion() != null && !rawRequest.dtlRegion().isBlank()) {
            Integer detailRegionCode = detailRegionCodeAgent.getDetailRegionCode(rawRequest.dtlRegion(), regionCode);
            resolvedDetailRegion = detailRegionCode != null ? String.valueOf(detailRegionCode) : null;
        }

        // 연령대 및 성벌
        String resolvedAge = null;
        if(rawRequest.age() != null && !rawRequest.age().isBlank()) {
            Integer ageCode = ageAgent.getAgeCode(rawRequest.age());
            resolvedAge = ageCode != null ? String.valueOf(ageCode) : null;

        }


        String resolvedGender = null;
        if(rawRequest.gender() != null && !rawRequest.gender().isBlank()) {
            resolvedGender = genderCodeAgent.getGenderCode(rawRequest.gender());
        }

        // 도서 구분, 부가기호, KDC 대분류/세부분류 해석
        String resolvedBookDvsn = null;
        if (rawRequest.bookDvsn() != null && !rawRequest.bookDvsn().isBlank()) {
            resolvedBookDvsn = bookDivisionAgent.getBookDivision(rawRequest.bookDvsn());
        }

        String resolvedAddCode = null;
        if (rawRequest.addCode() != null && !rawRequest.addCode().isBlank()) {
            resolvedAddCode = isbnAddCodeAgent.getAddCode(rawRequest.addCode());
        }

        String resolvedKdcCode = null;
        if(rawRequest.kdc() != null && !rawRequest.kdc().isBlank()) {
            resolvedKdcCode = kdcCodeAgent.getKdcCode(rawRequest.kdc());
        }

        String resolvedDetailKdcCode = null;
        if(rawRequest.dtlKdc() != null && !rawRequest.dtlKdc().isBlank()) {
            resolvedDetailKdcCode = detailKdcCodeAgent.getDtlKdcCode(rawRequest.dtlKdc());
        }

        if ((resolvedLibCode == null || resolvedLibCode.isBlank())
                && (resolvedRegion == null || resolvedRegion.isBlank())
                && (resolvedDetailRegion == null || resolvedDetailRegion.isBlank())) {
            throw new IllegalArgumentException("도서관명, 도서관 코드, 지역, 세부지역 중 하나는 필요합니다.");
        }

        //해석 완료 된 신규 요청 DTO 생성
        LibPopularBooksRequest resolvedRequest = LibPopularBooksRequest.builder()
                .libCode(resolvedLibCode)
                .region(resolvedRegion)
                .dtlRegion(resolvedDetailRegion)
                .startDt(resolvedStart)
                .endDt(resolvedEnd)
                .gender(resolvedGender)
                .fromAge(rawRequest.fromAge())
                .toAge(rawRequest.toAge())
                .age(resolvedAge)
                .addCode(resolvedAddCode)
                .kdc(resolvedKdcCode)
                .dtlKdc(resolvedDetailKdcCode)
                .bookDvsn(resolvedBookDvsn)
                .pageNo(rawRequest.pageNo())
                .pageSize(rawRequest.pageSize())
                .build();


        log.info("[LipPopularBookCoordinator] 에이전트 해석 완료. LibPopularBookAgent를 호출 합니다.");
        return libPopularBookAgent.searchLibPopularBook(resolvedRequest);

    }


}
