package com.nhnacademy.springailibrarycore.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RedisCacheConfigTest {

    @Test
    @DisplayName("DefaultTyping이 활성화된 ObjectMapper를 통해 BookSearchPageResult를 직렬화 및 역직렬화할 때 에러가 발생하지 않아야 한다")
    void testBookSearchPageResultSerializationWithDefaultTyping() throws IOException {
        // Given
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        
        SimpleModule pageModule = new SimpleModule();
        mapper.registerModule(pageModule);

        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        BookSearchResponse book = BookSearchResponse.builder()
                .id(1L)
                .title("Test Book")
                .isbn("1234567890")
                .price(BigDecimal.valueOf(15000))
                .build();

        BookSearchPageResult originalPage = new BookSearchPageResult(
                List.of(book),
                1
        );

        // When
        String json = mapper.writeValueAsString(originalPage);
        System.out.println("Serialized JSON: " + json);

        // Then
        BookSearchPageResult deserializedPage = mapper.readValue(json, BookSearchPageResult.class);

        assertThat(deserializedPage).isNotNull();
        assertThat(deserializedPage.getTotalElements()).isEqualTo(1);
        assertThat(deserializedPage.getContent()).hasSize(1);
        
        Object firstItem = deserializedPage.getContent().get(0);
        assertThat(firstItem).isInstanceOf(BookSearchResponse.class);
        BookSearchResponse deserializedBook = (BookSearchResponse) firstItem;
        assertThat(deserializedBook.getId()).isEqualTo(1L);
        assertThat(deserializedBook.getTitle()).isEqualTo("Test Book");
    }
}
