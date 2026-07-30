package com.codesphere.gameon.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Manages the HikariCP connection pool and Flyway migrations.
 */
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private final HikariDataSource dataSource;
    private final AppConfig appConfig;

    public DatabaseConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.dataSource = createDataSource();
    }

    /**
     * Returns the shared connection pool DataSource.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Runs all pending Flyway migrations against the configured database.
     */
    public void runMigrations() {
        logger.info("Running database migrations...");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        logger.info("Database migrations complete.");
    }

    /**
     * Closes the connection pool. Call on application shutdown.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed.");
        }
    }

    private HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(appConfig.getJdbcUrl());
        config.setUsername(appConfig.getDbUser());
        config.setPassword(appConfig.getDbPassword());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setPoolName("GameOnPool");
        return new HikariDataSource(config);
    }
}
