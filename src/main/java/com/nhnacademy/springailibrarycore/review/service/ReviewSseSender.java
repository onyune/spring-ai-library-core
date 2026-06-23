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
            throw new RuntimeException(e);
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
            } catch (IOException e) {
                emitter.completeWithError(e);
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
