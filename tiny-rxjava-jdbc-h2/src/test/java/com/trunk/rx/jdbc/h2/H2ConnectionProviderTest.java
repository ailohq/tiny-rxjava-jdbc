package com.trunk.rx.jdbc.h2;

import com.trunk.rx.jdbc.ConnectionPool;
import org.testng.annotations.Test;
import rx.observers.TestSubscriber;

import java.sql.ResultSet;
import java.sql.SQLException;

import static rx.Observable.just;

public class H2ConnectionProviderTest {
  @Test
  public void shouldCreateValidPool() throws Exception {
    try (H2ConnectionProvider provider = new H2ConnectionProvider("H2ConnectionPoolProviderTest-shouldCreateValidPool")) {
      ConnectionPool.from(provider)
        .execute(
          connection -> {
            try {
              return just(connection.prepareStatement("CREATE TABLE test (id int)").execute());
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        )
        .subscribe();

      TestSubscriber<Integer> tInsert = new TestSubscriber<>();
      ConnectionPool.from(provider)
        .execute(
          connection -> {
            try {
              return just(connection.prepareStatement("INSERT INTO test VALUES (7)").executeUpdate());
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        )
      .subscribe(tInsert);

      tInsert.assertValues(1);

      TestSubscriber<Integer> tSelect = new TestSubscriber<>();
      ConnectionPool.from(provider)
        .execute(
          connection -> {
            try {
              ResultSet resultSet = connection.prepareStatement("SELECT * FROM test").executeQuery();
              resultSet.next();
              return just(resultSet.getInt(1));
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          })
        .subscribe(tSelect);

      tSelect.assertValues(7);
    }
  }
}
