package com.trunk.rx.jdbc.pg;

import java.sql.Connection;
import java.sql.SQLException;

import org.postgresql.ds.PGPoolingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trunk.rx.jdbc.ConnectionProvider;

import rx.Observable;

/**
 * A native PostgreSQL ConnectionProvider using {@link PGPoolingDataSource}
 */
public class PgConnectionProvider implements ConnectionProvider {
  private static final Logger log = LoggerFactory.getLogger(PgConnectionProvider.class);

  private final PGPoolingDataSource dataSource;

  public PgConnectionProvider(String host, String database, String username, String password, int maxConnections) throws SQLException {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      log.error("Error loading class for PostgreSQL JDBC driver", e);
      throw new RuntimeException(e);
    }
    dataSource = new PGPoolingDataSource();
    dataSource.setUrl(String.format("jdbc:postgresql://%s/%s", host, database));
    dataSource.setProperty("user", username);
    dataSource.setProperty("password", password);
    dataSource.setMaxConnections(maxConnections);
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
