package com.coffeeshop.backend.batch;

import java.math.BigDecimal;

import com.coffeeshop.backend.payments.CoffeeType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PaymentItemRequest(
        @NotBlank
        String idempotencyKey,

        @NotNull
        CoffeeType coffeeType,

        @NotNull
        @DecimalMin(value = "0.01", message = "price must be greater than 0")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code")
        String currency,

        @NotBlank
        String loyaltyCardId
) {}
