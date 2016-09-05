package com.trunk.rx.jdbc;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.netflix.archaius.guice.ArchaiusModule;
import com.trunk.rx.jdbc.ConnectionPoolModule;
import com.trunk.rx.jdbc.ConnectionProvider;
import com.trunk.rx.jdbc.pg.PgConnectionProvider;
import com.trunk.rx.jdbc.pg.PgConnectionProviderModule;
import com.trunk.rx.jdbc.pg.PgHikariConnectionProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ConnectionPoolModuleTest {
  @Test
  public void shouldCreateModule() throws Exception {
    Injector injector = Guice.createInjector(new ArchaiusModule(), new ConnectionPoolModule(), new PgConnectionProviderModule());
    assertEquals(injector.getProvider(ConnectionPool.class).get().getClass(), ConnectionPool.class);
  }
}