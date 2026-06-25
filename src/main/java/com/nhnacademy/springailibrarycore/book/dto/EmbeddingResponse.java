package com.nhnacademy.springailibrarycore.book.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EmbeddingResponse implements Serializable {
    private float[] vector;
}