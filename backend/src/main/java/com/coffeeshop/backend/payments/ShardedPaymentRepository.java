package com.coffeeshop.backend.payments;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.coffeeshop.backend.config.ShardJdbcTemplates;

/**
 * Persists payments across multiple shard databases.
 * <p>
 * Writes are routed to the shard that owns the routing key (store_id + idempotency_key) so an
 * idempotent re-submit always lands on the same shard and hits the unique constraint.
 */
@Repository
public class ShardedPaymentRepository {

    private final ShardResolver shardResolver;
    private final ShardJdbcTemplates shardTemplates;

    public ShardedPaymentRepository(ShardResolver shardResolver, ShardJdbcTemplates shardTemplates) {
        this.shardResolver = shardResolver;
        this.shardTemplates = shardTemplates;
    }

    /**
     * Inserts the payment if no row exists yet for (store_id, idempotency_key).
     *
     * @return {@code true} when a new row was created, {@code false} when the unique constraint
     *         caught an idempotent replay.
     */
    public boolean saveIfAbsent(Payment payment) {
        int shard = shardResolver.resolveShard(routingKey(payment.storeId(), payment.idempotencyKey()));
        JdbcTemplate jdbc = shardTemplates.forShard(shard);
        try {
            jdbc.update(
                    "INSERT INTO payments (payment_id, store_id, coffee_type, price, currency, "
                            + "loyalty_card_id, idempotency_key, remote_payment_id, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    payment.paymentId(),
                    payment.storeId(),
                    payment.coffeeType().name(),
                    payment.price(),
                    payment.currency(),
                    payment.loyaltyCardId(),
                    payment.idempotencyKey(),
                    payment.remotePaymentId(),
                    Timestamp.from(payment.createdAt()));
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    public Optional<Payment> findByRoutingKey(String storeId, String idempotencyKey) {
        int shard = shardResolver.resolveShard(routingKey(storeId, idempotencyKey));
        JdbcTemplate jdbc = shardTemplates.forShard(shard);
        List<Payment> rows = jdbc.query(
                "SELECT payment_id, store_id, coffee_type, price, currency, loyalty_card_id, "
                        + "idempotency_key, remote_payment_id, created_at "
                        + "FROM payments WHERE store_id = ? AND idempotency_key = ?",
                rowMapper(),
                storeId,
                idempotencyKey);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public long countAll() {
        long total = 0;
        for (JdbcTemplate jdbc : shardTemplates.all()) {
            Long count = jdbc.queryForObject("SELECT COUNT(*) FROM payments", Long.class);
            if (count != null) {
                total += count;
            }
        }
        return total;
    }

    public long countOnShard(int shardIndex) {
        Long count = shardTemplates.forShard(shardIndex)
                .queryForObject("SELECT COUNT(*) FROM payments", Long.class);
        return count == null ? 0 : count;
    }

    private static String routingKey(String storeId, String idempotencyKey) {
        return storeId + "|" + idempotencyKey;
    }

    private static RowMapper<Payment> rowMapper() {
        return (rs, rowNum) -> new Payment(
                rs.getObject("payment_id", UUID.class),
                rs.getString("store_id"),
                CoffeeType.valueOf(rs.getString("coffee_type")),
                rs.getBigDecimal("price"),
                rs.getString("currency"),
                rs.getString("loyalty_card_id"),
                rs.getString("idempotency_key"),
                rs.getString("remote_payment_id"),
                toInstant(rs.getTimestamp("created_at"))
        );
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
