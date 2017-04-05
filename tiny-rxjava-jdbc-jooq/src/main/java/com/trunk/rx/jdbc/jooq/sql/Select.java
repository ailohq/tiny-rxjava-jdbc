package com.trunk.rx.jdbc.jooq.sql;

import com.trunk.rx.jdbc.sql.ExecuteQuery;
import org.jooq.Cursor;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.ResultQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

import java.sql.Connection;

import static com.trunk.rx.jdbc.jooq.Util.closeQuietly;

/**
 * An {@link Observable} that wraps {@link ResultQuery#fetchLazy()} and emits an
 * event for row of the returned {@link Cursor}. Each row is unmarshalled using
 * the given {@link RecordMapper}.
 * <p>
 * The Cursor will be closed and the Query canceled if the subscriber unsubscribes
 * before completion.
 * <p>
 * This manages the lifecycle of the Query and Cursor, and does not close the {@link Connection}.
 */
public class Select<R extends Record, T> extends Observable<T> {
  private static final Logger log = LoggerFactory.getLogger(ExecuteQuery.class);

  public static <R extends Record, T> Select<R, T> using(Connection connection,
                                                         QueryBuilder<ResultQuery<? extends R>> queryBuilder,
                                                         RecordMapper<? super R, ? extends T> recordMapper) {
    return new Select<>(connection, queryBuilder, recordMapper);
  }

  private Select(
    Connection connection,
    QueryBuilder<ResultQuery<? extends R>> queryBuilder,
    RecordMapper<? super R, ? extends T> recordMapper
  ) {
    super(
      subscriber -> {
        try (ResultQuery<? extends R> query = queryBuilder.build(connection)) {
          Cursor<? extends R> cursor = query.fetchLazy();
          setupUnsubscription(subscriber, query, cursor);
          log.debug("Select setProducer for  {}", query);
          subscriber.setProducer(new SelectProducer<>(
            subscriber,
            query,
            cursor,
            recordMapper
          ));
        } catch (Throwable t) {
          handleException(t, subscriber);
        }
      }
    );
  }

  private static <R extends Record, T> void setupUnsubscription(
    Subscriber<? super T> subscriber,
    ResultQuery<? extends R> query,
    Cursor<? extends R> cursor
  ) {
    subscriber.add(
      Subscriptions.create(
        () -> {
          closeQuietly(cursor);
          closeQuietly(query);
        }
      )
    );
  }

  private static <T> void handleException(Throwable t, Subscriber<? super T> subscriber) {
    log.debug("onError: ", t);
    if (subscriber.isUnsubscribed()) {
      log.debug("unsubscribed");
    } else {
      subscriber.onError(t);
    }
  }
}
