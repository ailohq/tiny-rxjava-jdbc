package com.trunk.rx.jdbc;

import java.sql.Connection;

import org.testng.annotations.Test;

import rx.observers.TestSubscriber;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static rx.Observable.error;
import static rx.Observable.just;

public class TransactionContextExecutorTest {
  @Test
  public void autoCommitShouldSetAutoCommitTrue() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null))
      .withAutoCommit()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, times(1)).setAutoCommit(anyBoolean());
    verify(c, times(1)).setAutoCommit(eq(true));
  }

  @Test
  public void autoCommitShouldReturnAllValues() throws Exception {
    TestSubscriber<Object> t = new TestSubscriber<>();
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(1, 2))
      .withAutoCommit()
      .subscribe(t);
    t.assertValues(1, 2);
  }

  @Test
  public void autoCommitShouldReturnAllValuesBeforeError() throws Exception {
    TestSubscriber<Object> t = new TestSubscriber<>();
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(1, 2).concatWith(error(new Exception())).concatWith(just(3)))
      .withAutoCommit()
      .subscribe(t);
    t.assertValues(1, 2);
  }

  @Test
  public void autoCommitShouldNotCallCommit() throws Exception {
    TestSubscriber<Object> t = new TestSubscriber<>();
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null))
      .withAutoCommit()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, never()).commit();
  }

  @Test
  public void autoCommitShouldNotCallRollbackOnError() throws Exception {
    TestSubscriber<Object> t = new TestSubscriber<>();
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null).concatWith(error(new Exception())))
      .withAutoCommit()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, never()).rollback();
  }

  @Test
  public void autoCommitShouldNotCallRollback() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null))
      .withAutoCommit()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, never()).rollback();
  }

  @Test
  public void autoCommitShouldCloseConnection() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c).execute(connection -> just(null, null))
      .withAutoCommit().toBlocking()
      .subscribe(o -> {}, throwable -> {});
    verify(c, times(1)).close();
  }

  @Test
  public void singleTransactionShouldSetAutoCommitFalse() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c).execute(connection -> just(null)).withSingleTransaction().toBlocking().last();
    verify(c, times(1)).setAutoCommit(anyBoolean());
    verify(c, times(1)).setAutoCommit(eq(false));
  }

  @Test
  public void singleTransactionShouldReturnAllValues() throws Exception {
    TestSubscriber<Object> t = new TestSubscriber<>();
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(1, 2))
      .withSingleTransaction()
      .subscribe(t);
    t.assertValues(1, 2);
  }

  @Test
  public void singleTransactionShouldReturnAllValuesBeforeError() throws Exception {
    TestSubscriber<Object> t = new TestSubscriber<>();
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(1, 2).concatWith(error(new Exception())).concatWith(just(3)))
      .withSingleTransaction()
      .subscribe(t);
    t.assertValues(1, 2);
  }

  @Test
  public void singleTransactionShouldCallCommitOnce() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null))
      .withSingleTransaction()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, times(1)).commit();
  }

  @Test
  public void singleTransactionShouldNotCallRollback() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null))
      .withSingleTransaction()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, never()).rollback();
  }

  @Test
  public void singleTransactionShouldNotCallCommitOnceOnError() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null).concatWith(error(new Exception())).concatWith(just(null)))
      .withSingleTransaction()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, never()).commit();
  }

  @Test
  public void singleTransactionShouldCallRollbackOnceOnErrorAndNotCallCommit() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null).concatWith(error(new Exception())).concatWith(just(null)))
      .withSingleTransaction()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, times(1)).rollback();
  }

  @Test
  public void singleTransactionShouldCloseConnection() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null))
      .withSingleTransaction()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, times(1)).close();
  }

  @Test
  public void transactionPerEventShouldSetAutoCommitFalse() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null))
      .withTransactionPerEvent()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, times(1)).setAutoCommit(anyBoolean());
    verify(c, times(1)).setAutoCommit(eq(false));
  }

  @Test
  public void transactionPerEventShouldReturnAllValues() throws Exception {
    TestSubscriber<Object> t = new TestSubscriber<>();
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(1, 2))
      .withTransactionPerEvent()
      .subscribe(t);
    t.assertValues(1, 2);
  }

  @Test
  public void transactionPerEventShouldReturnAllValuesBeforeError() throws Exception {
    TestSubscriber<Object> t = new TestSubscriber<>();
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(1, 2).concatWith(error(new Exception())).concatWith(just(3)))
      .withTransactionPerEvent()
      .subscribe(t);
    t.assertValues(1, 2);
  }

  @Test
  public void transactionPerEventShouldCallCommitPerEvent() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null))
      .withTransactionPerEvent()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, times(2)).commit();
  }

  @Test
  public void transactionPerEventShouldNotCallRollback() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null))
      .withTransactionPerEvent()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, never()).rollback();
  }

  @Test
  public void transactionPerEventShouldCallCommitForInitialNonErrorEventsOnlyOnceOnError() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null).concatWith(error(new Exception())).concatWith(just(null)))
      .withTransactionPerEvent()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, times(2)).commit();

  }

  @Test
  public void transactionPerEventShouldCallRollbackOnceOnError() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null).concatWith(error(new Exception())).concatWith(just(null)))
      .withTransactionPerEvent()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, times(1)).rollback();

  }

  @Test
  public void transactionPerEventShouldCloseConnection() throws Exception {
    Connection c = mock(Connection.class);
    ConnectionPool.of(c)
      .execute(connection -> just(null, null))
      .withTransactionPerEvent()
      .toBlocking().subscribe(o -> {}, throwable -> {});
    verify(c, times(1)).close();
  }
}
