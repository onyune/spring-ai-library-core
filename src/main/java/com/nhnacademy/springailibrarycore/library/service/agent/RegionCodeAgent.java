package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.common.codes.Region;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 자연어 시도 명칭(예: 서울, 경기도, 부산광역시)을 2자리 지역 코드로 변환하는 에이전트.
 */
@Service
@Slf4j
public class RegionCodeAgent {

    /**
     * 자연어 지역 명칭으로 2자리 지역 코드를 조회합니다.
     */
    public Integer getRegionCode(String regionName) {
        if (regionName == null || regionName.isBlank()) {
            return null;
        }

        Region region = Region.fromCode(regionName);
        if (region == null) {
            region = Region.fromName(regionName);
        }
        if (region == null) {
            // 더 넓은 범위의 포함 관계(Fuzzy/Substring) 일치 시도
            String cleanName = regionName.replaceAll("\\s+", "");
            region = Arrays.stream(Region.values())
                    .filter(r -> cleanName.contains(r.getFullName()) || cleanName.contains(r.getShortName())
                            || r.getFullName().contains(cleanName) || r.getShortName().contains(cleanName))
                    .findFirst()
                    .orElse(null);
        }

        if (region != null) {
            try {
                return Integer.parseInt(region.getCode());
            } catch (NumberFormatException e) {
                log.error("[RegionCodeAgent] 코드 파싱 실패: {}", region.getCode(), e);
            }
        }

        log.warn("[RegionCodeAgent] 매칭되는 지역 코드를 찾을 수 없습니다: {}", regionName);
        return null;
    }
}
