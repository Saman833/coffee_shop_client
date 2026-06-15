package com.coffeeshop.backend.worker;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.coffeeshop.backend.batch.BatchItem;
import com.coffeeshop.backend.batch.BatchItemRepository;
import com.coffeeshop.backend.batch.BatchRequestRepository;
import com.coffeeshop.backend.config.WorkerProperties;
import com.coffeeshop.backend.payments.Payment;
import com.coffeeshop.backend.payments.RemotePaymentClient;
import com.coffeeshop.backend.payments.RemotePaymentRequest;
import com.coffeeshop.backend.payments.RemotePaymentResult;
import com.coffeeshop.backend.payments.ShardedPaymentRepository;

/**
 * Drives the per-batch async pipeline:
 * 1. Lease a chunk of items from the coordinator DB.
 * 2. For each item, persist into the correct shard, call the remote payments API, and record
 *    the outcome.
 * 3. Update the request-level counters and status after every item.
 * <p>
 * Each leased item is processed in its own coordinator-DB transaction so a failure on one item
 * does not invalidate work already done for siblings. Shard writes and remote calls are both
 * idempotent, so re-leasing a stuck item after a crash is safe.
 */
@Component
public class BatchOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchOrchestrator.class);

    private final BatchItemRepository itemRepository;
    private final BatchRequestRepository requestRepository;
    private final ShardedPaymentRepository paymentRepository;
    private final RemotePaymentClient remoteClient;
    private final WorkerProperties workerProperties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public BatchOrchestrator(
            BatchItemRepository itemRepository,
            BatchRequestRepository requestRepository,
            ShardedPaymentRepository paymentRepository,
            RemotePaymentClient remoteClient,
            WorkerProperties workerProperties,
            PlatformTransactionManager coordinatorTransactionManager,
            Clock systemClock) {
        this.itemRepository = itemRepository;
        this.requestRepository = requestRepository;
        this.paymentRepository = paymentRepository;
        this.remoteClient = remoteClient;
        this.workerProperties = workerProperties;
        this.transactionTemplate = new TransactionTemplate(coordinatorTransactionManager);
        this.clock = systemClock;
    }

    /**
     * Lease (atomically) and process the next chunk of items.
     *
     * @return the number of items processed in this invocation.
     */
    public int processNextChunk() {
        Instant now = clock.instant();
        Instant leaseUntil = now.plusMillis(workerProperties.leaseDurationMs());

        List<BatchItem> leased = transactionTemplate.execute(tx ->
                itemRepository.leaseNextBatch(workerProperties.batchSize(), now, leaseUntil));
        if (leased == null || leased.isEmpty()) {
            return 0;
        }

        for (BatchItem item : leased) {
            transactionTemplate.executeWithoutResult(tx -> processItem(item));
        }
        return leased.size();
    }

    void processItem(BatchItem item) {
        Instant now = clock.instant();
        try {
            RemotePaymentResult remote = remoteClient.registerPayment(
                    item.storeId(),
                    item.idempotencyKey(),
                    new RemotePaymentRequest(
                            item.coffeeType(),
                            item.price(),
                            item.currency(),
                            item.loyaltyCardId()));

            paymentRepository.saveIfAbsent(new Payment(
                    UUID.randomUUID(),
                    item.storeId(),
                    item.coffeeType(),
                    item.price(),
                    item.currency(),
                    item.loyaltyCardId(),
                    item.idempotencyKey(),
                    remote.remotePaymentId(),
                    now));

            itemRepository.markDone(item.itemId(), remote.remotePaymentId(), remote.replayed(), now);
        } catch (Exception ex) {
            LOGGER.warn("Item {} failed on attempt {}: {}", item.itemId(), item.attemptCount(), ex.getMessage());
            handleFailure(item, ex, now);
        } finally {
            requestRepository.refreshAggregate(item.requestId(), now);
        }
    }

    private void handleFailure(BatchItem item, Exception ex, Instant now) {
        if (item.attemptCount() >= workerProperties.maxAttempts()) {
            itemRepository.markFailed(item.itemId(), ex.getMessage(), now);
        } else {
            itemRepository.requeueForRetry(item.itemId(), ex.getMessage(), now);
        }
    }
}
