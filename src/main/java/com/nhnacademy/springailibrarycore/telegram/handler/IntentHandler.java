package com.nhnacademy.springailibrarycore.telegram.handler;

import com.nhnacademy.springailibrarycore.telegram.domain.AssistantIntent;
import com.nhnacademy.springailibrarycore.telegram.dto.AskRequest;
import com.nhnacademy.springailibrarycore.telegram.dto.AskResponse;
import java.util.List;

public interface IntentHandler {
    boolean supports(AssistantIntent intent);
    List<AskResponse> handle (AskRequest request);
}
