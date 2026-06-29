package com.nhnacademy.springailibrarycore.library.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 자연어 대출 기간 표현(예: 최근 한 달, 올해, 작년)을 API 검색 조건인 시작일(startDt)과 종료일(endDt)로 변환하는 에이전트.
 */
@Service
@Slf4j
public class LoanDateRangeAgent {

    private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 자연어 대출 기간 쿼리를 분석하여 [검색시작일자(startDt), 검색종료일자(endDt)]를 yyyy-MM-dd 포맷 문자열 배열로 반환합니다.
     * 기본 기준일은 오늘(LocalDate.now())입니다.
     */
    public String[] getDateRange(String dateRangeQuery) {
        return getDateRange(dateRangeQuery, LocalDate.now());
    }

    /**
     * 테스트용 기준일 설정을 포함하는 대출 기간 해석 메서드.
     */
    public String[] getDateRange(String dateRangeQuery, LocalDate baseDate) {
        if (dateRangeQuery == null || dateRangeQuery.isBlank()) {
            return new String[]{null, null};
        }

        LocalDate start = baseDate;
        LocalDate end = baseDate;
        String clean = dateRangeQuery.replaceAll("\\s+", "");

        if (clean.contains("최근일주일") || clean.contains("지난주")) {
            start = baseDate.minusWeeks(1);
        } else if (clean.contains("최근한달") || clean.contains("최근1개월") || clean.contains("지난달")) {
            start = baseDate.minusMonths(1);
        } else if (clean.contains("최근3개월")) {
            start = baseDate.minusMonths(3);
        } else if (clean.contains("최근6개월")) {
            start = baseDate.minusMonths(6);
        } else if (clean.contains("올해")) {
            start = LocalDate.of(baseDate.getYear(), 1, 1);
        } else if (clean.contains("작년") || clean.contains("지난해")) {
            start = LocalDate.of(baseDate.getYear() - 1, 1, 1);
            end = LocalDate.of(baseDate.getYear() - 1, 12, 31);
        } else {
            // 정규식을 통한 연도/월 추출 시도 (예: "2025년", "2026년 3월")
            Pattern yearMonthPattern = Pattern.compile("(\\d{4})년(?:(\\d{1,2})월)?");
            Matcher m = yearMonthPattern.matcher(clean);
            if (m.find()) {
                int year = Integer.parseInt(m.group(1));
                if (m.group(2) != null) {
                    int month = Integer.parseInt(m.group(2));
                    start = LocalDate.of(year, month, 1);
                    end = start.with(TemporalAdjusters.lastDayOfMonth());
                } else {
                    start = LocalDate.of(year, 1, 1);
                    end = LocalDate.of(year, 12, 31);
                }
            } else {
                // 직접 yyyy-MM-dd를 쓴 경우 검출
                Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
                Matcher m2 = datePattern.matcher(clean);
                List<String> foundDates = new ArrayList<>();
                while (m2.find()) {
                    foundDates.add(m2.group());
                }
                if (foundDates.size() >= 2) {
                    return new String[]{foundDates.get(0), foundDates.get(1)};
                } else if (foundDates.size() == 1) {
                    return new String[]{foundDates.getFirst(), baseDate.format(YYYY_MM_DD)};
                }
                
                log.warn("[LoanDateRangeAgent] 기간 설정을 해석할 수 없음(오늘 날짜로 기본설정): {}", dateRangeQuery);
                return new String[]{null, null};
            }
        }

        String startStr = start.format(YYYY_MM_DD);
        String endStr = end.format(YYYY_MM_DD);
        log.info("[LoanDateRangeAgent] 기간 해석 성공: '{}' -> startDt={}, endDt={}", dateRangeQuery, startStr, endStr);
        return new String[]{startStr, endStr};
    }
}
