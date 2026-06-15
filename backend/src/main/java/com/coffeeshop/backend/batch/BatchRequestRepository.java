package com.coffeeshop.backend.batch;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BatchRequestRepository {

    private final JdbcTemplate jdbc;

    public BatchRequestRepository(JdbcTemplate coordinatorJdbcTemplate) {
        this.jdbc = coordinatorJdbcTemplate;
    }

    public void create(BatchRequest request) {
        jdbc.update(
                "INSERT INTO batch_request (request_id, store_id, status, total_items, processed_items, "
                        + "succeeded_items, replayed_items, failed_items, failure_reason, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                request.requestId(),
                request.storeId(),
                request.status().name(),
                request.totalItems(),
                request.processedItems(),
                request.succeededItems(),
                request.replayedItems(),
                request.failedItems(),
                request.failureReason(),
                Timestamp.from(request.createdAt()),
                Timestamp.from(request.updatedAt()));
    }

    public Optional<BatchRequest> findById(UUID requestId) {
        List<BatchRequest> rows = jdbc.query(
                "SELECT request_id, store_id, status, total_items, processed_items, succeeded_items, "
                        + "replayed_items, failed_items, failure_reason, created_at, updated_at "
                        + "FROM batch_request WHERE request_id = ?",
                rowMapper(),
                requestId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /**
     * Recomputes counters and status for a request from its current set of items in a single
     * statement. Called after each item state change so the aggregate stays consistent.
     */
    public void refreshAggregate(UUID requestId, Instant now) {
        BatchRequest existing = findById(requestId).orElse(null);
        if (existing == null) {
            return;
        }
        Counters counters = countItems(requestId);
        BatchStatus status = BatchStatusAggregator.aggregate(
                existing.totalItems(),
                counters.processed(),
                counters.succeeded(),
                counters.failed());
        jdbc.update(
                "UPDATE batch_request SET status = ?, processed_items = ?, succeeded_items = ?, "
                        + "replayed_items = ?, failed_items = ?, updated_at = ? WHERE request_id = ?",
                status.name(),
                counters.processed(),
                counters.succeeded(),
                counters.replayed(),
                counters.failed(),
                Timestamp.from(now),
                requestId);
    }

    private Counters countItems(UUID requestId) {
        return jdbc.queryForObject(
                "SELECT "
                        + "  COUNT(*) FILTER (WHERE status IN ('DONE', 'FAILED')) AS processed, "
                        + "  COUNT(*) FILTER (WHERE status = 'DONE' AND replayed = FALSE) AS succeeded, "
                        + "  COUNT(*) FILTER (WHERE status = 'DONE' AND replayed = TRUE) AS replayed, "
                        + "  COUNT(*) FILTER (WHERE status = 'FAILED') AS failed "
                        + "FROM batch_item WHERE request_id = ?",
                (rs, rowNum) -> new Counters(
                        rs.getInt("processed"),
                        rs.getInt("succeeded"),
                        rs.getInt("replayed"),
                        rs.getInt("failed")),
                requestId);
    }

    private static RowMapper<BatchRequest> rowMapper() {
        return (rs, rowNum) -> new BatchRequest(
                rs.getObject("request_id", UUID.class),
                rs.getString("store_id"),
                BatchStatus.valueOf(rs.getString("status")),
                rs.getInt("total_items"),
                rs.getInt("processed_items"),
                rs.getInt("succeeded_items"),
                rs.getInt("replayed_items"),
                rs.getInt("failed_items"),
                rs.getString("failure_reason"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private record Counters(int processed, int succeeded, int replayed, int failed) {}
}
