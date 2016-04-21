package com.trunk.rx.jdbc.h2;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.testng.annotations.Test;

import rx.observers.TestSubscriber;

public class H2ConnectionProviderTest {
  @Test
  public void shouldCreateValidPool() throws Exception {
    try (H2ConnectionProvider pool = new H2ConnectionProvider("H2ConnectionPoolProviderTest-shouldCreateValidPool")) {
      pool.get().subscribe(connection -> {
        try {
          connection.prepareStatement("CREATE TABLE test (id int)").execute();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      });

      TestSubscriber<Integer> tInsert = new TestSubscriber<>();
      pool.get().map(
        connection -> {
          try {
            return connection.prepareStatement("INSERT INTO test VALUES (7)").executeUpdate();
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        })
      .subscribe(tInsert);

      tInsert.assertValues(1);

      TestSubscriber<Integer> tSelect = new TestSubscriber<>();
      pool.get().map(
        connection -> {
          try {
            ResultSet resultSet = connection.prepareStatement("SELECT * FROM test").executeQuery();
            resultSet.next();
            return resultSet.getInt(1);
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        })
      .subscribe(tSelect);

      tSelect.assertValues(7);
    }
  }
}
