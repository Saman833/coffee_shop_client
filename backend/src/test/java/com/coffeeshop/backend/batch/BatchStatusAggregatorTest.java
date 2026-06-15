package com.coffeeshop.backend.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BatchStatusAggregatorTest {

    @Test
    void pendingWhenNothingProcessedYet() {
        assertThat(BatchStatusAggregator.aggregate(5, 0, 0, 0)).isEqualTo(BatchStatus.PENDING);
    }

    @Test
    void processingWhenSomeItemsStillInFlight() {
        assertThat(BatchStatusAggregator.aggregate(5, 2, 2, 0)).isEqualTo(BatchStatus.PROCESSING);
    }

    @Test
    void doneWhenAllItemsTerminalAndNoFailures() {
        assertThat(BatchStatusAggregator.aggregate(5, 5, 5, 0)).isEqualTo(BatchStatus.DONE);
    }

    @Test
    void failedWhenEveryItemFailed() {
        assertThat(BatchStatusAggregator.aggregate(3, 3, 0, 3)).isEqualTo(BatchStatus.FAILED);
    }

    @Test
    void failedPartialWhenSomeFailedAndSomeSucceeded() {
        assertThat(BatchStatusAggregator.aggregate(5, 5, 3, 2)).isEqualTo(BatchStatus.FAILED_PARTIAL);
    }
}
