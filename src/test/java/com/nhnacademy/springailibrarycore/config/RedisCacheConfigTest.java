package com.nhnacademy.springailibrarycore.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class RedisCacheConfigTest {

    @Test
    @DisplayName("DefaultTyping이 활성화된 ObjectMapper를 통해 PageImpl을 직렬화 및 역직렬화할 때 에러가 발생하지 않아야 한다")
    void testPageImplSerializationWithDefaultTyping() throws IOException {
        // Given
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        
        SimpleModule pageModule = new SimpleModule();
        pageModule.addDeserializer(PageImpl.class, new PageImplDeserializer());
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

        PageImpl<BookSearchResponse> originalPage = new PageImpl<>(
                List.of(book),
                PageRequest.of(0, 10),
                1
        );

        // When
        String json = mapper.writeValueAsString(originalPage);
        System.out.println("Serialized JSON: " + json);

        // Then
        PageImpl<?> deserializedPage = mapper.readValue(json, PageImpl.class);

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
