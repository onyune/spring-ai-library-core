package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.LibrarySearchResponse.LibraryInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 여러 도서관 검색 관련 에이전트들의 실행 순서 및 매개변수를 조율(Coordinate)하는 코디네이터.
 * 데이터베이스 조회, 캐시 갱신, OpenAPI 통신 등의 세부 비즈니스 로직은
 * 하위 데이터 에이전트(LibrarySearchAgent)가 전담하며, 이 클래스는 파라미터 판단 및 에이전트 실행 조율만 수행합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LibrarySearchCoordinator {

    private final RegionCodeAgent regionCodeAgent;
    private final DetailRegionCodeAgent detailRegionCodeAgent;
    private final LibrarySearchAgent librarySearchAgent;

    /**
     * 자연어 매개변수들을 받아서 지역/시군구/도서관 코드를 변환하고,
     * 상황에 맞는 최적의 검색을 LibrarySearchAgent에 위임하여 도서관 상세 목록을 반환합니다.
     */
    public List<LibraryInfo> search(String libraryName, String regionName, String dtlRegionName, String libCode) {
        log.info("[Coordinator] search 시작: libraryName={}, regionName={}, dtlRegionName={}, libCode={}",
                libraryName, regionName, dtlRegionName, libCode);

        // Case 1: 도서관 코드가 명시적으로 주어졌을 때 -> 지역 해석 없이 단일 조회 즉시 위임
        if (libCode != null && !libCode.isBlank()) {
            return librarySearchAgent.search(libCode, null, null, null);
        }

        // Case 2: 도서관 명칭이 주어졌을 때 -> 코드로 매핑하여 단일 조회 우선 위임
        if (libraryName != null && !libraryName.isBlank()) {
            String targetLibCode = librarySearchAgent.getLibraryCode(libraryName);
            if (targetLibCode != null) {
                return librarySearchAgent.search(targetLibCode, null, null, null);
            }
        }

        // Case 3: 특정 도서관 매칭이 불가능하거나 지역(시도/시군구) 위주의 다중 도서관 조회일 때
        Integer regionCode = null;
        if (regionName != null && !regionName.isBlank()) {
            regionCode = regionCodeAgent.getRegionCode(regionName);
        }

        Integer dtlRegionCode = null;
        if (dtlRegionName != null && !dtlRegionName.isBlank()) {
            dtlRegionCode = detailRegionCodeAgent.getDetailRegionCode(dtlRegionName, regionCode);
        }

        // 다중 도서관 지역 필터링 검색 위임
        return librarySearchAgent.search(null, regionCode, dtlRegionCode, libraryName);
    }
}
