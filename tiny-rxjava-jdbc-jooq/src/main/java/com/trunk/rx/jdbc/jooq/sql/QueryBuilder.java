package com.trunk.rx.jdbc.jooq.sql;

import org.jooq.Query;

import java.sql.Connection;

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
