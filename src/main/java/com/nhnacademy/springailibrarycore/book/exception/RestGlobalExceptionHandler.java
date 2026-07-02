package com.nhnacademy.springailibrarycore.book.exception;

import com.nhnacademy.springailibrarycore.review.exception.ReviewException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = {
        "com.nhnacademy.springailibrarycore.review.controller",
        "com.nhnacademy.springailibrarycore.telegram.controller"
})
public class RestGlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[REST Error] 잘못된 요청: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(ReviewException.class)
    public ResponseEntity<Map<String, Object>> handleReviewException(ReviewException ex) {
        log.warn("[REST Error] 리뷰 서비스 오류: {}", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "status", ex.getStatus().value(),
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(org.springframework.web.context.request.async.AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(org.springframework.web.context.request.async.AsyncRequestNotUsableException ex) {
        log.debug("[REST] 비동기 요청 처리 중 클라이언트 연결 끊김 (SSE Broken pipe): {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("[REST Error] 예상치 못한 서버 오류", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "message", "서버 처리 중 오류가 발생했습니다."
        ));
    }
}
