package com.trunk.rx.jdbc;

import rx.Observable;

import java.sql.Connection;

/**
 * A function to hoist {@link Connection}s into {@link Observable}s. This can be used to wrap
 * specific connection pool implementations.
 */
public interface ConnectionProvider extends AutoCloseable {
  Observable<Connection> get();
}
