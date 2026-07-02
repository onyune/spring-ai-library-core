package com.nhnacademy.springailibrarycore.library.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 도서관 이름(자연어)을 고유 6자리 도서관 코드로 변환하는 초고속 에이전트.
 * 실질적인 도서관 정보 검색이나 필터링은 LibrarySearchAgent가 전담하며,
 * 이 에이전트는 오직 명칭-코드 간 매핑의 인메모리 관리 및 탐색만을 수행합니다.
 */
@Service
@Slf4j
public class LibraryCodeAgent {

    // 도서관 이름(공백 제거 정규화 적용)을 키로 도서관 코드 매핑
    private final Map<String, String> libraryCodeMap = new ConcurrentHashMap<>();

    /**
     * 도서관 명칭과 코드 매핑을 등록합니다.
     */
    public void register(String libraryName, String code) {
        if (libraryName != null && code != null) {
            libraryCodeMap.put(libraryName.trim(), code.trim());
        }
    }

    /**
     * 캐시된 명칭-코드 정보를 비웁니다.
     */
    public void clear() {
        libraryCodeMap.clear();
    }

    /**
     * 도서관 이름(자연어)으로 고유 도서관 코드를 조회합니다.
     * 공백을 무시하고 정확히 일치하거나 부분 포함 관계를 비교해 반환합니다.
     */
    public String getLibraryCode(String libraryName) {
        if (libraryName == null || libraryName.isBlank()) {
            throw new IllegalArgumentException("도서관 이름을 입력해주세요");
        }

        String normalizedQuery = libraryName.replaceAll("\\s+", "");

        // 입력값이 이미 캐시에 유효하게 등록된 도서관 코드(Value) 자체인지 판별
        if (libraryCodeMap.containsValue(normalizedQuery)) {
            log.info("[LibraryCodeAgent] 캐시에 등록된 유효한 도서관 코드로 확인되어 즉시 반환합니다: {}", normalizedQuery);
            return normalizedQuery;
        }

        // 캐시에서 공백을 제외한 정확한 일치 판별
        String code = lookup(normalizedQuery, true);
        if (code != null) return code;

        // 캐시에서 포함 관계(Fuzzy/Substring) 일치 판별
        code = lookup(normalizedQuery, false);
        if (code != null) return code;

        log.warn("[LibraryCodeAgent] 매칭되는 도서관 코드를 찾을 수 없습니다: {}", libraryName);
        return null;
    }

    private String lookup(String normalizedQuery, boolean exact) {
        for (Map.Entry<String, String> entry : libraryCodeMap.entrySet()) {
            String cachedName = entry.getKey().replaceAll("\\s+", "");
            if (exact) {
                if (cachedName.equals(normalizedQuery)) {
                    return entry.getValue();
                }
            } else {
                if (cachedName.contains(normalizedQuery) || normalizedQuery.contains(cachedName)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 현재 캐시된 매핑 데이터 개수를 반환합니다.
     */
    public int getCacheSize() {
        return libraryCodeMap.size();
    }
}
