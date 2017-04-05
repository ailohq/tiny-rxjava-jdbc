package com.trunk.rx.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Allows the execution of a {@link ConnectionConsumer} in a transaction context.
 * Defaults to using auto-commit transactions. It can be subscribed to as the
 * result of the given {@link ConnectionConsumer}.
 * <p>
 * TransactionContextExecutor manages the lifecycle of the connection objects it creates
 * by getting a connection from the {@link ConnectionProvider} for each subscription.
 *
 * @see #withAutoCommit()
 * @see #withSingleTransaction()
 * @see #withTransactionPerEvent()
 */
public class TransactionContextExecutor<T> extends Observable<T> {
  private static final Logger log = LoggerFactory.getLogger(TransactionContext.class);

  public static final AutoCommitTransactionContext AUTO_COMMIT_TRANSACTION_CONTEXT = new AutoCommitTransactionContext();
  public static final SingleTransactionTransactionContext SINGLE_TRANSACTION_TRANSACTION_CONTEXT = new SingleTransactionTransactionContext();
  public static final TransactionPerEventTransactionContext TRANSACTION_PER_EVENT_TRANSACTION_CONTEXT = new TransactionPerEventTransactionContext();

  private final ConnectionProvider provider;
  private final ConnectionConsumer<T> connectionConsumer;

  public TransactionContextExecutor(
    TransactionContext transactionContext,
    ConnectionProvider provider,
    ConnectionConsumer<T> connectionConsumer
  ) {
    super(subscriber -> transactionContext.f(provider, connectionConsumer).subscribe(subscriber));
    this.provider = provider;
    this.connectionConsumer = connectionConsumer;
  }

  /**
   * Execute the {@link ConnectionConsumer} using {@link Connection#setAutoCommit(boolean)} true.
   *
   * @return the result of executing {@link ConnectionConsumer#call(Connection)} with auto-commit transactions
   */
  public TransactionContextExecutor<T> withAutoCommit() {
    return new TransactionContextExecutor<>(AUTO_COMMIT_TRANSACTION_CONTEXT, provider, connectionConsumer);
  }

  /**
   * Execute the {@link ConnectionConsumer} using a single transaction that will be committed on completion.
   * On an error or early unsubscription the whole transaction will be rolled back.
   *
   * @return the result of executing {@link ConnectionConsumer#call(Connection)}
   */
  public TransactionContextExecutor<T> withSingleTransaction() {
    return new TransactionContextExecutor<>(SINGLE_TRANSACTION_TRANSACTION_CONTEXT, provider, connectionConsumer);
  }

  /**
   * Execute the {@link ConnectionConsumer} using a transaction committed
   * per event emitted from the {@link ConnectionConsumer}.
   * On an error or early unsubscription any currently running transaction will be rolled back.
   *
   * @return the result of executing {@link ConnectionConsumer#call(Connection)}
   */
  public TransactionContextExecutor<T> withTransactionPerEvent() {
    return new TransactionContextExecutor<>(TRANSACTION_PER_EVENT_TRANSACTION_CONTEXT, provider, connectionConsumer);
  }

  private static <T> Observable<T> withAutoCommit(Connection connection) {
    try {
      log.debug("With auto commit transactions");
      connection.setAutoCommit(true);
      return Observable.empty();
    } catch (SQLException e) {
      return Observable.error(e);
    }
  }

  private static <T> Observable<T> withManualTransactions(Connection connection) {
    try {
      log.debug("With manual transactions");
      connection.setAutoCommit(false);
      return Observable.empty();
    } catch (SQLException e) {
      return Observable.error(e);
    }
  }

  private static void closeConnection(Connection connection) {
    try {
      if (!connection.isClosed()) {
        log.debug("Closing connection");
        connection.close();
      }
    } catch (SQLException e) {
      log.warn("Unexpected error closing connection", e);
    }
  }

  private static void commitTransaction(Connection connection) {
    try {
      if (!connection.isClosed()) {
        log.debug("Committing transaction");
        connection.commit();
      } else {
        log.warn("Commit called on closed connection");
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static void rollBackTransaction(Connection connection) {
    try {
      if (!connection.isClosed()) {
        log.debug("Rolling-back transaction");
        connection.rollback();
      }
    } catch (SQLException rollbackError) {
      log.warn("Rollback error", rollbackError);
    }
  }

  private static void cleanupConnection(Connection connection) {
    rollBackTransaction(connection);
    closeConnection(connection);
  }

  private static Observable<Connection> autoclosingConnection(ConnectionProvider provider) {
    return Observable.using(
      provider,
      Observable::just,
      TransactionContextExecutor::closeConnection,
      true
    );
  }

  public interface TransactionContext {
    <T> Observable<T> f(ConnectionProvider provider, ConnectionConsumer<T> consumer);
  }

  public static class AutoCommitTransactionContext implements TransactionContext {
    @Override
    public <T> Observable<T> f(ConnectionProvider provider, ConnectionConsumer<T> consumer) {
      return autoclosingConnection(provider)
        .flatMap(
          c -> {
            UnclosableConnection unclosableConnection = new UnclosableConnection(c);
            return TransactionContextExecutor.<T>withAutoCommit(c)
              .concatWith(consumer.call(unclosableConnection));
          }
        );
    }
  }

  public static class SingleTransactionTransactionContext implements TransactionContext {
    @Override
    public <T> Observable<T> f(ConnectionProvider provider, ConnectionConsumer<T> consumer) {
      return autoclosingConnection(provider)
        .flatMap(
          c -> {
            UnclosableConnection unclosableConnection = new UnclosableConnection(c);
            return TransactionContextExecutor.<T>withManualTransactions(c)
              .concatWith(consumer.call(unclosableConnection))
              .doOnCompleted(() -> commitTransaction(c))
              .doOnError(e -> rollBackTransaction(c))
              .doOnUnsubscribe(() -> cleanupConnection(c))
              .finallyDo(() -> closeConnection(c));
          }
        );
    }
  }

  public static class TransactionPerEventTransactionContext implements TransactionContext {
    @Override
    public <T> Observable<T> f(ConnectionProvider provider, ConnectionConsumer<T> consumer) {
      return autoclosingConnection(provider)
        .flatMap(
          c -> {
            UnclosableConnection unclosableConnection = new UnclosableConnection(c);
            return TransactionContextExecutor.<T>withManualTransactions(c)
              .concatWith(consumer.call(unclosableConnection))
              .doOnNext(t -> commitTransaction(c))
              .doOnError(e -> rollBackTransaction(c))
              .doOnUnsubscribe(() -> cleanupConnection(c))
              .finallyDo(() -> closeConnection(c));
          }
        );
    }
  }
}
