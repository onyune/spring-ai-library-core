package com.nhnacademy.springailibrarycore.telegram.dto;

import com.nhnacademy.springailibrarycore.telegram.domain.AssistantIntent;

public record IntentResult(AssistantIntent intent, String keyword) {}