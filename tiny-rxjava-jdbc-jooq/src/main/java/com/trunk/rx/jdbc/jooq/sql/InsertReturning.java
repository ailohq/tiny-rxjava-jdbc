package com.trunk.rx.jdbc.jooq.sql;

import org.jooq.InsertResultStep;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

import java.sql.Connection;

import static com.trunk.rx.jdbc.jooq.Util.closeQuietly;

/**
 * An {@link Observable} that wraps {@link InsertResultStep#fetch()} and emits an
 * event for row of the returned {@link Result}. Each row is unmarshalled using
 * the given {@link RecordMapper}.
 *
 * The Query will be canceled if the subscriber unsubscribes
 * before completion, but since this is an insert the Query will
 * <i>not</i> be canceled if the subscriber unsubscribes
 * after the first {@link Record} is retrieved.
 *
 * This manages the lifecycle of the Query and does not close the {@link Connection}.
 */
public class InsertReturning<R extends Record, T> extends Observable<T> {
  private static final Logger log = LoggerFactory.getLogger(InsertReturning.class);

  public static <R extends Record, T> InsertReturning<R, T> using(
    Connection connection,
    InsertReturningQueryBuilder<? extends InsertResultStep<? extends R>> queryBuilder,
    RecordMapper<? super R, ? extends T> recordMapper
  ) {
    return new InsertReturning<>(connection, queryBuilder, recordMapper);
  }

  private InsertReturning(
    Connection connection,
    InsertReturningQueryBuilder<? extends InsertResultStep<? extends R>> queryBuilder,
    RecordMapper<? super R, ? extends T> recordMapper
  ) {
    super(
      subscriber -> {
        try (InsertResultStep<? extends R> query = queryBuilder.build(connection)) {
          Iterable<? extends R> result = query.fetch();
          setupUnsubscription(subscriber, query);
          subscriber.setProducer(new InsertReturningProducer<>(
            subscriber,
            query,
            result,
            recordMapper
          ));
        } catch (Throwable t) {
          handleException(t, subscriber);
        }
      }
    );
  }

  private static <T> void setupUnsubscription(Subscriber<? super T> subscriber, Query query) {
    subscriber.add(
      Subscriptions.create(
        () -> closeQuietly(query)
      )
    );
  }

  private static <T> void handleException(Throwable t, Subscriber<? super T> subscriber) {
    log.debug("onError: " + t.getMessage());
    if (subscriber.isUnsubscribed())
      log.debug("unsubscribed");
    else {
      subscriber.onError(t);
    }
  }
}
