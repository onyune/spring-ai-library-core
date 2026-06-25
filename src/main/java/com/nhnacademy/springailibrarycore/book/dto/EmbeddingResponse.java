package com.nhnacademy.springailibrarycore.book.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;

// 단순히 float[]을 감싸는 용도의 객체
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public record EmbeddingResponse(float[] vector) implements Serializable {
}