package com.trunk.rx.jdbc.h2;

import com.trunk.rx.jdbc.ConnectionProvider;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A native H2 ConnectionProvider using {@link JdbcConnectionPool}
 */
public class H2ConnectionProvider implements ConnectionProvider {
  private static final Logger log = LoggerFactory.getLogger(H2ConnectionProvider.class);

  private final JdbcConnectionPool connectionPool;

  public H2ConnectionProvider() {
    this("H2ConnectionPoolProvider");
  }

  public H2ConnectionProvider(String name) {
    try {
      Class.forName("org.h2.Driver");
    } catch (ClassNotFoundException e) {
      log.error("Error loading class for H2 JDBC driver", e);
      throw new RuntimeException(e);
    }
    connectionPool =  JdbcConnectionPool.create("jdbc:h2:mem:" + name, "sa", "sa");
  }

  @Override
  public Connection call() {
    try {
      return connectionPool.getConnection();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws Exception {
    connectionPool.dispose();
  }
}
