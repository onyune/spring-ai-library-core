package com.nhnacademy.springailibrarycore.book.dto;

public record BookFeedbackStatistics(
        Long bookId,
        long goodCount,
        long badCount
) {

    public long totalCount() {
        return goodCount + badCount;
    }

    public double score() {
        long total = totalCount();
        if (total == 0) {
            return 0.0;
        }
        // 피드백 점수
        return (double) (goodCount - badCount) / total;
    }
}
