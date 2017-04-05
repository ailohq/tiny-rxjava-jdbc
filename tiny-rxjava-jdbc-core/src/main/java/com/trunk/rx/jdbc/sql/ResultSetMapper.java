package com.trunk.rx.jdbc.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Map a {@link ResultSet} to a typed object. This is needed since {@link ResultSet}s
 * do not allow concurrent access.
 *
 * @param <T> type of the result
 * @see ExecuteQuery
 */
@FunctionalInterface
public interface ResultSetMapper<T> {
  T f(ResultSet resultSet) throws SQLException;
}
