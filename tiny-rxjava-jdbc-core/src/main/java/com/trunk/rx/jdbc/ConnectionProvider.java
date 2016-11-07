package com.trunk.rx.jdbc;

import rx.functions.Func0;

import java.sql.Connection;

/**
 * Provide {@link Connection}s for use in querying. This can be used to wrap
 * specific connection pool implementations.
 */
public interface ConnectionProvider extends AutoCloseable, Func0<Connection> {
}
