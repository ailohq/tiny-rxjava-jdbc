package com.trunk.rx.jdbc.sql;

import com.trunk.rx.jdbc.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * An observable that wraps {@link PreparedStatement#executeQuery()} from the given
 * {@link PreparedStatementBuilder} in an {@link Observable}. The given {@link ResultSetMapper}
 * is used to unmarshal each returned row.
 * <p>
 * The {@link ResultSet} and {@link PreparedStatement} will be canceled if the subscriber unsubscribes
 * before completion.
 * <p>
 * It manages the lifecycle of the
 * {@link PreparedStatement} and the required {@link ResultSet} and does not close the given {@link Connection}.
 */
public class ExecuteQuery<T> extends Observable<T> {
  private static final Logger log = LoggerFactory.getLogger(ExecuteQuery.class);

  public static <T> ExecuteQuery<T> using(
    Connection connection,
    PreparedStatementBuilder preparedStatementBuilder,
    ResultSetMapper<? extends T> resultSetMapper
  ) {
    return new ExecuteQuery<>(connection, preparedStatementBuilder, resultSetMapper);
  }

  private ExecuteQuery(
    Connection connection,
    PreparedStatementBuilder preparedStatementBuilder,
    ResultSetMapper<? extends T> resultSetMapper
  ) {
    super(
      subscriber -> {
        try (PreparedStatement preparedStatement = preparedStatementBuilder.build(connection)) {
          try (ResultSet resultSet = preparedStatement.executeQuery()) {
            setupUnsubscription(subscriber, preparedStatement, resultSet);
            subscriber.setProducer(
              new SelectProducer<>(
                resultSetMapper,
                subscriber,
                preparedStatement,
                resultSet
              )
            );
          }
        } catch (Throwable t) {
          handleException(t, subscriber);
        }
      }
    );
  }

  private static <T> void setupUnsubscription(
    Subscriber<? super T> subscriber,
    PreparedStatement preparedStatement,
    ResultSet resultSet
  ) {
    subscriber.add(
      Subscriptions.create(
        () -> {
          Util.closeQuietly(resultSet);
          Util.closeQuietly(preparedStatement);
        }
      )
    );
  }

  private static <T> void handleException(Throwable t, Subscriber<? super T> subscriber) {
    log.debug("onError: " + t.getMessage());
    if (subscriber.isUnsubscribed()) {
      log.debug("unsubscribed");
    } else {
      subscriber.onError(t);
    }
  }
}
