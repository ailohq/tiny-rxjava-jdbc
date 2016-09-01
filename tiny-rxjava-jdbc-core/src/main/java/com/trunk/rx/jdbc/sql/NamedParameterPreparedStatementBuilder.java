package com.trunk.rx.jdbc.sql;

import com.trunk.jdbc.NamedParameterPreparedStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * An immutable class to simplify building named-parameter {@link PreparedStatement}s.
 */
public class NamedParameterPreparedStatementBuilder implements PreparedStatementBuilder {

  private final String sql;
  private final Collection<SqlObjectConverter<Object>> converters;
  private final Collection<NamedTypedObject> parameters;

  /**
   * Create the base builder
   *
   * @param sql the SQL execute. This may have named parameters
   * @return the new builder
   */
  public static NamedParameterPreparedStatementBuilder of(String sql) {
    return new NamedParameterPreparedStatementBuilder(sql, Collections.emptyList(), Collections.emptyList());
  }

  private NamedParameterPreparedStatementBuilder(String sql, Collection<NamedTypedObject> parameters, Collection<SqlObjectConverter<Object>> converters) {
    this.sql = sql;
    this.converters = Collections.unmodifiableCollection(converters);
    this.parameters = Collections.unmodifiableCollection(parameters);
  }

  /**
   * Add an new {@link SqlObjectConverter}. These are tested in the order they are added.
   *
   * @param converter the {@link SqlObjectConverter} to add to the list of converters
   * @return a new {@link NamedParameterPreparedStatementBuilder} with the additional converter
   */
  public NamedParameterPreparedStatementBuilder with(SqlObjectConverter<Object> converter) {
    Collection<SqlObjectConverter<Object>> newConverters = new ArrayList<>();
    newConverters.addAll(converters);
    newConverters.add(converter);
    return new NamedParameterPreparedStatementBuilder(sql, parameters, newConverters);
  }

  /**
   * Add a new parameter. Parameters with duplicate names wil replace existing parameters.
   * @param name  the name of the parameter as it appears in the query
   * @param o     the object to be added
   * @param type  the type from {@link java.sql.Types}
   * @return      a new {@link NamedParameterPreparedStatementBuilder} with the added object
   */
  public NamedParameterPreparedStatementBuilder add(String name, Object o, int type) throws SQLException {
    List<NamedTypedObject> newParameters = new ArrayList<>();
    newParameters.addAll(parameters);
    newParameters.add(new NamedTypedObject(name, o, type));
    return new NamedParameterPreparedStatementBuilder(sql, newParameters, converters);
  }

  @Override
  public PreparedStatement build(Connection connection) throws SQLException {
    NamedParameterPreparedStatement statement = NamedParameterPreparedStatement.from(connection, sql);
    for (NamedTypedObject o : parameters) {
      if (o.object == null) {
        statement.setNull(o.name, o.type);
      } else {
        statement.setObject(o.name, convert(o.object, o.type), o.type);
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

  private class NamedTypedObject {
    private final String name;
    private final Object object;
    private final int type;

    NamedTypedObject(String name, Object object, int type) {
      this.name = name;
      this.object = object;
      this.type = type;}
  }
}
