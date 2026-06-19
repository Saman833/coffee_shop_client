package com.coffeeshop.backend.batch;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BatchSubmissionRequest(
        @NotBlank String storeId,
        @NotEmpty @Size(max = 1000) @Valid List<PaymentItemRequest> payments
) {}
