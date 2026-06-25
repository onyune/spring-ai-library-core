package com.nhnacademy.springailibrarycore.book.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class EmbeddingResponse implements Serializable {
    private float[] vector;
}