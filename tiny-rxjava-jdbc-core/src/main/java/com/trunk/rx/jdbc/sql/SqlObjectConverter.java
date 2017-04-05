package com.trunk.rx.jdbc.sql;

/**
 * Convert an object of type I into an object of type O that can be written to a database via JDBC
 */
public interface SqlObjectConverter<I> {
  Object convert(I in);

  /**
   * Test if this converter can convert the given object
   *
   * @param iClass the {@link Class} of the object to be converted
   * @param type   the expected SQL type from {@link java.sql.Types}
   * @return true if the class and type can be converted
   */
  boolean matches(Class<?> iClass, int type);
}
