package com.coffeeshop.backend.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("coffeeshop.remote")
public record RemoteProperties(
        @NotBlank String baseUrl
) {}
