package com.trunk.rx.jdbc.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trunk.rx.jdbc.Util;

import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

/**
 * An observable that wraps {@link PreparedStatement#executeUpdate()} from the given
 * {@link PreparedStatementBuilder} in an {@link Observable}. It emits a single {@link Integer}
 * event containing the result of {@link PreparedStatement#executeUpdate() executeUpdate()}.
 *
 * The {@link PreparedStatement} will be canceled if the subscriber unsubscribes
 * before completion.
 *
 * It manages the lifecycle of the
 * {@link PreparedStatement} and does not close the given {@link Connection}.
 */
public class ExecuteUpdate extends Observable<Integer> {
  private static final Logger log = LoggerFactory.getLogger(ExecuteQuery.class);

  public static ExecuteUpdate using(Connection connection, PreparedStatementBuilder preparedStatementBuilder) {
    return new ExecuteUpdate(connection, preparedStatementBuilder);
  }

  private ExecuteUpdate(Connection connection, PreparedStatementBuilder preparedStatementBuilder) {
    super(
      subscriber -> {
        try (PreparedStatement preparedStatement = preparedStatementBuilder.build(connection)) {
          setupUnsubscription(subscriber, preparedStatement);
          int updatedRows = preparedStatement.executeUpdate();
          subscriber.onNext(updatedRows);
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
