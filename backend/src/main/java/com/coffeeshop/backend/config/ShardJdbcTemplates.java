package com.coffeeshop.backend.config;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wrapper bean exposing the per-shard JdbcTemplates.
 * Indexed by shard number (0-based). Resolved via {@link com.coffeeshop.backend.payments.ShardResolver}.
 */
public final class ShardJdbcTemplates {

    private final List<JdbcTemplate> templates;

    public ShardJdbcTemplates(List<JdbcTemplate> templates) {
        this.templates = List.copyOf(templates);
    }

    public JdbcTemplate forShard(int shardIndex) {
        if (shardIndex < 0 || shardIndex >= templates.size()) {
            throw new IllegalArgumentException("Shard index out of range: " + shardIndex);
        }
        return templates.get(shardIndex);
    }

    public int count() {
        return templates.size();
    }

    public List<JdbcTemplate> all() {
        return templates;
    }
}
