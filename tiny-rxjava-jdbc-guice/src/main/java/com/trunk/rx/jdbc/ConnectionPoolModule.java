package com.trunk.rx.jdbc;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.netflix.archaius.api.Config;
import com.trunk.rx.jdbc.ConnectionProvider;

import javax.inject.Singleton;
import java.sql.SQLException;

public class ConnectionPoolModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ConnectionPool.class).toProvider(Provider.class);
  }

  @Singleton
  private static class Provider implements com.google.inject.Provider<ConnectionPool> {

    private final ConnectionProvider connectionProvider;

    @Inject
    public Provider(ConnectionProvider connectionProvider) throws SQLException {
      this.connectionProvider = connectionProvider;
    }

    @Override
    public ConnectionPool get() {
      return ConnectionPool.from(connectionProvider);
    }
  }
}
