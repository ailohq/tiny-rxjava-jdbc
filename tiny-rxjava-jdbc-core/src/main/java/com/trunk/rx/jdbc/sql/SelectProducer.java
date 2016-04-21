/**
 * Copywrite https://github.com/davidmoten/rxjava-jdbc/blob/master/LICENSE
 */
package com.trunk.rx.jdbc.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trunk.rx.jdbc.Util;

import rx.Producer;
import rx.Subscriber;

import static rx.internal.operators.BackpressureUtils.getAndAddRequest;

/**
 * A back pressure sensitive {@link Producer} over {@link ResultSet}s.
 * If the {@link Subscriber} unsubscribes before completion it will
 * cancel the request.
 */
public class SelectProducer<T> implements Producer {

  private static final Logger log = LoggerFactory.getLogger(SelectProducer.class);

  private final ResultSetMapper<? extends T> resultSetMapper;
  private final Subscriber<? super T> subscriber;
  private final PreparedStatement preparedStatement;
  private final ResultSet resultSet;
  private volatile boolean keepGoing = true;

  private final AtomicLong requested = new AtomicLong(0);

  public SelectProducer(ResultSetMapper<? extends T> resultSetMapper,
                 Subscriber<? super T> subscriber,
                 PreparedStatement preparedStatement,
                 ResultSet resultSet) {
    this.resultSetMapper = resultSetMapper;
    this.subscriber = subscriber;
    this.preparedStatement = preparedStatement;
    this.resultSet = resultSet;
  }

  @Override
  public void request(long n) {
    if (requested.get() == Long.MAX_VALUE) {
      // already started with fast path
      return;
    } else if (n == Long.MAX_VALUE && requested.compareAndSet(0, Long.MAX_VALUE)) {
      requestAll();
    } else if (n > 0) {
      requestSome(n);
    }
  }

  private void requestAll() {
    // fast path
    try {
      while(keepGoing) {
        processRow(subscriber);
      }
      closeQuietly();
      complete(subscriber);
    } catch (Exception e) {
      closeAndHandleException(e);
    }
  }

  private void requestSome(long n) {
    // back pressure path
    // this algorithm copied generally from OnSubscribeFromIterable.java
    long previousCount = getAndAddRequest(requested, n);
    if (previousCount == 0) {
      try {
        while(true) {
          long r = requested.get();
          long numToEmit = r;

          while(keepGoing && --numToEmit >= 0) {
            processRow(subscriber);
          }
          if (keepGoing) {
            if (requested.addAndGet(-r) == 0) {
              return;
            }
          } else {
            closeQuietly();
            complete(subscriber);
            return;
          }
        }
      } catch (Exception e) {
        closeAndHandleException(e);
      }
    }
  }

  private void closeAndHandleException(Exception e) {
    try {
      closeQuietly();
    } finally {
      handleException(e, subscriber);
    }
  }

  /**
   * Processes each row of the {@link ResultSet}.
   *
   * @param subscriber
   *
   * @throws SQLException
   */
  private void processRow(Subscriber<? super T> subscriber) throws SQLException {
    checkSubscription(subscriber);
    if (!keepGoing) {
      return;
    }
    if (resultSet.next()) {
      log.trace("onNext");
      subscriber.onNext(resultSetMapper.f(resultSet));
    } else {
      keepGoing = false;
    }
  }

  /**
   * Tells observer that stream is complete and closes resources.
   *
   * @param subscriber
   */
  private void complete(Subscriber<? super T> subscriber) {
    if (subscriber.isUnsubscribed()) {
      log.debug("unsubscribed");
    } else {
      log.debug("onCompleted");
      subscriber.onCompleted();
    }
  }

  /**
   * Tells observer about exception.
   *
   * @param e
   * @param subscriber
   */
  private void handleException(Exception e, Subscriber<? super T> subscriber) {
    log.debug("onError: " + e.getMessage());
    if (subscriber.isUnsubscribed()) {
      log.debug("unsubscribed");
    } else {
      subscriber.onError(e);
    }
  }

  /**
   * Closes connection resources (connection, prepared statement and result set).
   */
  private void closeQuietly() {
    log.debug("closing rs");
    Util.closeQuietly(resultSet);
    log.debug("closing ps");
    Util.closeQuietly(preparedStatement);
  }

  /**
   * If subscribe unsubscribed sets keepGoing to false.
   *
   * @param subscriber
   */
  private void checkSubscription(Subscriber<? super T> subscriber) {
    if (subscriber.isUnsubscribed()) {
      keepGoing = false;
      log.debug("unsubscribing");
    }
  }

}
