package com.nhnacademy.springailibrarycore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "data4-library.api")
public class Data4LibraryApiProperties {
    private String url;
    private String serviceKey;
}