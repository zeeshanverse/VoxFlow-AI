package com.voxflow.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Builds the PostgreSQL connection pool from DB_URL / DB_USERNAME / DB_PASSWORD.
 * DB_URL may be a JDBC URL or a provider-style postgresql:// URI (as copied from Neon);
 * DbUrlParser normalizes both, so a wrong URL format can no longer stop the app from starting.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    DataSource dataSource(
            @Value("${voxflow.db.url}") String rawUrl,
            @Value("${voxflow.db.username}") String username,
            @Value("${voxflow.db.password}") String password
    ) {
        DbUrlParser.Result db = DbUrlParser.parse(rawUrl, username, password);

        // Log the target without credentials, so Render logs show what was actually used.
        log.info("Database target: {}", db.jdbcUrl());

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(db.jdbcUrl());
        config.setUsername(db.username());
        config.setPassword(db.password());

        // Small pool for free-tier hosting; Neon suspends idle connections,
        // so retire them before the server does.
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(0);
        config.setMaxLifetime(300_000);
        config.setIdleTimeout(120_000);
        config.setConnectionTimeout(30_000);

        return new HikariDataSource(config);
    }
}