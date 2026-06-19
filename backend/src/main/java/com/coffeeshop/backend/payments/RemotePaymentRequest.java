package com.coffeeshop.backend.payments;

import java.math.BigDecimal;

public record RemotePaymentRequest(
        CoffeeType coffeeType,
        BigDecimal price,
        String currency,
        String loyaltyCardId
) {}
