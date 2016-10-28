/**
 * Copywrite https://github.com/davidmoten/rxjava-jdbc/blob/master/LICENSE
 */
package com.trunk.rx.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods.
 */
public final class Util {

  /**
   * Private constructor to prevent instantiation.
   */
  private Util() {
    // prevent instantiation
  }

  private static final Logger log = LoggerFactory.getLogger(Util.class);

  /**
   * Closes a {@link Connection} and logs exceptions without throwing. Does
   * nothing if connection is null.
   *
   * @param connection
   */
  public static void closeQuietly(Connection connection) {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
        log.debug("closed {}", connection);
      }
    } catch (SQLException e) {
      log.debug(e.getMessage(), e);
    } catch (RuntimeException e) {
      log.debug(e.getMessage(), e);
    }
  }

  /**
   * Cancels then closes a {@link PreparedStatement} and logs exceptions without throwing. Does nothing if ps is null.
   *
   * @param ps
   */
  public static void closeQuietly(PreparedStatement ps) {
    try {
      boolean isClosed;
      try {
        if (ps != null) {
          isClosed = ps.isClosed();
        } else {
          isClosed = true;
        }
      } catch (SQLException e) {
        log.debug(e.getMessage());
        isClosed = true;
      }
      if (ps != null && !isClosed) {
        try {
          ps.cancel();
          log.debug("cancelled {}", ps);
        } catch (SQLException e) {
          log.debug(e.getMessage());
        }
        ps.close();
        log.debug("closed {}", ps);
      }
    } catch (SQLException e) {
      log.debug(e.getMessage(), e);
    } catch (RuntimeException e) {
      log.debug(e.getMessage(), e);
    }
  }

  /**
   * Closes a {@link ResultSet} and logs exceptions without throwing.
   *
   * @param rs
   */
  public static void closeQuietly(ResultSet rs) {
    try {
      if (rs != null && !rs.isClosed()) {
        rs.close();
        log.debug("closed {}", rs);
      }
    } catch (SQLException e) {
      log.debug(e.getMessage(), e);
    } catch (RuntimeException e) {
      log.debug(e.getMessage(), e);
    }
  }
}
