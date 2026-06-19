package com.coffeeshop.backend.payments;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.coffeeshop.backend.config.ShardingProperties;

/**
 * Deterministic hash-modulo shard router.
 * <p>
 * For a given routing key (store_id + idempotency_key in our case) the same shard index is always
 * produced, so payments for the same logical entity stay co-located on a single shard.
 * The number of shards is fixed at startup; resharding is not implemented.
 */
@Component
public class ShardResolver {

    private final int shardCount;

    public ShardResolver(ShardingProperties properties) {
        this.shardCount = properties.count();
    }

    public int resolveShard(String routingKey) {
        if (routingKey == null) {
            throw new IllegalArgumentException("Routing key must not be null");
        }
        // Use 32-bit FNV-1a for determinism that does not depend on JVM hashCode strategy.
        int hash = fnv1a(routingKey);
        return Math.floorMod(hash, shardCount);
    }

    public int shardCount() {
        return shardCount;
    }

    private static int fnv1a(String value) {
        final int prime = 0x01000193;
        int hash = 0x811C9DC5;
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xFF);
            hash *= prime;
        }
        return hash;
    }
}
