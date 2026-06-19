package com.coffeeshop.backend.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("coffeeshop.worker")
public record WorkerProperties(
        boolean enabled,
        @Min(100) long pollDelayMs,
        @Min(1) int batchSize,
        @Min(1) int maxAttempts,
        @Min(1000) long leaseDurationMs
) {}
