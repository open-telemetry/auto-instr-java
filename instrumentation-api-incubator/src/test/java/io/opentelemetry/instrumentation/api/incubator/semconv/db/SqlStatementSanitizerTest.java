/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.db;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class SqlStatementSanitizerTest {

  @ParameterizedTest
  @ArgumentsSource(SqlArgs.class)
  void sanitizeSql(String original, String expected) {
    SqlStatementInfo result = SqlStatementSanitizer.create(true).sanitize(original);
    assertThat(result.getFullStatement()).isEqualTo(expected);
  }

  @ParameterizedTest
  @ArgumentsSource(CouchbaseArgs.class)
  void normalizeCouchbase(String original, String expected) {
    SqlStatementInfo result =
        SqlStatementSanitizer.create(true).sanitize(original, SqlDialect.COUCHBASE);
    assertThat(result.getFullStatement()).isEqualTo(expected);
  }

  @ParameterizedTest
  @ArgumentsSource(SimplifyArgs.class)
  void simplifySql(String original, Function<String, SqlStatementInfo> expectedFunction) {
    SqlStatementInfo result = SqlStatementSanitizer.create(true).sanitize(original);
    SqlStatementInfo expected = expectedFunction.apply(original);
    assertThat(result.getFullStatement()).isEqualTo(expected.getFullStatement());
    assertThat(result.getOperation()).isEqualTo(expected.getOperation());
    assertThat(result.getMainIdentifier()).isEqualToIgnoringCase(expected.getMainIdentifier());
  }

  @Test
  void veryLongSelectStatementsAreOk() {
    StringBuilder sb = new StringBuilder("SELECT * FROM table WHERE");
    for (int i = 0; i < 2000; i++) {
      sb.append(" column").append(i).append("=123 and");
    }
    String query = sb.toString();

    String sanitizedQuery = query.replace("=123", "=?").substring(0, AutoSqlSanitizer.LIMIT);
    SqlStatementInfo expected = SqlStatementInfo.create(sanitizedQuery, "SELECT", "table");

    SqlStatementInfo result = SqlStatementSanitizer.create(true).sanitize(query);

    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @ArgumentsSource(DdlArgs.class)
  void checkDdlOperationStatementsAreOk(
      String actual, Function<String, SqlStatementInfo> expectFunc) {
    SqlStatementInfo result = SqlStatementSanitizer.create(true).sanitize(actual);
    SqlStatementInfo expected = expectFunc.apply(actual);
    assertThat(result.getFullStatement()).isEqualTo(expected.getFullStatement());
    assertThat(result.getOperation()).isEqualTo(expected.getOperation());
    assertThat(result.getMainIdentifier()).isEqualTo(expected.getMainIdentifier());
  }

  @Test
  void lotsOfTicksDontCauseStackOverflowOrLongRuntimes() {
    String s = "'";
    SqlStatementSanitizer sanitizer = SqlStatementSanitizer.create(true);
    for (int i = 0; i < 10000; i++) {
      assertThat(sanitizer.sanitize(s)).isNotNull();
      s += "'";
    }
  }

  @Test
  void veryLongNumbersAreOk() {
    String s = "";
    for (int i = 0; i < 10000; i++) {
      s += String.valueOf(i);
    }
    SqlStatementInfo result = SqlStatementSanitizer.create(true).sanitize(s);
    assertThat(result.getFullStatement()).isEqualTo("?");
  }

  @Test
  void veryLongNumbersAtEndOfTableAreOk() {
    String s = "A";
    for (int i = 0; i < 10000; i++) {
      s += String.valueOf(i);
    }
    SqlStatementInfo result = SqlStatementSanitizer.create(true).sanitize(s);
    assertThat(result.getFullStatement()).isEqualTo(s.substring(0, AutoSqlSanitizer.LIMIT));
  }

  @Test
  void test32kTruncation() {
    StringBuffer s = new StringBuffer();
    for (int i = 0; i < 10000; i++) {
      s.append("SELECT * FROM TABLE WHERE FIELD = 1234 AND ");
    }
    String sanitized = SqlStatementSanitizer.create(true).sanitize(s.toString()).getFullStatement();
    assertThat(sanitized.length()).isLessThanOrEqualTo(AutoSqlSanitizer.LIMIT);
    assertThat(sanitized).doesNotContain("1234");
  }

  @Test
  void randomBytesDontCauseExceptionsOrTimeouts() {
    Random r = new Random(0);
    for (int i = 0; i < 1000; i++) {
      StringBuffer sb = new StringBuffer();
      for (int c = 0; c < 1000; c++) {
        sb.append((char) r.nextInt(Character.MAX_VALUE));
      }
      SqlStatementSanitizer.create(true).sanitize(sb.toString());
    }
  }

  @Test
  public void longInStatementDoesntCauseStackOverflow() {
    StringBuilder s = new StringBuilder("select col from table where col in (");
    for (int i = 0; i < 10000; i++) {
      s.append("?,");
    }
    s.append("?)");

    String sanitized = SqlStatementSanitizer.create(true).sanitize(s.toString()).getFullStatement();

    assertThat(sanitized).isEqualTo("select col from table where col in (?)");
  }

  @Test
  public void largeStatementCached() {
    // test that short statement is cached
    String shortStatement = "SELECT * FROM TABLE WHERE FIELD = 1234";
    String sanitizedShort =
        SqlStatementSanitizer.create(true).sanitize(shortStatement).getFullStatement();
    assertThat(sanitizedShort).doesNotContain("1234");
    assertThat(SqlStatementSanitizer.isCached(shortStatement)).isTrue();

    // test that large statement is not cached
    StringBuffer s = new StringBuffer();
    for (int i = 0; i < 10000; i++) {
      s.append("SELECT * FROM TABLE WHERE FIELD = 1234 AND ");
    }
    String largeStatement = s.toString();
    String sanitizedLarge =
        SqlStatementSanitizer.create(true).sanitize(largeStatement).getFullStatement();
    assertThat(sanitizedLarge).doesNotContain("1234");
    assertThat(SqlStatementSanitizer.isCached(largeStatement)).isFalse();
  }

  static class SqlArgs implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of("SELECT * FROM TABLE WHERE FIELD=1234", "SELECT * FROM TABLE WHERE FIELD=?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = 1234", "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD>=-1234", "SELECT * FROM TABLE WHERE FIELD>=?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD<-1234", "SELECT * FROM TABLE WHERE FIELD<?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD <.1234", "SELECT * FROM TABLE WHERE FIELD <?"),
          Arguments.of("SELECT 1.2", "SELECT ?"),
          Arguments.of("SELECT -1.2", "SELECT ?"),
          Arguments.of("SELECT -1.2e-9", "SELECT ?"),
          Arguments.of("SELECT 2E+9", "SELECT ?"),
          Arguments.of("SELECT +0.2", "SELECT ?"),
          Arguments.of("SELECT .2", "SELECT ?"),
          Arguments.of("7", "?"),
          Arguments.of(".7", "?"),
          Arguments.of("-7", "?"),
          Arguments.of("+7", "?"),
          Arguments.of("SELECT 0x0af764", "SELECT ?"),
          Arguments.of("SELECT 0xdeadBEEF", "SELECT ?"),
          Arguments.of("SELECT * FROM \"TABLE\"", "SELECT * FROM \"TABLE\""),

          // Not numbers but could be confused as such
          Arguments.of("SELECT A + B", "SELECT A + B"),
          Arguments.of("SELECT -- comment", "SELECT -- comment"),
          Arguments.of("SELECT * FROM TABLE123", "SELECT * FROM TABLE123"),
          Arguments.of(
              "SELECT FIELD2 FROM TABLE_123 WHERE X<>7", "SELECT FIELD2 FROM TABLE_123 WHERE X<>?"),

          // Semi-nonsensical almost-numbers to elide or not
          Arguments.of("SELECT --83--...--8e+76e3E-1", "SELECT ?"),
          Arguments.of("SELECT DEADBEEF", "SELECT DEADBEEF"),
          Arguments.of("SELECT 123-45-6789", "SELECT ?"),
          Arguments.of("SELECT 1/2/34", "SELECT ?/?/?"),

          // Basic ' strings
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = ''", "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = 'words and spaces'",
              "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = ' an escaped '' quote mark inside'",
              "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = '\\\\'", "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = '\"inside doubles\"'",
              "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = '\"$$$$\"'",
              "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = 'a single \" doublequote inside'",
              "SELECT * FROM TABLE WHERE FIELD = ?"),

          // Some databases allow using dollar-quoted strings
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = $$$$", "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = $$words and spaces$$",
              "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = $$quotes '\" inside$$",
              "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = $$\"''\"$$",
              "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = $$\\\\$$", "SELECT * FROM TABLE WHERE FIELD = ?"),

          // PostgreSQL native parameter marker, we want to keep $1 instead of replacing it with ?
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = $1", "SELECT * FROM TABLE WHERE FIELD = $1"),

          // Unicode, including a unicode identifier with a trailing number
          Arguments.of(
              "SELECT * FROM TABLEओ7 WHERE FIELD = 'ɣ'", "SELECT * FROM TABLEओ7 WHERE FIELD = ?"),

          // whitespace normalization
          Arguments.of(
              "SELECT    *    \t\r\nFROM  TABLE WHERE FIELD1 = 12344 AND FIELD2 = 5678",
              "SELECT * FROM TABLE WHERE FIELD1 = ? AND FIELD2 = ?"),

          // hibernate/jpa query language
          Arguments.of("FROM TABLE WHERE FIELD=1234", "FROM TABLE WHERE FIELD=?"));
    }
  }

  static class CouchbaseArgs implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          // Some databases support/encourage " instead of ' with same escape rules
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = \"\"", "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = \"words and spaces'\"",
              "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = \" an escaped \"\" quote mark inside\"",
              "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = \"\\\\\"", "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = \"'inside singles'\"",
              "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = \"'$$$$'\"",
              "SELECT * FROM TABLE WHERE FIELD = ?"),
          Arguments.of(
              "SELECT * FROM TABLE WHERE FIELD = \"a single ' singlequote inside\"",
              "SELECT * FROM TABLE WHERE FIELD = ?"));
    }
  }

  static class SimplifyArgs implements ArgumentsProvider {

    static Function<String, SqlStatementInfo> expect(String operation, String identifier) {
      return sql -> SqlStatementInfo.create(sql, operation, identifier);
    }

    static Function<String, SqlStatementInfo> expect(
        String sql, String operation, String identifier) {
      return ignored -> SqlStatementInfo.create(sql, operation, identifier);
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          // Select
          Arguments.of("SELECT x, y, z FROM schema.table", expect("SELECT", "schema.table")),
          Arguments.of("SELECT x, y, z FROM `schema table`", expect("SELECT", "schema table")),
          Arguments.of("SELECT x, y, z FROM \"schema table\"", expect("SELECT", "schema table")),
          Arguments.of(
              "WITH subquery as (select a from b) SELECT x, y, z FROM table",
              expect("SELECT", null)),
          Arguments.of("SELECT x, y, (select a from b) as z FROM table", expect("SELECT", null)),
          Arguments.of(
              "select delete, insert into, merge, update from table", expect("SELECT", "table")),
          Arguments.of("select col /* from table2 */ from table", expect("SELECT", "table")),
          Arguments.of("select col from table join anotherTable", expect("SELECT", null)),
          Arguments.of("select col from (select * from anotherTable)", expect("SELECT", null)),
          Arguments.of(
              "select col from (select * from anotherTable) alias", expect("SELECT", null)),
          Arguments.of(
              "select col from table1 union select col from table2", expect("SELECT", null)),
          Arguments.of(
              "select col from table where col in (select * from anotherTable)",
              expect("SELECT", null)),
          Arguments.of("select col from table1, table2", expect("SELECT", null)),
          Arguments.of("select col from table1 t1, table2 t2", expect("SELECT", null)),
          Arguments.of("select col from table1 as t1, table2 as t2", expect("SELECT", null)),
          Arguments.of(
              "select col from table where col in (1, 2, 3)",
              expect("select col from table where col in (?)", "SELECT", "table")),
          Arguments.of(
              "select 'a' IN(x, 'b') from table where col in (1) and z IN( '3', '4' )",
              expect(
                  "select ? IN(x, ?) from table where col in (?) and z IN(?)", "SELECT", "table")),
          Arguments.of("select col from table order by col, col2", expect("SELECT", "table")),
          Arguments.of("select ąś∂ń© from źćļńĶ order by col, col2", expect("SELECT", "źćļńĶ")),
          Arguments.of("select 12345678", expect("select ?", "SELECT", null)),
          Arguments.of("/* update comment */ select * from table1", expect("SELECT", "table1")),
          Arguments.of("select /*((*/abc from table", expect("SELECT", "table")),
          Arguments.of("SeLeCT * FrOm TAblE", expect("SELECT", "table")),
          Arguments.of("select next value in hibernate_sequence", expect("SELECT", null)),

          // hibernate/jpa
          Arguments.of("FROM schema.table", expect("SELECT", "schema.table")),
          Arguments.of("/* update comment */ from table1", expect("SELECT", "table1")),

          // Insert
          Arguments.of(" insert into table where lalala", expect("INSERT", "table")),
          Arguments.of("insert insert into table where lalala", expect("INSERT", "table")),
          Arguments.of("insert into db.table where lalala", expect("INSERT", "db.table")),
          Arguments.of("insert into `db table` where lalala", expect("INSERT", "db table")),
          Arguments.of("insert into \"db table\" where lalala", expect("INSERT", "db table")),
          Arguments.of("insert without i-n-t-o", expect("INSERT", null)),

          // Delete
          Arguments.of("delete from table where something something", expect("DELETE", "table")),
          Arguments.of(
              "delete from `my table` where something something", expect("DELETE", "my table")),
          Arguments.of(
              "delete from \"my table\" where something something", expect("DELETE", "my table")),
          Arguments.of(
              "delete from foo where x IN (1,2,3)",
              expect("delete from foo where x IN (?)", "DELETE", "foo")),
          Arguments.of("delete from 12345678", expect("delete from ?", "DELETE", null)),
          Arguments.of("delete   (((", expect("delete (((", "DELETE", null)),

          // Update
          Arguments.of(
              "update table set answer=42", expect("update table set answer=?", "UPDATE", "table")),
          Arguments.of(
              "update `my table` set answer=42",
              expect("update `my table` set answer=?", "UPDATE", "my table")),
          Arguments.of(
              "update `my table` set answer=42 where x IN('a', 'b') AND y In ('a',  'b')",
              expect(
                  "update `my table` set answer=? where x IN(?) AND y In (?)",
                  "UPDATE",
                  "my table")),
          Arguments.of(
              "update \"my table\" set answer=42",
              expect("update \"my table\" set answer=?", "UPDATE", "my table")),
          Arguments.of("update /*table", expect("UPDATE", null)),

          // Call
          Arguments.of("call test_proc()", expect("CALL", "test_proc")),
          Arguments.of("call test_proc", expect("CALL", "test_proc")),
          Arguments.of("call next value in hibernate_sequence", expect("CALL", null)),
          Arguments.of("call db.test_proc", expect("CALL", "db.test_proc")),

          // Merge
          Arguments.of("merge into table", expect("MERGE", "table")),
          Arguments.of("merge into `my table`", expect("MERGE", "my table")),
          Arguments.of("merge into \"my table\"", expect("MERGE", "my table")),
          Arguments.of("merge table (into is optional in some dbs)", expect("MERGE", "table")),
          Arguments.of("merge (into )))", expect("MERGE", null)),

          // Unknown operation
          Arguments.of("and now for something completely different", expect(null, null)),
          Arguments.of("", expect(null, null)),
          Arguments.of(null, expect(null, null)));
    }
  }

  static class DdlArgs implements ArgumentsProvider {

    static Function<String, SqlStatementInfo> expect(String operation, String identifier) {
      return sql -> SqlStatementInfo.create(sql, operation, identifier);
    }

    static Function<String, SqlStatementInfo> expect(
        String sql, String operation, String identifier) {
      return ignored -> SqlStatementInfo.create(sql, operation, identifier);
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of("CREATE TABLE `table`", expect("CREATE TABLE", "table")),
          Arguments.of("CREATE TABLE IF NOT EXISTS table", expect("CREATE TABLE", "table")),
          Arguments.of("DROP TABLE `if`", expect("DROP TABLE", "if")),
          Arguments.of(
              "ALTER TABLE table ADD CONSTRAINT c FOREIGN KEY (foreign_id) REFERENCES ref (id)",
              expect("ALTER TABLE", "table")),
          Arguments.of("CREATE INDEX types_name ON types (name)", expect("CREATE INDEX", null)),
          Arguments.of("DROP INDEX types_name ON types (name)", expect("DROP INDEX", null)),
          Arguments.of(
              "CREATE VIEW tmp AS SELECT type FROM table WHERE id = ?",
              expect("CREATE VIEW", null)),
          Arguments.of(
              "CREATE PROCEDURE p AS SELECT * FROM table GO", expect("CREATE PROCEDURE", null)));
    }
  }
}
