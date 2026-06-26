package com.nhnacademy.springailibrarycore.telegram.handler.impl;

import com.nhnacademy.springailibrarycore.telegram.domain.AssistantIntent;
import com.nhnacademy.springailibrarycore.telegram.dto.AskRequest;
import com.nhnacademy.springailibrarycore.telegram.dto.AskResponse;
import com.nhnacademy.springailibrarycore.telegram.handler.IntentHandler;
import java.util.List;

public class GeneralHandler implements IntentHandler {
    @Override
    public boolean supports(AssistantIntent intent) {
        return intent.equals(AssistantIntent.GENERAL_CHAT);
    }

    @Override
    public List<AskResponse> handle(AskRequest request) {
        return List.of(new AskResponse(null, "도서관 정보 및 도서 추천을 해주세요!"));
    }
}
