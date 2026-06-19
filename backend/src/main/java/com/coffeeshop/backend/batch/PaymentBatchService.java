package com.coffeeshop.backend.batch;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentBatchService {

    private final BatchRequestRepository requestRepository;
    private final BatchItemRepository itemRepository;
    private final Clock clock;

    public PaymentBatchService(
            BatchRequestRepository requestRepository,
            BatchItemRepository itemRepository,
            Clock systemClock) {
        this.requestRepository = requestRepository;
        this.itemRepository = itemRepository;
        this.clock = systemClock;
    }

    @Transactional
    public BatchSubmissionResponse submit(BatchSubmissionRequest request) {
        UUID requestId = UUID.randomUUID();
        Instant now = clock.instant();

        BatchRequest batchRequest = new BatchRequest(
                requestId,
                request.storeId(),
                BatchStatus.PENDING,
                request.payments().size(),
                0, 0, 0, 0,
                null,
                now,
                now);
        requestRepository.create(batchRequest);

        List<BatchItem> items = request.payments().stream()
                .map(p -> new BatchItem(
                        UUID.randomUUID(),
                        requestId,
                        request.storeId(),
                        p.idempotencyKey(),
                        p.coffeeType(),
                        p.price(),
                        p.currency(),
                        p.loyaltyCardId(),
                        ItemStatus.PENDING,
                        0,
                        null,
                        null,
                        false,
                        null,
                        now,
                        now))
                .toList();
        itemRepository.saveAll(items);

        return new BatchSubmissionResponse(requestId, BatchStatus.PENDING, items.size());
    }

    @Transactional(readOnly = true)
    public Optional<BatchStatusResponse> getStatus(UUID requestId) {
        return requestRepository.findById(requestId).map(this::toStatusResponse);
    }

    private BatchStatusResponse toStatusResponse(BatchRequest request) {
        return new BatchStatusResponse(
                request.requestId(),
                request.status(),
                request.totalItems(),
                request.processedItems(),
                request.succeededItems(),
                request.replayedItems(),
                request.failedItems(),
                request.failureReason(),
                request.createdAt(),
                request.updatedAt());
    }
}
