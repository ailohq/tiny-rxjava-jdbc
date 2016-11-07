package com.trunk.rx.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides a light weight DSL to convert {@link ConnectionProvider}s into
 * {@link TransactionContextExecutor}s using given {@link ConnectionConsumer}s.
 */
public class ConnectionPool implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);

  private final ConnectionProvider provider;

  /**
   * Create a ConnectionPool from the given {@link ConnectionProvider}
   *
   * @param provider the ConnectionProvider to use for this ConnectionPool
   * @return a new ConnectionPool
   */
  public static ConnectionPool from(ConnectionProvider provider) {
    return new ConnectionPool(provider);
  }

  /**
   * Create a ConnectionPool that will return the same Connection. The connection will remain open
   * until {@link #close()} is called.
   *
   * @param connection the connection to use for {@link #execute(ConnectionConsumer)}
   * @return a new ConnectionPool
   */
  public static ConnectionPool of(Connection connection) {
    return new ConnectionPool(new ConnectionProvider() {
      @Override
      public Connection call() {
        // we have to wrap the connection so it stays open when executed
        // even if it gets wrapped again when executed
        return new UnclosableConnection(connection);
      }

      @Override
      public void close() {
        try {
          connection.close();
        } catch (SQLException e) {
          log.warn("Error closing connection", e);
        }
      }
    });
  }

  private ConnectionPool(ConnectionProvider provider) {
    this.provider = provider;
  }

  public <T> TransactionContextExecutor<T> execute(ConnectionConsumer<T> consumer) {
    return new TransactionContextExecutor<>(TransactionContextExecutor.AUTO_COMMIT_TRANSACTION_CONTEXT, provider, consumer);
  }

  @Override
  public void close() throws Exception {
    provider.close();
  }
}
