package com.trunk.rx.jdbc;

import org.testng.annotations.Test;
import rx.Observable;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertSame;

public class ConnectionPoolTest {
  @Test
  public void shouldExecuteWithGivenConnectionPoolProvider() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionProvider cpp = mock(ConnectionProvider.class);
    when(cpp.get()).thenReturn(Observable.just(c));
    ConnectionPool p = ConnectionPool.from(cpp);
    AtomicReference<Connection> connectionReturned = new AtomicReference<>();
    p.execute(connection -> {
      connectionReturned.set(connection);
      return Observable.just(null);
    }).toBlocking().single();
    assertSame(((UnclosableConnection)connectionReturned.get()).getDelegate(), c);
  }

  @Test
  public void shouldExecuteWithAutoCommitAsDefault() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool p = ConnectionPool.of(c);
    p.execute(connection -> Observable.just(null)).toBlocking().single();
    verify(c, times(1)).setAutoCommit(anyBoolean());
    verify(c, times(1)).setAutoCommit(eq(true));
  }

  @Test
  public void connectionPoolOfShouldReturnProviderWithGivenConnection() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool p = ConnectionPool.of(c);
    AtomicReference<Connection> connectionReturned = new AtomicReference<>();
    p.execute(connection -> {
      connectionReturned.set(connection);
      return Observable.just(null);
    }).toBlocking().single();
    // nasty - see comments in code
    assertSame(((UnclosableConnection)((UnclosableConnection)connectionReturned.get()).getDelegate()).getDelegate(), c);
  }

  @Test
  public void connectionPoolOfShouldCloseConnectionOnClose() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool p = ConnectionPool.of(c);
    p.close();
    verify(c, times(1)).close();
  }
}
