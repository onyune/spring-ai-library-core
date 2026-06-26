package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.common.codes.DetailRegion;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 자연어 시군구 명칭(예: 강남구, 마포구, 분당)을 5자리 상세 지역 코드로 변환하는 에이전트.
 */
@Service
@Slf4j
public class DetailRegionCodeAgent {

    /**
     * 자연어 시군구 명칭으로 5자리 상세 지역 코드를 조회합니다.
     * 시도 코드(regionCode)가 있으면 중복 행정구역 명칭(예: 중구)을 올바르게 판별하는데 사용됩니다.
     */
    public Integer getDetailRegionCode(String dtlRegionName, Integer regionCode) {
        if (dtlRegionName == null || dtlRegionName.isBlank()) {
            return null;
        }

        String cityCodeStr = (regionCode != null) ? String.valueOf(regionCode) : null;
        DetailRegion dtlRegion = DetailRegion.fromNameAndCity(dtlRegionName, cityCodeStr);
        
        if (dtlRegion == null) {
            // 더 넓은 범위의 포함 관계(Fuzzy/Substring) 일치 시도
            String cleanName = dtlRegionName.replaceAll("\\s+", "");
            List<DetailRegion> candidates = java.util.Arrays.stream(DetailRegion.values())
                    .filter(d -> d.getName().contains(cleanName) || cleanName.contains(d.getName()))
                    .toList();

            if (!candidates.isEmpty()) {
                if (cityCodeStr != null) {
                    dtlRegion = candidates.stream()
                            .filter(d -> d.getCityCode().equals(cityCodeStr))
                            .findFirst()
                            .orElse(candidates.getFirst());
                } else {
                    dtlRegion = candidates.getFirst();
                }
            }
        }

        if (dtlRegion != null) {
            try {
                return Integer.parseInt(dtlRegion.getCode());
            } catch (NumberFormatException e) {
                log.error("[DetailRegionCodeAgent] 코드 파싱 실패: {}", dtlRegion.getCode(), e);
            }
        }

        log.warn("[DetailRegionCodeAgent] 매칭되는 상세 지역 코드를 찾을 수 없습니다: {}", dtlRegionName);
        return null;
    }
}
