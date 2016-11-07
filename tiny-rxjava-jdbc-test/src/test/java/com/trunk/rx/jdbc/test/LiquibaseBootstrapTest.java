package com.trunk.rx.jdbc.test;

import com.trunk.rx.jdbc.ConnectionPool;
import com.trunk.rx.jdbc.ConnectionProvider;
import com.trunk.rx.jdbc.h2.H2ConnectionProvider;
import com.trunk.rx.jdbc.sql.ExecuteQuery;
import org.testng.annotations.Test;
import rx.observers.TestSubscriber;

public class LiquibaseBootstrapTest {
  @Test
  public void shouldBootstrapFromMigrations() throws Exception {
    ConnectionProvider provider =
      LiquibaseBootstrap.using(new H2ConnectionProvider("LiquibaseBootstrapConnectionPoolProviderTest-shouldBootstrapFromMigrations"));

    TestSubscriber<Integer> tSelect = new TestSubscriber<>();
    ConnectionPool.from(provider)
      .execute(
        connection ->
          ExecuteQuery.using(connection, c -> c.prepareStatement("SELECT COUNT(*) from test"), rs -> rs.getInt(1))
      )
      .subscribe(tSelect);


    tSelect.assertValues(0);
  }

  @Test
  public void shouldBootstrapAdditionalFiles() throws Exception {
    ConnectionProvider provider =
      LiquibaseBootstrap.using(new H2ConnectionProvider("LiquibaseBootstrapConnectionPoolProviderTest-shouldBootstrapAdditionalFiles"), "named_migration.xml");

    TestSubscriber<Integer> tSelect = new TestSubscriber<>();
    ConnectionPool.from(provider)
      .execute(
        connection ->
          ExecuteQuery.using(connection, c -> c.prepareStatement("SELECT * from test"), rs -> rs.getInt(1))
      )
      .subscribe(tSelect);


    tSelect.assertValues(1, 2, 3);
  }
}
