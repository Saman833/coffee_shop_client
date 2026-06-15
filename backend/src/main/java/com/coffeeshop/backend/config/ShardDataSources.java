package com.coffeeshop.backend.config;

import java.util.List;

import javax.sql.DataSource;

/**
 * Wrapper bean exposing the per-shard {@link DataSource}s without colliding with the coordinator
 * datasource during dependency injection.
 */
public final class ShardDataSources {

    private final List<DataSource> dataSources;

    public ShardDataSources(List<DataSource> dataSources) {
        this.dataSources = List.copyOf(dataSources);
    }

    public List<DataSource> all() {
        return dataSources;
    }

    public int count() {
        return dataSources.size();
    }
}
