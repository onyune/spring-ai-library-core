package com.nhnacademy.springailibrarycore.book.dto.cohere;

import java.util.List;

public record CohereRerankRequest(String model, String query, List<String> documents, int top_n) {}