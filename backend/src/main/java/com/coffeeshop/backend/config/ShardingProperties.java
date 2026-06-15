package com.coffeeshop.backend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("coffeeshop.sharding")
public record ShardingProperties(
        @Min(1) int count,
        @NotEmpty @Valid List<ShardInstance> instances
) {

    public record ShardInstance(
            @NotBlank String url,
            @NotBlank String username,
            String password
    ) {}
}
