package com.nhnacademy.springailibrarycore.book.dto.cohere;

import java.util.List;

public record CohereRerankResponse(String id, List<CohereRerankResult> results) {}