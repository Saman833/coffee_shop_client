package com.coffeeshop.backend.config;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource coordinatorDataSource(CoordinatorProperties properties) {
        return buildDataSource(properties.url(), properties.username(), properties.password(), "coordinator-pool");
    }

    @Bean
    @Primary
    public JdbcTemplate coordinatorJdbcTemplate(DataSource coordinatorDataSource) {
        return new JdbcTemplate(coordinatorDataSource);
    }

    @Bean
    @Primary
    public PlatformTransactionManager coordinatorTransactionManager(DataSource coordinatorDataSource) {
        return new DataSourceTransactionManager(coordinatorDataSource);
    }

    @Bean
    public ShardDataSources shardDataSources(ShardingProperties properties) {
        if (properties.instances().size() != properties.count()) {
            throw new IllegalStateException(
                    "coffeeshop.sharding.count=" + properties.count()
                            + " does not match number of configured instances=" + properties.instances().size());
        }
        List<DataSource> sources = new ArrayList<>(properties.instances().size());
        int index = 0;
        for (ShardingProperties.ShardInstance instance : properties.instances()) {
            sources.add(buildDataSource(instance.url(), instance.username(), instance.password(),
                    "shard-pool-" + index++));
        }
        return new ShardDataSources(sources);
    }

    @Bean
    public ShardJdbcTemplates shardJdbcTemplates(ShardDataSources shardDataSources) {
        List<JdbcTemplate> templates = shardDataSources.all().stream()
                .map(JdbcTemplate::new)
                .toList();
        return new ShardJdbcTemplates(templates);
    }

    /**
     * Runs Flyway migrations against the coordinator and every shard datasource on startup.
     * Multiple datasources mean we cannot rely on Spring Boot's single-datasource Flyway autoconfig.
     */
    @Bean
    public ApplicationRunner flywayMigrator(DataSource coordinatorDataSource, ShardDataSources shardDataSources) {
        return args -> {
            Flyway.configure()
                    .dataSource(coordinatorDataSource)
                    .locations("classpath:db/migration/coordinator")
                    .load()
                    .migrate();
            for (DataSource shard : shardDataSources.all()) {
                Flyway.configure()
                        .dataSource(shard)
                        .locations("classpath:db/migration/shard")
                        .load()
                        .migrate();
            }
        };
    }

    private static DataSource buildDataSource(String url, String username, String password, String poolName) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setPoolName(poolName);
        ds.setMaximumPoolSize(5);
        return ds;
    }
}
