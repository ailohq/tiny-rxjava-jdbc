package com.trunk.rx.jdbc.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A function to defer the creation of {@link PreparedStatement}s so we can
 * safely manage their lifecycle.
 *
 * @see Execute
 * @see ExecuteQuery
 * @see ExecuteUpdate
 */
@FunctionalInterface
public interface PreparedStatementBuilder {
  /**
   * Return a new {@link PreparedStatement} from the given connection
   *
   * @param connection the JDBC connection to use when creating the {@link PreparedStatement}
   * @return a new PreparedStatement
   * @throws SQLException
   */
  PreparedStatement build(Connection connection) throws SQLException;
}
