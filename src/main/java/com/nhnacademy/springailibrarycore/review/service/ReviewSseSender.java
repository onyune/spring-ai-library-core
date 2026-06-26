package com.nhnacademy.springailibrarycore.review.service;

import com.nhnacademy.springailibrarycore.review.dto.ReviewSummaryResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
public class ReviewSseSender {
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void addEmitter(Long bookId, SseEmitter emitter) {
        emitters.computeIfAbsent(bookId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("[ReviewSseSender] 도서 ID {}에 SSE 구독 등록 완료", bookId);

        emitter.onCompletion(() -> removeEmitter(bookId, emitter));
        emitter.onTimeout(() -> removeEmitter(bookId, emitter));
        emitter.onError((ex) -> removeEmitter(bookId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE Connection  established. Processing review summary...")
            );
        } catch (IOException e) {
            log.error("[ReviewSseSender - addEmitter]  초기 연결 메시지 전송 실패 (클라이언트 조기 이탈 의심) 도서 ID:{}", bookId);
        }
    }

    public void sendSummary(Long bookId, ReviewSummaryResponse response) {
        CopyOnWriteArrayList<SseEmitter> sseEmitters = emitters.get(bookId);
        if (sseEmitters == null || sseEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("review-summary")
                        .data(response));
                emitter.complete();
            } catch (IllegalStateException | IOException e) {
                removeEmitter(bookId, emitter);
                log.debug("[ReviewSseSender] 클라이언트 연결 종료됨. 도서 ID:{}",bookId);
            }catch (Exception e){
                log.error("[ReviewSseSender] SSE 전송 중 심각한 오류 발생. 도서 ID:{}", bookId);
                emitter.completeWithError(e);
                removeEmitter(bookId, emitter);
            }
        }
    }

    private void removeEmitter(Long bookId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> sseEmitters = emitters.get(bookId);
        if(sseEmitters != null) {
            sseEmitters.remove(emitter);

            if(sseEmitters.isEmpty()) {
                emitters.remove(bookId);
            }
        }
    }
}
