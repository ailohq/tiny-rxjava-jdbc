package com.trunk.rx.jdbc.pg;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.netflix.archaius.api.Config;
import com.trunk.rx.jdbc.ConnectionProvider;

import javax.inject.Singleton;
import java.sql.SQLException;

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
      String host = configuration.getString(Keys.DATABASE_HOST);
      String database = configuration.getString(Keys.DATABASE_DATABASE);
      String username = configuration.getString(Keys.DATABASE_USERNAME);
      String password = configuration.getString(Keys.DATABASE_PASSWORD);
      int maxConnections = configuration.getInteger(Keys.DATABASE_MAX_CONNECTIONS, 4);
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
