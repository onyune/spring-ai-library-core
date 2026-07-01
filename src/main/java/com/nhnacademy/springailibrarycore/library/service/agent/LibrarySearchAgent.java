package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.LibrarySearchResponse.LibraryInfo;
import com.nhnacademy.springailibrarycore.library.dto.request.LibrarySearchRequest;
import com.nhnacademy.springailibrarycore.library.service.LibraryNaruService;
import com.nhnacademy.springailibrarycore.library.dto.common.codes.Region;
import com.nhnacademy.springailibrarycore.library.dto.common.codes.DetailRegion;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 도서관 상세 정보를 메모리에 캐시하고,
 * 캐시 갱신(refreshCache), 상세 정보 룩업, 시도/시군구/명칭 인메모리 필터링 및 OpenAPI 실시간 폴백 호출까지
 * 데이터 접근 및 탐색 전반을 전담하는 데이터 에이전트.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LibrarySearchAgent {

    private final LibraryNaruService libraryNaruService;
    private final LibraryCodeAgent libraryCodeAgent;

    // 도서관 코드를 키로 LibraryInfo 전체 정보 매핑
    private final Map<String, LibraryInfo> libraryInfoMap = new ConcurrentHashMap<>();

    // 전체 도서관 리스트 (지역 필터링 및 전체 조회용)
    private final List<LibraryInfo> libraryList = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * 전체 도서관 정보를 오픈 API로부터 수집하여 캐시를 채우고,
     * LibraryCodeAgent에 명칭-코드 매핑을 동적 등록합니다.
     */
    public synchronized void refreshCache() {
        try {
            log.info("[LibrarySearchAgent] 전국 도서관 정보 캐시 갱신 시작...");

            List<LibraryInfo> tempLibraryList = new java.util.ArrayList<>();

            Map<String, LibraryInfo> tempLibraryInfoMap = new java.util.HashMap<>();

            int pageNo = 1;
            int pageSize = 500; // API 부하 및 크기 제한을 고려해 500개씩 조회

            while (true) {
                if (pageNo > 50) {
                    log.warn("[LibrarySearchAgent] 수집된 페이지 수가 50개를 초과하여 무한 루프 방지를 위해 작업을 중단합니다.");
                    break;
                }

                LibrarySearchRequest request = LibrarySearchRequest.builder()
                        .pageNo(pageNo)
                        .pageSize(pageSize)
                        .build();

                List<LibraryInfo> info = libraryNaruService.getLibraries(request);
                if (info == null || info.isEmpty()) {
                    break;
                }

                // API 에러 응답 방어 (예: dummy key 입력 등으로 유효하지 않은 응답이 오면 탈출)
                if (info.get(0).libCode() == null || info.get(0).libCode().isBlank()) {
                    log.warn("[LibrarySearchAgent] 수집된 데이터의 도서관 코드가 올바르지 않습니다(에러 응답 의심). 작업을 중단합니다.");
                    break;
                }

                tempLibraryList.addAll(info);

                for (LibraryInfo lib : info) {
                    if (lib.libCode() != null) {
                        tempLibraryInfoMap.put(lib.libCode().trim(), lib);
                    }
                }

                log.info("[LibrarySearchAgent] {}페이지 {}건 수집 완료 (누적: {}건)", 
                        pageNo, info.size(), tempLibraryList.size());

                pageNo++;
            }

            if (!tempLibraryList.isEmpty()) {
                // 상세 정보 인메모리 캐시 갱신
                libraryList.clear();
                libraryList.addAll(tempLibraryList);

                libraryInfoMap.clear();
                libraryInfoMap.putAll(tempLibraryInfoMap);

                // LibraryCodeAgent의 명칭-코드 매핑 정보 갱신
                libraryCodeAgent.clear();
                for (LibraryInfo lib : tempLibraryList) {
                    if (lib.libName() != null && lib.libCode() != null) {
                        libraryCodeAgent.register(lib.libName(), lib.libCode());
                    }
                }

                log.info("[LibrarySearchAgent] 총 {}건의 도서관 상세 정보 및 코드 매핑 캐시 갱신 완료!", libraryList.size());
            } else {
                log.warn("[LibrarySearchAgent] 수집된 도서관 정보가 없어 기존 캐시를 유지합니다.");
            }

        } catch (Exception e) {
            log.error("[LibrarySearchAgent] 도서관 정보 갱신 중 예외 발생", e);
        }
    }

    /**
     * 도서관 이름(자연어)으로 고유 도서관 코드를 조회합니다.
     * 캐시 미스 시 실시간으로 캐시를 갱신(refreshCache)하여 최신 정보를 다시 확인합니다.
     */
    public String getLibraryCode(String libraryName) {
        String code = libraryCodeAgent.getLibraryCode(libraryName);
        if (code == null) {
            log.warn("[LibrarySearchAgent] 도서관 명칭 캐시 미스: {}. 실시간 캐시 동기화를 진행합니다.", libraryName);
            refreshCache();
            code = libraryCodeAgent.getLibraryCode(libraryName);
        }
        return code;
    }

    /**
     * 도서관 코드로 LibraryInfo 전체 상세 정보를 조회합니다.
     */
    public LibraryInfo getLibraryInfo(String libCode) {
        if (libCode == null || libCode.isBlank()) {
            return null;
        }
        if (libraryInfoMap.isEmpty()) {
            refreshCache();
        }
        return libraryInfoMap.get(libCode.trim());
    }

    /**
     * 단일 도서관 코드 조회, 다중 지역 필터링 및 실시간 OpenAPI 최종 폴백까지
     * 검색 비즈니스 로직 전체를 수행하여 최종 도서관 리스트를 반환합니다.
     */
    public List<LibraryInfo> search(String libCode, Integer regionCode, Integer dtlRegionCode, String libraryName) {
        // 도서관 코드가 존재하는 경우 단일 룩업 시도
        if (libCode != null && !libCode.isBlank()) {
            LibraryInfo info = getLibraryInfo(libCode);
            if (info != null) {
                return List.of(info);
            }
            // 인메모리에 없는 경우 실시간 OpenAPI 단일 조회 폴백
            log.info("[LibrarySearchAgent] 도서관 코드 {} 캐시 미스. OpenAPI 단일 조회를 시도합니다.", libCode);
            LibrarySearchRequest request = LibrarySearchRequest.builder().libCode(libCode).build();
            return libraryNaruService.getLibraries(request);
        }

        // 코드가 없는 경우 인메모리 필터링 시도
        List<LibraryInfo> list = filterLibraries(regionCode, dtlRegionCode, libraryName);

        // 인메모리 필터링 결과가 비어있는 경우 실시간 OpenAPI 리스트 조회 폴백
        if (list.isEmpty()) {
            log.info("[LibrarySearchAgent] 인메모리 조회 결과 없음. 외부 OpenAPI 실시간 조회를 수행합니다.");
            LibrarySearchRequest request = LibrarySearchRequest.builder()
                    .region(regionCode)
                    .dtlRegion(dtlRegionCode)
                    .build();
            list = libraryNaruService.getLibraries(request);
        }

        return list;
    }

    /**
     * 시도 코드, 시군구 코드, 도서관 명칭으로 인메모리에서 도서관 목록을 필터링하여 반환합니다.
     * 네트워크 지연 없이 즉시 필터링 결과를 도출합니다.
     */
    public List<LibraryInfo> filterLibraries(Integer regionCodeInt, Integer dtlRegionCodeInt, String libraryName) {
        if (libraryList.isEmpty()) {
            refreshCache();
        }

        Stream<LibraryInfo> stream = libraryList.stream();

        // 1. 시도(region) 필터링
        if (regionCodeInt != null) {
            String regionCodeStr = String.valueOf(regionCodeInt);
            Region region = Region.fromCode(regionCodeStr);
            if (region != null) {
                String fullName = region.getFullName();
                String shortName = region.getShortName();
                stream = stream.filter(lib -> lib.address() != null && 
                        (lib.address().contains(fullName) || lib.address().contains(shortName)));
            }
        }

        // 2. 시군구(dtlRegion) 필터링
        if (dtlRegionCodeInt != null) {
            String dtlCodeStr = String.valueOf(dtlRegionCodeInt);
            DetailRegion dtlRegion = java.util.Arrays.stream(DetailRegion.values())
                    .filter(d -> d.getCode().equals(dtlCodeStr))
                    .findFirst()
                    .orElse(null);
            if (dtlRegion != null) {
                String dtlName = dtlRegion.getName();
                // 2-1. 상세 지역명 필터링
                stream = stream.filter(lib -> lib.address() != null && lib.address().contains(dtlName));

                // 2-2. 상세 지역의 상위 시도(CityCode) 필터링을 함께 적용하여 중복 구 이름(예: 중구, 서구) 혼선 방지
                String cityCode = dtlRegion.getCityCode();
                Region parentRegion = Region.fromCode(cityCode);
                if (parentRegion != null) {
                    String parentFullName = parentRegion.getFullName();
                    String parentShortName = parentRegion.getShortName();
                    stream = stream.filter(lib -> lib.address() != null && 
                            (lib.address().contains(parentFullName) || lib.address().contains(parentShortName)));
                }
            }
        }

        // 3. 도서관 이름 키워드 필터링
        if (libraryName != null && !libraryName.isBlank()) {
            String normalizedQuery = libraryName.replaceAll("\\s+", "");
            stream = stream.filter(lib -> lib.libName() != null && 
                    lib.libName().replaceAll("\\s+", "").contains(normalizedQuery));
        }

        return stream.collect(Collectors.toList());
    }

    /**
     * 캐시된 도서관 정보 목록 전체를 반환합니다.
     */
    public List<LibraryInfo> getAllLibraries() {
        if (libraryList.isEmpty()) {
            refreshCache();
        }
        return Collections.unmodifiableList(libraryList);
    }
}
