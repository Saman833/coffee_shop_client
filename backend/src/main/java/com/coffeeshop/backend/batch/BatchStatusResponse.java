package com.coffeeshop.backend.batch;

import java.time.Instant;
import java.util.UUID;

public record BatchStatusResponse(
        UUID requestId,
        BatchStatus status,
        int total,
        int processed,
        int succeeded,
        int replayed,
        int failed,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {}
