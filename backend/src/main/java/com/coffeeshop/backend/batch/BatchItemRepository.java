package com.coffeeshop.backend.batch;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.coffeeshop.backend.payments.CoffeeType;

@Repository
public class BatchItemRepository {

    private final JdbcTemplate jdbc;

    public BatchItemRepository(JdbcTemplate coordinatorJdbcTemplate) {
        this.jdbc = coordinatorJdbcTemplate;
    }

    public void saveAll(List<BatchItem> items) {
        if (items.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(
                "INSERT INTO batch_item (item_id, request_id, store_id, idempotency_key, coffee_type, "
                        + "price, currency, loyalty_card_id, status, attempt_count, last_error, "
                        + "remote_payment_id, replayed, lease_until, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                        BatchItem item = items.get(i);
                        ps.setObject(1, item.itemId());
                        ps.setObject(2, item.requestId());
                        ps.setString(3, item.storeId());
                        ps.setString(4, item.idempotencyKey());
                        ps.setString(5, item.coffeeType().name());
                        ps.setBigDecimal(6, item.price());
                        ps.setString(7, item.currency());
                        ps.setString(8, item.loyaltyCardId());
                        ps.setString(9, item.status().name());
                        ps.setInt(10, item.attemptCount());
                        ps.setString(11, item.lastError());
                        ps.setString(12, item.remotePaymentId());
                        ps.setBoolean(13, item.replayed());
                        if (item.leaseUntil() == null) {
                            ps.setNull(14, Types.TIMESTAMP);
                        } else {
                            ps.setTimestamp(14, Timestamp.from(item.leaseUntil()));
                        }
                        ps.setTimestamp(15, Timestamp.from(item.createdAt()));
                        ps.setTimestamp(16, Timestamp.from(item.updatedAt()));
                    }

                    @Override
                    public int getBatchSize() {
                        return items.size();
                    }
                });
    }

    /**
     * Atomically claims a batch of items for processing. Uses {@code FOR UPDATE SKIP LOCKED} so
     * multiple workers (across processes or pods) can safely lease disjoint subsets.
     */
    public List<BatchItem> leaseNextBatch(int batchSize, Instant now, Instant leaseUntil) {
        return jdbc.query(
                "UPDATE batch_item "
                        + "SET status = 'PROCESSING', "
                        + "    attempt_count = attempt_count + 1, "
                        + "    lease_until = ?, "
                        + "    updated_at = ? "
                        + "WHERE item_id IN ( "
                        + "    SELECT item_id FROM batch_item "
                        + "    WHERE status = 'PENDING' "
                        + "       OR (status = 'PROCESSING' AND lease_until < ?) "
                        + "    ORDER BY created_at "
                        + "    LIMIT ? "
                        + "    FOR UPDATE SKIP LOCKED "
                        + ") "
                        + "RETURNING item_id, request_id, store_id, idempotency_key, coffee_type, price, "
                        + "currency, loyalty_card_id, status, attempt_count, last_error, remote_payment_id, "
                        + "replayed, lease_until, created_at, updated_at",
                rowMapper(),
                Timestamp.from(leaseUntil),
                Timestamp.from(now),
                Timestamp.from(now),
                batchSize);
    }

    public void markDone(UUID itemId, String remotePaymentId, boolean replayed, Instant now) {
        jdbc.update(
                "UPDATE batch_item SET status = 'DONE', remote_payment_id = ?, replayed = ?, "
                        + "lease_until = NULL, last_error = NULL, updated_at = ? WHERE item_id = ?",
                remotePaymentId,
                replayed,
                Timestamp.from(now),
                itemId);
    }

    public void markFailed(UUID itemId, String errorMessage, Instant now) {
        jdbc.update(
                "UPDATE batch_item SET status = 'FAILED', last_error = ?, lease_until = NULL, "
                        + "updated_at = ? WHERE item_id = ?",
                truncate(errorMessage),
                Timestamp.from(now),
                itemId);
    }

    public void requeueForRetry(UUID itemId, String errorMessage, Instant now) {
        jdbc.update(
                "UPDATE batch_item SET status = 'PENDING', last_error = ?, lease_until = NULL, "
                        + "updated_at = ? WHERE item_id = ?",
                truncate(errorMessage),
                Timestamp.from(now),
                itemId);
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private static RowMapper<BatchItem> rowMapper() {
        return (rs, rowNum) -> new BatchItem(
                rs.getObject("item_id", UUID.class),
                rs.getObject("request_id", UUID.class),
                rs.getString("store_id"),
                rs.getString("idempotency_key"),
                CoffeeType.valueOf(rs.getString("coffee_type")),
                rs.getBigDecimal("price"),
                rs.getString("currency"),
                rs.getString("loyalty_card_id"),
                ItemStatus.valueOf(rs.getString("status")),
                rs.getInt("attempt_count"),
                rs.getString("last_error"),
                rs.getString("remote_payment_id"),
                rs.getBoolean("replayed"),
                toInstant(rs.getTimestamp("lease_until")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
