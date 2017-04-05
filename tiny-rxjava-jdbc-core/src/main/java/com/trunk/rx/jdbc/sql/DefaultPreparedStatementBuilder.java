package com.trunk.rx.jdbc.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * An immutable class to simplify building indexed-parameter {@link PreparedStatement}s.
 */
public class DefaultPreparedStatementBuilder implements PreparedStatementBuilder {

  private final String sql;
  private final Collection<SqlObjectConverter<Object>> converters;
  private final List<TypedObject> parameters;

  /**
   * Create the base builder
   *
   * @param sql the SQL execute. This may have indexed parameters
   * @return the new builder
   */
  public static DefaultPreparedStatementBuilder of(String sql) {
    return new DefaultPreparedStatementBuilder(sql, Collections.emptyList(), Collections.emptyList());
  }

  private DefaultPreparedStatementBuilder(String sql, List<TypedObject> parameters, Collection<SqlObjectConverter<Object>> converters) {
    this.sql = sql;
    this.converters = Collections.unmodifiableCollection(converters);
    this.parameters = Collections.unmodifiableList(parameters);
  }

  /**
   * Add an new {@link SqlObjectConverter}. These are tested in the order they are added.
   *
   * @param converter the {@link SqlObjectConverter} to add to the list of converters
   * @return a new {@link DefaultPreparedStatementBuilder} with the additional converter
   */
  public DefaultPreparedStatementBuilder with(SqlObjectConverter<Object> converter) {
    Collection<SqlObjectConverter<Object>> newConverters = new ArrayList<>();
    newConverters.addAll(converters);
    newConverters.add(converter);
    return new DefaultPreparedStatementBuilder(sql, parameters, newConverters);
  }

  /**
   * Add a new parameter. These are indexed in the order they are added.
   *
   * @param o    the object to be added
   * @param type the type from {@link java.sql.Types}
   * @return a new {@link DefaultPreparedStatementBuilder} with the added object
   */
  public DefaultPreparedStatementBuilder add(Object o, int type) {
    List<TypedObject> newParameters = new ArrayList<>();
    newParameters.addAll(parameters);
    newParameters.add(new TypedObject(o, type));
    return new DefaultPreparedStatementBuilder(sql, newParameters, converters);
  }

  @Override
  public PreparedStatement build(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement(sql);

    for (int i = 0; i < parameters.size(); ++i) {
      TypedObject o = parameters.get(0);
      if (o.object == null) {
        statement.setNull(i, o.type);
      } else {
        statement.setObject(i, convert(o.object, o.type), o.type);
      }
    }

    return statement;
  }

  private Object convert(Object o, int type) {
    return converters.stream()
      .filter(c -> c.matches(o.getClass(), type))
      .limit(1)
      .map(c -> c.convert(o))
      .findFirst()
      .orElse(o);
  }

  private class TypedObject {
    private final Object object;
    private final int type;

    TypedObject(Object object, int type) {
      this.object = object;
      this.type = type;
    }
  }
}
