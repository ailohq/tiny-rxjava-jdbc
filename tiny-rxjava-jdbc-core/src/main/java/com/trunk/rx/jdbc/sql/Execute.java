package com.trunk.rx.jdbc.sql;

import com.trunk.rx.jdbc.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * An observable that wraps {@link PreparedStatement#execute()} from the given
 * {@link PreparedStatementBuilder} in an {@link Observable}. It emits a single {@link Void}
 * event on completion - the output is suppressed since
 * execute is a legacy general-purpose methods.
 *
 * The {@link PreparedStatement} will be canceled if the subscriber unsubscribes
 * before completion.
 *
 * It manages the lifecycle of the {@link PreparedStatement}
 * and does not close the given {@link Connection}.
 */
public class Execute extends Observable<Void> {
  private static final Logger log = LoggerFactory.getLogger(Execute.class);

  public static Execute using(Connection connection, PreparedStatementBuilder preparedStatementBuilder) {
    return new Execute(connection, preparedStatementBuilder);
  }

  private Execute(Connection connection, PreparedStatementBuilder preparedStatementBuilder) {
    super(
      subscriber -> {
        try (PreparedStatement preparedStatement = preparedStatementBuilder.build(connection)) {
          setupUnsubscription(subscriber, preparedStatement);
          preparedStatement.execute();
          subscriber.onNext(null);
          subscriber.onCompleted();
        } catch (Throwable t) {
          handleException(t, subscriber);
        }
      }
    );
  }

  private static <T> void setupUnsubscription(Subscriber<? super T> subscriber,
                                              PreparedStatement preparedStatement) {
    subscriber.add(
      Subscriptions.create(
        () -> Util.closeQuietly(preparedStatement)
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
