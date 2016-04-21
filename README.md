# tiny-rxjava-jdbc

TBD status lozenges

Functional access to JDBC using RxJava

A simple RxJava wrapper around JDBC that lets you convert JDBC `Connections` to
[RxJava](https://github.com/ReactiveX/RxJava)
[Observables](https://github.com/ReactiveX/RxJava/wiki/Observable).
`tiny-rxjava-jdbc` manages the lifecycle of you `Connections`, transactions, `PreparedStatements` and `ResultSets`.

The two main drivers for developing this library were:
* ability to manage JDBC transactions while working with `Observables`;
* access to the raw ResultSet (and then Record when we moved to jOOQ).

For query chaining and ORM-like access use [rxjava-jdbc](https://github.com/davidmoten/rxjava-jdbc).


## tiny-rxjava-jdbc-core

Provides access to core JDBC functionality.

Connections and transactions are managed from the
[ConnectionPool](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-core/src/main/java/com/trunk/rx/jdbc/ConnectionPool.java) and
[TransactionContextExecutor](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-core/src/main/java/com/trunk/rx/jdbc/TransactionContextExecutor.java).
All SQL executions start with the `ConnectionPool` the returned `TransactionContextExecutor` is used to manage how
transactions are handled. Signaling is borrowed from [RxNetty](https://github.com/ReactiveX/RxNetty),
using the completion of the ConnectionConsumer's Observable to signal completion of activity on the
`Connection`. The only extension to this is `withCommitPerEvent`, which uses each emitted event to
 signal the need to commit a transaction.


TBC mave/gradle

### Connections

```java
ConnectionPool pool = ConnectionPool.from(...);

Observable<Void> result = pool
  .execute(
    connection -> {
      // do stuff with connection ...

      // signal completion
      return Observable.empty();
    }
  )
  .withSingleTransaction();
```

### Composable SQL execution

```java
ConnectionPool pool = ConnectionPool.from(...);

Observable<Object> result = pool
  .execute(
    connection ->
      Execute.using(connection, connection -> connection.prepareStatement("CREATE TABLE test (id INT);"))
        .concatWith(
          ExecuteUpdate.using(connection, connection -> connection.prepareStatement("INSERT INTO test VALUES (7);"))
        )
        .concatWith(
          ExecuteQuery.using(connection, connection -> connection.prepareStatement("SELECT id FROM test;"))
        )
  );
// result is [null, 1, 7]
```

### PreparedStatement builders

```java
ConnectionPool pool = ConnectionPool.from(...);

Observable<Integer> result = pool
  .execute(
    connection ->
      ExecuteQuery.using(
        connection,
        DefaultPreparedStatementBuilder.of("SELECT id FROM test WHERE id = ?;")
          .add(7, Types.INTEGER)
      )
  );
```

```java
ConnectionPool pool = ConnectionPool.from(...);

Observable<Integer> result = pool
  .execute(
    connection ->
      ExecuteQuery.using(
        connection,
        NamedParameterPreparedStatementBuilder.of("SELECT id FROM test WHERE id = :id;")
          .add("id", 7, Types.INTEGER)
      )
  );
// result is [null, 1, 7]
```


See [FunctionalTests.java](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-test/src/test/java/com/trunk/rx/jdbc/FunctionalTests.java)
for examples.

## tiny-rxjava-jdbc-h2

Provides a named, pooled [H2 ConnectionProvider](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-h2/src/main/java/com/trunk/rx/jdbc/h2/H2ConnectionProvider.java).

See [FunctionalTests.java](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-test/src/test/java/com/trunk/rx/jdbc/FunctionalTests.java)
for examples.

TBC maven/gradle

```java
import com.trunk.rx.jdbc.h2.H2ConnectionProvider;

ConnectionProvider p = new H2ConnectionProvider("pool-name");
```

## tiny-rxjava-jdbc-jooq

Use [jOOQ's](http://www.jooq.org/) DSL to query your database.

* Select using [ExecuteSelect](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-jooq/src/main/java/com/trunk/rx/jdbc/jooq/sql/ExecuteSelect.java)
* Modify using [Execute](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-jooq/src/main/java/com/trunk/rx/jdbc/jooq/sql/Execute.java)

See [FunctionalTests.java](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-test/src/test/java/com/trunk/rx/jdbc/FunctionalTests.java)
for examples.

TBC maven/gradle

```java
Table<Record> test = table("test");
Field<Integer> id = field("id", SQLDataType.INTEGER);

ConnectionPool pool = ConnectionPool.from(...);

Observable<Integer> ids = pool
  .execute(
    connection ->
      Select.using(connection,
                   c -> using(c, SQLDialect.H2).select(id).from(test),
                   r -> r.getValue(0, Integer.class))
  );
```


## tiny-rxjava-jdbc-pg

Provides a named, pooled [PostgreSQL ConnectionProvider](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-pg/src/main/java/com/trunk/rx/jdbc/pg/PgConnectionProvider.java).

TBC maven/gradle

```java
import com.trunk.rx.jdbc.pg.PgConnectionProvider;

ConnectionProvider p = new PgConnectionProvider(...);
```

## tiny-rxjava-jdbc-test

Bootstrap test data into a connection for testing using Liquibase.

See [FunctionalTests.java](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-test/src/test/java/com/trunk/rx/jdbc/FunctionalTests.java)
for examples.

TBC maven/gradle

```java
ConnectionProvider connectionProvider = ...;
Observable<ConnectionProvider> pool = LiquibaseBootstrap.using(connectionProvider, "test/sample_update.xml");
```

