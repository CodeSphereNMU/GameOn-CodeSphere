package com.codesphere.gameon.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base class for all DAO implementations.
 * Provides access to the shared DataSource (connection pool).
 *
 * Subclasses should:
 * - Use try-with-resources for Connection, PreparedStatement, and ResultSet
 * - Use parameterised queries (never concatenate user input into SQL)
 * - Map ResultSet rows to domain model objects
 */
public abstract class BaseDao {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final DataSource dataSource;

    protected BaseDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Obtains a connection from the pool.
     * Always use inside try-with-resources to ensure it is returned to the pool.
     */
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
