package com.coffeeshop.backend.payments;

/**
 * Outcome of calling the remote payments API. Mirrors the upstream contract
 * (201 = created, 200 = idempotent replay).
 */
public record RemotePaymentResult(
        String remotePaymentId,
        boolean replayed
) {}
