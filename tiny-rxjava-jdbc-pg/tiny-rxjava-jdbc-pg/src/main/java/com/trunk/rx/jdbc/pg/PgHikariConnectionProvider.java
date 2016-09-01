package com.trunk.rx.jdbc.pg;

import com.trunk.rx.jdbc.ConnectionProvider;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A native PostgreSQL ConnectionProvider using {@link com.zaxxer.hikari.HikariDataSource}
 */
public class PgHikariConnectionProvider implements ConnectionProvider {
  private static final Logger log = LoggerFactory.getLogger(PgHikariConnectionProvider.class);

  private final HikariDataSource dataSource;

  public PgHikariConnectionProvider(String host, String database, String username, String password, int maxConnections) throws SQLException {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      log.error("Error loading class for PostgreSQL JDBC driver", e);
      throw new RuntimeException(e);
    }
    dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(String.format("jdbc:postgresql://%s/%s", host, database));
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.setMaximumPoolSize(maxConnections);
  }

  @Override
  public Observable<Connection> get() {
    return Observable.defer(
      () -> {
        try {
          return Observable.just(dataSource.getConnection());
        } catch (SQLException e) {
          return Observable.error(e);
        }
      }
    );
  }

  @Override
  public void close() throws Exception {
    dataSource.close();
  }
}
