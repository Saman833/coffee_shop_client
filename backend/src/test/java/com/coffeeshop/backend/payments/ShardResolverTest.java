package com.coffeeshop.backend.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.coffeeshop.backend.config.ShardingProperties;
import com.coffeeshop.backend.config.ShardingProperties.ShardInstance;

class ShardResolverTest {

    @Test
    void mapsKeysIntoConfiguredRange() {
        ShardResolver resolver = newResolver(4);

        for (int i = 0; i < 100; i++) {
            int shard = resolver.resolveShard("store-1|key-" + i);
            assertThat(shard).isBetween(0, 3);
        }
    }

    @Test
    void isDeterministicForTheSameRoutingKey() {
        ShardResolver resolver = newResolver(8);
        String key = "store-7|" + UUID.randomUUID();

        int first = resolver.resolveShard(key);
        for (int i = 0; i < 10; i++) {
            assertThat(resolver.resolveShard(key)).isEqualTo(first);
        }
    }

    @Test
    void distributesAcrossAllShardsForTypicalLoad() {
        ShardResolver resolver = newResolver(4);
        Set<Integer> shardsHit = new HashSet<>();

        for (int i = 0; i < 200; i++) {
            shardsHit.add(resolver.resolveShard("store-A|" + UUID.randomUUID()));
        }

        assertThat(shardsHit).containsExactlyInAnyOrder(0, 1, 2, 3);
    }

    @Test
    void rejectsNullRoutingKey() {
        ShardResolver resolver = newResolver(2);
        assertThatThrownBy(() -> resolver.resolveShard(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposesConfiguredShardCount() {
        assertThat(newResolver(3).shardCount()).isEqualTo(3);
    }

    private static ShardResolver newResolver(int count) {
        List<ShardInstance> instances = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            instances.add(new ShardInstance("jdbc:postgresql://localhost/x" + i, "u", "p"));
        }
        return new ShardResolver(new ShardingProperties(count, instances));
    }
}
