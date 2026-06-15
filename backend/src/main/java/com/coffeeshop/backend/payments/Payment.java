package com.coffeeshop.backend.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Payment(
        UUID paymentId,
        String storeId,
        CoffeeType coffeeType,
        BigDecimal price,
        String currency,
        String loyaltyCardId,
        String idempotencyKey,
        String remotePaymentId,
        Instant createdAt
) {}
