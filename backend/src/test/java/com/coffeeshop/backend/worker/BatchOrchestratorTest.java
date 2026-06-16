package com.coffeeshop.backend.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.coffeeshop.backend.batch.BatchItem;
import com.coffeeshop.backend.batch.BatchItemRepository;
import com.coffeeshop.backend.batch.BatchRequestRepository;
import com.coffeeshop.backend.batch.ItemStatus;
import com.coffeeshop.backend.config.WorkerProperties;
import com.coffeeshop.backend.payments.CoffeeType;
import com.coffeeshop.backend.payments.RemotePaymentClient;
import com.coffeeshop.backend.payments.RemotePaymentResult;
import com.coffeeshop.backend.payments.ShardedPaymentRepository;

@ExtendWith(MockitoExtension.class)
class BatchOrchestratorTest {

    @Mock private BatchItemRepository itemRepository;
    @Mock private BatchRequestRepository requestRepository;
    @Mock private ShardedPaymentRepository paymentRepository;
    @Mock private RemotePaymentClient remoteClient;
    @Mock private PlatformTransactionManager txManager;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final WorkerProperties workerProps = new WorkerProperties(true, 1000, 25, 3, 60000);

    private BatchOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        orchestrator = new BatchOrchestrator(
                itemRepository,
                requestRepository,
                paymentRepository,
                remoteClient,
                workerProps,
                txManager,
                fixedClock);
    }

    @Test
    void marksItemDoneAndPersistsToShardWhenRemoteReturnsCreated() {
        BatchItem item = newItem(0);
        when(itemRepository.leaseNextBatch(eq(25), any(), any())).thenReturn(List.of(item));
        when(remoteClient.registerPayment(eq(item.storeId()), eq(item.idempotencyKey()), any()))
                .thenReturn(new RemotePaymentResult("rp-1", false));

        int processed = orchestrator.processNextChunk();

        assertThat(processed).isEqualTo(1);
        verify(paymentRepository).saveIfAbsent(any());
        verify(itemRepository).markDone(eq(item.itemId()), eq("rp-1"), eq(false), any());
        verify(itemRepository, never()).markFailed(any(), anyString(), any());
        verify(requestRepository).refreshAggregate(eq(item.requestId()), any());
    }

    @Test
    void markedAsReplayedWhenRemoteReturnsTwoHundred() {
        BatchItem item = newItem(0);
        when(itemRepository.leaseNextBatch(anyInt(), any(), any())).thenReturn(List.of(item));
        when(remoteClient.registerPayment(any(), any(), any()))
                .thenReturn(new RemotePaymentResult("rp-9", true));

        orchestrator.processNextChunk();

        verify(itemRepository).markDone(eq(item.itemId()), eq("rp-9"), eq(true), any());
    }

    @Test
    void requeuesItemBelowMaxAttemptsAndDoesNotMarkFailed() {
        BatchItem item = newItem(1); // attemptCount=1, maxAttempts=3
        when(itemRepository.leaseNextBatch(anyInt(), any(), any())).thenReturn(List.of(item));
        when(remoteClient.registerPayment(any(), any(), any()))
                .thenThrow(new RuntimeException("connection refused"));

        orchestrator.processNextChunk();

        verify(itemRepository).requeueForRetry(eq(item.itemId()), anyString(), any());
        verify(itemRepository, never()).markFailed(any(), anyString(), any());
        verify(itemRepository, never()).markDone(any(), any(), eq(false), any());
        verify(requestRepository).refreshAggregate(eq(item.requestId()), any());
    }

    @Test
    void failsItemPermanentlyWhenMaxAttemptsReached() {
        BatchItem item = newItem(3); // attemptCount=3 == maxAttempts
        when(itemRepository.leaseNextBatch(anyInt(), any(), any())).thenReturn(List.of(item));
        when(remoteClient.registerPayment(any(), any(), any()))
                .thenThrow(new RuntimeException("upstream 500"));

        orchestrator.processNextChunk();

        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(itemRepository).markFailed(eq(item.itemId()), reason.capture(), any());
        assertThat(reason.getValue()).contains("upstream 500");
        verify(itemRepository, never()).requeueForRetry(any(), anyString(), any());
    }

    @Test
    void failsImmediatelyOnPermanentRemoteErrorEvenWithAttemptsRemaining() {
        BatchItem item = newItem(0); // attempt 0, far below maxAttempts
        when(itemRepository.leaseNextBatch(anyInt(), any(), any())).thenReturn(List.of(item));
        when(remoteClient.registerPayment(any(), any(), any()))
                .thenThrow(new RemotePaymentClient.PermanentRemotePaymentException("400 invalid currency"));

        orchestrator.processNextChunk();

        verify(itemRepository).markFailed(eq(item.itemId()), anyString(), any());
        verify(itemRepository, never()).requeueForRetry(any(), anyString(), any());
        verify(requestRepository).refreshAggregate(eq(item.requestId()), any());
    }

    @Test
    void returnsZeroWhenNothingLeased() {
        when(itemRepository.leaseNextBatch(anyInt(), any(), any())).thenReturn(List.of());

        int processed = orchestrator.processNextChunk();

        assertThat(processed).isZero();
        verify(remoteClient, never()).registerPayment(any(), any(), any());
        verify(requestRepository, never()).refreshAggregate(any(), any());
    }

    @Test
    void processesEachLeasedItemIndependently() {
        BatchItem first = newItem(0);
        BatchItem second = newItem(0);
        when(itemRepository.leaseNextBatch(anyInt(), any(), any())).thenReturn(List.of(first, second));
        when(remoteClient.registerPayment(eq(first.storeId()), eq(first.idempotencyKey()), any()))
                .thenReturn(new RemotePaymentResult("a", false));
        when(remoteClient.registerPayment(eq(second.storeId()), eq(second.idempotencyKey()), any()))
                .thenThrow(new RuntimeException("boom"));

        int processed = orchestrator.processNextChunk();

        assertThat(processed).isEqualTo(2);
        verify(itemRepository).markDone(eq(first.itemId()), eq("a"), eq(false), any());
        verify(itemRepository).requeueForRetry(eq(second.itemId()), anyString(), any());
        verify(requestRepository, times(2)).refreshAggregate(any(), any());
    }

    private static BatchItem newItem(int attempt) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new BatchItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "store-1",
                "idem-" + UUID.randomUUID(),
                CoffeeType.LATTE,
                new BigDecimal("4.50"),
                "USD",
                "loyalty-1",
                ItemStatus.PROCESSING,
                attempt,
                null,
                null,
                false,
                now.plusSeconds(60),
                now,
                now);
    }
}
