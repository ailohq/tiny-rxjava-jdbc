package com.trunk.rx.jdbc.pg;

import java.sql.SQLException;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.netflix.archaius.api.Config;
import com.trunk.rx.jdbc.ConnectionProvider;

public class PgHikariConnectionProviderModule extends AbstractModule {


  @Override
  protected void configure() {
    bind(ConnectionProvider.class).toProvider(Provider.class).in(Scopes.SINGLETON);
  }

  @Singleton
  private static class Provider implements com.google.inject.Provider<ConnectionProvider> {

    private final ConnectionProvider connectionProvider;

    @Inject
    public Provider(Config configuration) throws SQLException {
      String host = configuration.getString("database_host");
      String database = configuration.getString("database_database");
      String username = configuration.getString("database_username");
      String password = configuration.getString("database_password");
      int maxConnections = configuration.getInteger("database_maxConnections", 25);
      connectionProvider = new PgHikariConnectionProvider(
          host,
          database,
          username,
          password,
          maxConnections
      );
    }

    @Override
    public ConnectionProvider get() {
      return connectionProvider;
    }
  }
}
