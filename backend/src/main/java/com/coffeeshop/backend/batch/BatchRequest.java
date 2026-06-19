package com.coffeeshop.backend.batch;

import java.time.Instant;
import java.util.UUID;

public record BatchRequest(
        UUID requestId,
        String storeId,
        BatchStatus status,
        int totalItems,
        int processedItems,
        int succeededItems,
        int replayedItems,
        int failedItems,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {}
