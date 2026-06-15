package com.coffeeshop.backend.batch;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.coffeeshop.backend.payments.CoffeeType;

public record BatchItem(
        UUID itemId,
        UUID requestId,
        String storeId,
        String idempotencyKey,
        CoffeeType coffeeType,
        BigDecimal price,
        String currency,
        String loyaltyCardId,
        ItemStatus status,
        int attemptCount,
        String lastError,
        String remotePaymentId,
        boolean replayed,
        Instant leaseUntil,
        Instant createdAt,
        Instant updatedAt
) {}
