package com.coffeeshop.backend.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("coffeeshop.coordinator")
public record CoordinatorProperties(
        @NotBlank String url,
        @NotBlank String username,
        String password
) {}
