package com.trunk.rx.jdbc;

import rx.Observable;

import java.sql.Connection;

/**
 * A function to convert a {@link Connection}s to an {@link Observable} of Ts.
 *
 * Consumers do not need to close the connection when this is used in
 * {@link ConnectionPool#execute(ConnectionConsumer)} and {@link TransactionContextExecutor}.
 * Calling code may close the Connection - Connections used is from classes are protected from being closed by
 * the {@link UnclosableConnection}.
 */
@FunctionalInterface
public interface ConnectionConsumer<T> {
  Observable<T> call(Connection connection);
}
