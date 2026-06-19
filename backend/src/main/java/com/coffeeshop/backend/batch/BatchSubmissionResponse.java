package com.coffeeshop.backend.batch;

import java.util.UUID;

public record BatchSubmissionResponse(
        UUID requestId,
        BatchStatus status,
        int total
) {}
