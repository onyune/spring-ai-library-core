package com.nhnacademy.springailibrarycore.book.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleInvalidRequest(
            IllegalArgumentException exception,
            Model model
    ) {
        log.warn("잘못된 요청: {}", exception.getMessage());
        return errorView(model, 400, exception.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(AiServiceException.class)
    public String handleAiService(
            AiServiceException exception,
            Model model
    ) {
        log.error("AI 서비스 호출 오류", exception);
        return errorView(
                model,
                502,
                "AI 추천 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요."
        );
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(AiResponseParseException.class)
    public String handleAiResponse(
            AiResponseParseException exception,
            Model model
    ) {
        log.error("AI 응답 처리 오류", exception);
        return errorView(
                model,
                502,
                "AI 추천 결과를 처리할 수 없습니다. 잠시 후 다시 시도해주세요."
        );
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public String handleUnexpected(
            Exception exception,
            Model model
    ) {
        log.error("예상하지 못한 서버 오류", exception);
        return errorView(
                model,
                500,
                "서버 처리 중 오류가 발생했습니다."
        );
    }

    private String errorView(
            Model model,
            int status,
            String message
    ) {
        model.addAttribute("status", status);
        model.addAttribute("message", message);
        return "common/error/error";
    }
}
