package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.request.PopularBooksSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 인기 도서 검색의 다양한 자연어 파라미터를 개별 에이전트를 통해 코드로 변환하고,
 * 최종적으로 PopularBookSearchAgent를 통해 인기 도서 목록 조회를 조율(Coordinate)하는 코디네이터.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PopularBookSearchCoordinator {

    private final RegionCodeAgent regionCodeAgent;
    private final DetailRegionCodeAgent detailRegionCodeAgent;
    private final AgeAgent ageAgent;
    private final GenderCodeAgent genderCodeAgent;
    private final BookDivisionAgent bookDivisionAgent;
    private final IsbnAddCodeAgent isbnAddCodeAgent;
    private final KdcCodeAgent kdcCodeAgent;
    private final DetailKdcCodeAgent detailKdcCodeAgent;
    private final LoanDateRangeAgent loanDateRangeAgent;
    private final PopularBookSearchAgent popularBookSearchAgent;

    /**
     * PopularBooksSearchRequest 요청 DTO를 받아 자연어 파라미터가 있는 경우 코드로 해석/매핑하여 
     * 최종 조회를 수행합니다.
     */
    public List<NaruBookInfo> search(PopularBooksSearchRequest rawRequest) {
        if (rawRequest == null) {
            return List.of();
        }

        log.info("[PopularBookSearchCoordinator] 인기 도서 검색 조율 시작 (DTO 입력)");

        // 대출 기간 해석 (시작일자 / 종료일자)
        String resolvedStart = rawRequest.startDt();
        String resolvedEnd = rawRequest.endDt();
        if (rawRequest.startDt() != null && !rawRequest.startDt().isBlank()) {
            String[] range = loanDateRangeAgent.getDateRange(rawRequest.startDt());
            if (range[0] != null) {
                resolvedStart = range[0];
                resolvedEnd = range[1];
            }
        }

        // 지역 및 세부지역 해석
        Integer regionCode = null;
        String resolvedRegion = null;
        if (rawRequest.region() != null && !rawRequest.region().isBlank()) {
            regionCode = regionCodeAgent.getRegionCode(rawRequest.region());
            resolvedRegion = regionCode != null ? String.valueOf(regionCode) : null;
        }

        String resolvedDtlRegion = null;
        if (rawRequest.dtlRegion() != null && !rawRequest.dtlRegion().isBlank()) {
            Integer dtlRegionCode = detailRegionCodeAgent.getDetailRegionCode(rawRequest.dtlRegion(), regionCode);
            resolvedDtlRegion = dtlRegionCode != null ? String.valueOf(dtlRegionCode) : null;
        }

        // 연령대 및 성별 해석
        String resolvedAge = null;
        if (rawRequest.age() != null && !rawRequest.age().isBlank()) {
            Integer ageCode = ageAgent.getAgeCode(rawRequest.age());
            resolvedAge = ageCode != null ? String.valueOf(ageCode) : null;
        }

        String resolvedGender = null;
        if (rawRequest.gender() != null && !rawRequest.gender().isBlank()) {
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

        String resolvedKdc = null;
        if (rawRequest.kdc() != null && !rawRequest.kdc().isBlank()) {
            resolvedKdc = kdcCodeAgent.getKdcCode(rawRequest.kdc());
        }

        String resolvedDtlKdc = null;
        if (rawRequest.dtlKdc() != null && !rawRequest.dtlKdc().isBlank()) {
            resolvedDtlKdc = detailKdcCodeAgent.getDtlKdcCode(rawRequest.dtlKdc());
        }

        // 해석 완료된 신규 요청 DTO 생성
        PopularBooksSearchRequest resolvedRequest = PopularBooksSearchRequest.builder()
                .startDt(resolvedStart)
                .endDt(resolvedEnd)
                .gender(resolvedGender)
                .fromAge(rawRequest.fromAge())
                .toAge(rawRequest.toAge())
                .age(resolvedAge)
                .region(resolvedRegion)
                .dtlRegion(resolvedDtlRegion)
                .bookDvsn(resolvedBookDvsn)
                .addCode(resolvedAddCode)
                .kdc(resolvedKdc)
                .dtlKdc(resolvedDtlKdc)
                .pageNo(rawRequest.pageNo())
                .pageSize(rawRequest.pageSize())
                .build();

        log.info("[PopularBookSearchCoordinator] 에이전트 해석 완료. PopularBookSearchAgent 호출합니다.");
        return popularBookSearchAgent.searchPopularBook(resolvedRequest);
    }
}
