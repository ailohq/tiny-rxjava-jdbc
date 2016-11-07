package com.trunk.rx.jdbc.pg;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.netflix.archaius.guice.ArchaiusModule;
import com.trunk.rx.jdbc.ConnectionProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PgHikariConnectionProviderModuleTest {
  @Test
  public void shouldCreateModule() throws Exception {
    Injector injector = Guice.createInjector(new ArchaiusModule(), new PgHikariConnectionProviderModule());
    assertEquals(injector.getProvider(ConnectionProvider.class).get().getClass(), PgHikariConnectionProvider.class);
  }

  @Test(expectedExceptions = ProvisionException.class)
  public void shouldFailWithMissingHost() throws Exception {
    Injector injector = Guice.createInjector(
      new ArchaiusModule() {
        @Override
        protected void configureArchaius() {
          bindConfigurationName().toInstance("fail");
        }
      },
      new PgHikariConnectionProviderModule()
    );
    assertEquals(injector.getProvider(ConnectionProvider.class).get().getClass(), PgHikariConnectionProvider.class);
  }
}