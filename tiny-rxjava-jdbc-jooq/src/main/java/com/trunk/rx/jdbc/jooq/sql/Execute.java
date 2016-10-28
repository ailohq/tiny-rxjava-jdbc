package com.trunk.rx.jdbc.jooq.sql;

import java.sql.Connection;

import org.jooq.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trunk.rx.jdbc.jooq.Util;

import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

/**
 * An {@link Observable} that wraps {@link Query#execute()} and emits a single
 * event containing the result of {@link Query#execute() execute()}.
 *
 * The Query will be canceled if the subscriber unsubscribes
 * before completion.
 *
 * This manages the lifecycle of the Query and does not close the {@link Connection}.
 */
public class Execute extends Observable<Integer> {
  private static final Logger log = LoggerFactory.getLogger(Execute.class);

  public static Execute using(Connection connection, QueryBuilder<? extends Query> queryBuilder) {
    return new Execute(connection, queryBuilder);
  }

  private Execute(Connection connection,
                  QueryBuilder<? extends Query> queryBuilder) {
    super(
      subscriber -> {
        try (Query query = queryBuilder.build(connection)) {
          setupUnsubscription(subscriber, query);
          int i = query.execute();
          if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(i);
            subscriber.onCompleted();
          }
        } catch (Throwable t) {
          handleException(t, subscriber);
        }
      }
    );
  }

  private static <T> void setupUnsubscription(Subscriber<? super T> subscriber,
                                              Query query) {
    subscriber.add(
      Subscriptions.create(
        () -> Util.closeQuietly(query)
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
