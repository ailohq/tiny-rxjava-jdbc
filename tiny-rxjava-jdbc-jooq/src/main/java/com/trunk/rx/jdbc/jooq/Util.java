package com.trunk.rx.jdbc.jooq;

import org.jooq.Cursor;
import org.jooq.Query;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods
 */
public class Util {
  private static final Logger log = LoggerFactory.getLogger(Util.class);

  /**
   * Close the cursor quietly
   */
  public static <R extends Record> void closeQuietly(Cursor<R> cursor) {
    try {
      if (cursor != null && !cursor.isClosed()) {
        cursor.close();
        log.debug("closed {}", cursor);
      }
    } catch (Exception e) {
      log.debug(e.getMessage(), e);
    }
  }

  /**
   * Cancel and close the query quietly.
   */
  public static void closeQuietly(Query query) {
    try {
      if (query != null) {
        query.cancel();
        log.debug("cancelled {}", query);
        query.close();
        log.debug("closed {}", query);
      }
    } catch (Exception e) {
      log.debug(e.getMessage(), e);
    }
  }

}
