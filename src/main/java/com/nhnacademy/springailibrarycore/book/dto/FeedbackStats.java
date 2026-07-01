package com.nhnacademy.springailibrarycore.book.dto;

/**
 * GOOD/BAD 피드백의 집계 및 계산 결과입니다.
 */
public record FeedbackStats(
        long goodCount,
        long badCount,
        long totalCount,
        double goodRatio,
        double feedbackScore
) {

    // 전체 피드백 수가 가중치 반영에 필요한 최소 개수 이상인지 확인합니다.
    public boolean hasMinimumCount(long minimumCount) {
        if (minimumCount < 0) {
            throw new IllegalArgumentException(
                    "최소 피드백 수는 음수일 수 없습니다."
            );
        }
        return totalCount >= minimumCount;
    }
}
