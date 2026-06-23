package com.nhnacademy.springailibrarycore.config;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SearchTypeConverter implements Converter<String, SearchType> {

    @Override
    public SearchType convert(String source) {
        return SearchType.from(source);
    }
}
