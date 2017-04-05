# tiny-rxjava-jdbc

TBD status lozenges

A simple RxJava wrapper around JDBC that lets you convert JDBC `Connections` to
[RxJava](https://github.com/ReactiveX/RxJava)
[Observables](https://github.com/ReactiveX/RxJava/wiki/Observable).
`tiny-rxjava-jdbc` manages the lifecycle of you `Connections`, transactions, `PreparedStatements` and `ResultSets`.

The two main drivers for developing this library were:
* ability to manage JDBC transactions while working with `Observables`;
* access to the raw ResultSet (and then Record when we moved to jOOQ).

For query chaining and ORM-like access use [rxjava-jdbc](https://github.com/davidmoten/rxjava-jdbc).


## Building

Using `tiny-rxjava-jdbc` uses [Gradle](http://gradle.org/) for build.

```bash
git clone https://github.com/Trunkplatform/tiny-rxjava-jdbc.git
cd tiny-rxjava-jdbc
./gradlew clean build
```

Running the tests for `tiny-rxjava-jdbc-test` requires postgres to be installed.
See `db-env`.

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


TBC maven/gradle

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

* Select using [Select](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-jooq/src/main/java/com/trunk/rx/jdbc/jooq/sql/Select.java)
* Modify using [Execute](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-jooq/src/main/java/com/trunk/rx/jdbc/jooq/sql/Execute.java)
* Insert Returning using [InsertReturning](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-jooq/src/main/java/com/trunk/rx/jdbc/jooq/sql/InsertReturning.java)
  when supported by the database. 

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

Provides a named, pooled [PostgreSQL ConnectionProvider](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-pg/src/main/java/com/trunk/rx/jdbc/pg/PgConnectionProvider.java) 
and a [Hikari](https://github.com/brettwooldridge/HikariCP) based
[ConnectionProvider](https://github.com/Trunkplatform/tiny-rxjava-jdbc/blob/master/tiny-rxjava-jdbc-pg/src/main/java/com/trunk/rx/jdbc/pg/PgConnectionProvider.java).

TBC maven/gradle

```java
import com.trunk.rx.jdbc.pg.PgConnectionProvider;
import com.trunk.rx.jdbc.pg.PgHikariConnectionProvider;

new PgConnectionProvider(...);
new PgHikariConnectionProvider(...);
```

# tiny-rxjava-jdbc-guice

[Archaius](https://github.com/Netflix/archaius)/[Guice](https://github.com/google/guice)
bindings for `tiny-rxjava-jdbc-core`. This requires a binding for a `ConnectionProvider`,
eg `PgConnectionProviderModule` or `PgHikariConnectionProviderModule`.
This is provided as a singleton.

```java
Injector injector = Guice.createInjector(new ArchaiusModule(), new ConnectionPoolModule(), new PgConnectionProviderModule());
```

# tiny-rxjava-jdbc-pg-guice

[Archaius](https://github.com/Netflix/archaius)/[Guice](https://github.com/google/guice)
bindings for `tiny-rxjava-jdbc-pg`. `PgConnectionProvider` and `PgHikariConnectionProvider` are both 
bound to `ConnectionProvider`, so only one of the modules should be added to any
one `Injector`. These are provided as singletons.

Expects the following in your properties:
* `database_host` the database hostname
* `database_database` the database name
* `database_username` the username 
* `database_password` the password
* `database_maxConnections` (optional) the connection pool size

```java
Injector injector = Guice.createInjector(new ArchaiusModule(), new PgConnectionProviderModule());
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

## License

Code ported from [rxjava-jdbc is copyright David Moten](https://github.com/davidmoten/rxjava-jdbc/).

All other material copyright 2016 Trunk Platform.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
