package com.trunk.rx.jdbc;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.archaius.guice.ArchaiusModule;
import com.trunk.rx.jdbc.pg.PgConnectionProviderModule;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ConnectionPoolModuleTest {
  @Test
  public void shouldCreateModule() throws Exception {
    Injector injector = Guice.createInjector(new ArchaiusModule(), new ConnectionPoolModule(), new PgConnectionProviderModule());
    assertEquals(injector.getProvider(ConnectionPool.class).get().getClass(), ConnectionPool.class);
  }
}