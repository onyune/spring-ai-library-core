package com.nhnacademy.springailibrarycore.telegram.handler.impl;

import com.nhnacademy.springailibrarycore.telegram.domain.AssistantIntent;
import com.nhnacademy.springailibrarycore.telegram.dto.AskRequest;
import com.nhnacademy.springailibrarycore.telegram.dto.AskResponse;
import com.nhnacademy.springailibrarycore.telegram.handler.IntentHandler;
import java.util.List;

public class LibraryInfoHandler implements IntentHandler {

    @Override
    public boolean supports(AssistantIntent intent) {
        return intent.equals(AssistantIntent.LIBRARY_INFO);
    }

    @Override
    public List<AskResponse> handle(AskRequest request) {
        return List.of(new AskResponse(null, "여기에 결과값"));
    }
}
