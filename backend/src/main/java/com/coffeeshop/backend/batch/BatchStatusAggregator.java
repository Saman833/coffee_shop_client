package com.coffeeshop.backend.batch;

/**
 * Pure logic for deriving the request-level status from per-item counters.
 * Kept free of Spring/JDBC so it can be unit tested in isolation.
 */
public final class BatchStatusAggregator {

    private BatchStatusAggregator() {}

    /**
     * Determines the overall status of a batch request given the totals.
     *
     * @param total          total items submitted in the batch
     * @param processed      number of items that have reached a terminal state (DONE or FAILED)
     * @param succeeded      number of items that ended in DONE (created or replayed)
     * @param failed         number of items that ended in FAILED after retries
     * @return the aggregated batch status
     */
    public static BatchStatus aggregate(int total, int processed, int succeeded, int failed) {
        if (processed < total) {
            return processed == 0 ? BatchStatus.PENDING : BatchStatus.PROCESSING;
        }
        if (failed == 0) {
            return BatchStatus.DONE;
        }
        if (succeeded == 0) {
            return BatchStatus.FAILED;
        }
        return BatchStatus.FAILED_PARTIAL;
    }
}
