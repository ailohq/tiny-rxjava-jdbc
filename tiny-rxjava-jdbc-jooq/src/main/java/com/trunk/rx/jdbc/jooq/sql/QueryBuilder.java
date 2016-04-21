package com.trunk.rx.jdbc.jooq.sql;

import java.sql.Connection;

import org.jooq.Query;

/**
 * A functional interface to defer the creation of
 * {@link Query Queries} until needed.
 *
 * @see Execute
 * @see Select
 */
@FunctionalInterface
public interface QueryBuilder<T extends Query> {
  T build(Connection connection);
}
