package com.trunk.rx.jdbc.test;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trunk.rx.jdbc.ConnectionProvider;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import rx.Observable;

import static com.trunk.rx.jdbc.Util.closeQuietly;


/**
 * A utility that will first load <i>migrations.xml</i> and any additional
 * liquibase upgrade files passed to it.
 */
public class LiquibaseBootstrap {
  private static final Logger log = LoggerFactory.getLogger(LiquibaseBootstrap.class);

  /**
   * Runs {@link Liquibase#update(Contexts)} on a connection from the given {@link ConnectionProvider}.
   * It will first use <i>migrations.xml</i> from the classpath, then use any given update files. All
   * paths are relative to the current classpath.
   *
   * @param connectionProvider  the ConnectionProvider to run {@link Liquibase#update(Contexts)} on
   * @param updateFiles the paths on the classpath to any additional update files
   * @return the connectionProvider
   */
  public static Observable<ConnectionProvider> using(ConnectionProvider connectionProvider, String... updateFiles) {
    return bootstrap(connectionProvider, updateFiles);
  }

  private LiquibaseBootstrap() {
    // do nothing
  }

  private static Observable<ConnectionProvider> bootstrap(ConnectionProvider connectionProvider, String[] updateFiles) {
    return connectionProvider.get()
      .doOnNext(connection -> runLiquibaseUpgrade(updateFiles, connection))
      .map(ignore -> connectionProvider)
      .take(1);
  }

  private static void runLiquibaseUpgrade(String[] updateFiles, Connection connection){
    Database database = null;
    try {
      database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
      Liquibase liquibase = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), database);
      liquibase.update(new Contexts());

      for (String updateFile : updateFiles) {
        Liquibase l = new Liquibase(updateFile, new ClassLoaderResourceAccessor(), database);
        l.update(new Contexts());
      }

    } catch (Exception e) {
      log.error("Error running upgrade", e);
      throw new RuntimeException(e);
    } finally {
      try {
        if (database != null) {
          database.close();
        }
      } catch (Exception e) {
        log.warn("Error closing connection", e);
      }
      closeQuietly(connection);
    }
  }
}
