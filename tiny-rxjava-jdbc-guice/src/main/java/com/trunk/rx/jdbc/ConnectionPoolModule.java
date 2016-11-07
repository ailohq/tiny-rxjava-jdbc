package com.trunk.rx.jdbc;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;

import javax.inject.Singleton;
import java.sql.SQLException;

public class ConnectionPoolModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ConnectionPool.class).toProvider(Provider.class).in(Scopes.SINGLETON);
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
