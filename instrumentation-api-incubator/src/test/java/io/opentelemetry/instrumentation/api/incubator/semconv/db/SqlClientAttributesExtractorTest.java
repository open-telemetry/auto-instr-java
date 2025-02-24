/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.db;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation") // using deprecated semconv
class SqlClientAttributesExtractorTest {

  static class TestAttributesGetter
      implements SqlClientAttributesGetter<Map<String, Object>, Void> {

    @Override
    public Collection<String> getRawQueryTexts(Map<String, Object> map) {
      String statement = read(map, "db.statement");
      return statement == null ? emptySet() : singleton(statement);
    }

    @Override
    public String getDbSystem(Map<String, Object> map) {
      return read(map, "db.system");
    }

    @Deprecated
    @Override
    public String getUser(Map<String, Object> map) {
      return read(map, "db.user");
    }

    @Override
    public String getDbNamespace(Map<String, Object> map) {
      return read(map, "db.name");
    }

    @Deprecated
    @Override
    public String getConnectionString(Map<String, Object> map) {
      return read(map, "db.connection_string");
    }

    @Override
    public Long getBatchSize(Map<String, Object> map) {
      return read(map, "db.operation.batch.size", Long.class);
    }

    protected String read(Map<String, Object> map, String key) {
      return read(map, key, String.class);
    }

    protected <T> T read(Map<String, Object> map, String key, Class<T> clazz) {
      return clazz.cast(map.get(key));
    }
  }

  static class TestMultiAttributesGetter extends TestAttributesGetter
      implements SqlClientAttributesGetter<Map<String, Object>, Void> {

    @SuppressWarnings("unchecked")
    @Override
    public Collection<String> getRawQueryTexts(Map<String, Object> map) {
      return (Collection<String>) map.get("db.statements");
    }
  }

  @SuppressWarnings("deprecation") // TODO DbIncubatingAttributes.DB_CONNECTION_STRING deprecation
  @Test
  void shouldExtractAllAttributes() {
    // given
    Map<String, Object> request = new HashMap<>();
    request.put("db.system", "myDb");
    request.put("db.user", "username");
    request.put("db.name", "potatoes");
    request.put("db.connection_string", "mydb:///potatoes");
    request.put("db.statement", "SELECT * FROM potato WHERE id=12345");

    Context context = Context.root();

    AttributesExtractor<Map<String, Object>, Void> underTest =
        SqlClientAttributesExtractor.create(new TestAttributesGetter());

    // when
    AttributesBuilder startAttributes = Attributes.builder();
    underTest.onStart(startAttributes, context, request);

    AttributesBuilder endAttributes = Attributes.builder();
    underTest.onEnd(endAttributes, context, request, null, null);

    // then
    if (SemconvStability.emitStableDatabaseSemconv() && SemconvStability.emitOldDatabaseSemconv()) {
      assertThat(startAttributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_SYSTEM, "myDb"),
              entry(DbIncubatingAttributes.DB_SYSTEM_NAME, "myDb"),
              entry(DbIncubatingAttributes.DB_USER, "username"),
              entry(DbIncubatingAttributes.DB_NAME, "potatoes"),
              entry(DbIncubatingAttributes.DB_CONNECTION_STRING, "mydb:///potatoes"),
              entry(DbIncubatingAttributes.DB_STATEMENT, "SELECT * FROM potato WHERE id=?"),
              entry(DbIncubatingAttributes.DB_OPERATION, "SELECT"),
              entry(DbIncubatingAttributes.DB_SQL_TABLE, "potato"),
              entry(DbIncubatingAttributes.DB_NAMESPACE, "potatoes"),
              entry(DbIncubatingAttributes.DB_QUERY_TEXT, "SELECT * FROM potato WHERE id=?"),
              entry(DbIncubatingAttributes.DB_OPERATION_NAME, "SELECT"),
              entry(DbIncubatingAttributes.DB_COLLECTION_NAME, "potato"));
    } else if (SemconvStability.emitOldDatabaseSemconv()) {
      assertThat(startAttributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_SYSTEM, "myDb"),
              entry(DbIncubatingAttributes.DB_USER, "username"),
              entry(DbIncubatingAttributes.DB_NAME, "potatoes"),
              entry(DbIncubatingAttributes.DB_CONNECTION_STRING, "mydb:///potatoes"),
              entry(DbIncubatingAttributes.DB_STATEMENT, "SELECT * FROM potato WHERE id=?"),
              entry(DbIncubatingAttributes.DB_OPERATION, "SELECT"),
              entry(DbIncubatingAttributes.DB_SQL_TABLE, "potato"));
    } else if (SemconvStability.emitStableDatabaseSemconv()) {
      assertThat(startAttributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_SYSTEM_NAME, "myDb"),
              entry(DbIncubatingAttributes.DB_NAMESPACE, "potatoes"),
              entry(DbIncubatingAttributes.DB_QUERY_TEXT, "SELECT * FROM potato WHERE id=?"),
              entry(DbIncubatingAttributes.DB_OPERATION_NAME, "SELECT"),
              entry(DbIncubatingAttributes.DB_COLLECTION_NAME, "potato"));
    }

    assertThat(endAttributes.build().isEmpty()).isTrue();
  }

  @Test
  void shouldNotExtractTableIfAttributeIsNotSet() {
    // given
    Map<String, Object> request = new HashMap<>();
    request.put("db.statement", "SELECT *");

    Context context = Context.root();

    AttributesExtractor<Map<String, Object>, Void> underTest =
        SqlClientAttributesExtractor.create(new TestAttributesGetter());

    // when
    AttributesBuilder attributes = Attributes.builder();
    underTest.onStart(attributes, context, request);

    // then
    if (SemconvStability.emitStableDatabaseSemconv() && SemconvStability.emitOldDatabaseSemconv()) {
      assertThat(attributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_STATEMENT, "SELECT *"),
              entry(DbIncubatingAttributes.DB_OPERATION, "SELECT"),
              entry(DbIncubatingAttributes.DB_QUERY_TEXT, "SELECT *"),
              entry(DbIncubatingAttributes.DB_OPERATION_NAME, "SELECT"));
    } else if (SemconvStability.emitOldDatabaseSemconv()) {
      assertThat(attributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_STATEMENT, "SELECT *"),
              entry(DbIncubatingAttributes.DB_OPERATION, "SELECT"));
    } else if (SemconvStability.emitStableDatabaseSemconv()) {
      assertThat(attributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_QUERY_TEXT, "SELECT *"),
              entry(DbIncubatingAttributes.DB_OPERATION_NAME, "SELECT"));
    }
  }

  @Test
  @SuppressWarnings("deprecation") // to support old database semantic conventions
  void shouldExtractTableToSpecifiedKey() {
    // given
    Map<String, Object> request = new HashMap<>();
    request.put("db.statement", "SELECT * FROM table");

    Context context = Context.root();

    AttributesExtractor<Map<String, Object>, Void> underTest =
        SqlClientAttributesExtractor.<Map<String, Object>, Void>builder(new TestAttributesGetter())
            .setTableAttribute(DbIncubatingAttributes.DB_CASSANDRA_TABLE)
            .build();

    // when
    AttributesBuilder attributes = Attributes.builder();
    underTest.onStart(attributes, context, request);

    // then
    if (SemconvStability.emitStableDatabaseSemconv() && SemconvStability.emitOldDatabaseSemconv()) {
      assertThat(attributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_STATEMENT, "SELECT * FROM table"),
              entry(DbIncubatingAttributes.DB_OPERATION, "SELECT"),
              entry(DbIncubatingAttributes.DB_CASSANDRA_TABLE, "table"),
              entry(DbIncubatingAttributes.DB_QUERY_TEXT, "SELECT * FROM table"),
              entry(DbIncubatingAttributes.DB_OPERATION_NAME, "SELECT"),
              entry(DbIncubatingAttributes.DB_COLLECTION_NAME, "table"));
    } else if (SemconvStability.emitOldDatabaseSemconv()) {
      assertThat(attributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_STATEMENT, "SELECT * FROM table"),
              entry(DbIncubatingAttributes.DB_OPERATION, "SELECT"),
              entry(DbIncubatingAttributes.DB_CASSANDRA_TABLE, "table"));
    } else if (SemconvStability.emitStableDatabaseSemconv()) {
      assertThat(attributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_QUERY_TEXT, "SELECT * FROM table"),
              entry(DbIncubatingAttributes.DB_OPERATION_NAME, "SELECT"),
              entry(DbIncubatingAttributes.DB_COLLECTION_NAME, "table"));
    }
  }

  @Test
  void shouldExtractNoAttributesIfNoneAreAvailable() {
    // when
    AttributesExtractor<Map<String, Object>, Void> underTest =
        SqlClientAttributesExtractor.create(new TestAttributesGetter());

    // when
    AttributesBuilder attributes = Attributes.builder();
    underTest.onStart(attributes, Context.root(), Collections.emptyMap());

    // then
    assertThat(attributes.build().isEmpty()).isTrue();
  }

  @Test
  void shouldExtractSingleQueryBatchAttributes() {
    // given
    Map<String, Object> request = new HashMap<>();
    request.put("db.name", "potatoes");
    request.put("db.statements", singleton("INSERT INTO potato VALUES(?)"));
    request.put("db.operation.batch.size", 2L);

    Context context = Context.root();

    AttributesExtractor<Map<String, Object>, Void> underTest =
        SqlClientAttributesExtractor.create(new TestMultiAttributesGetter());

    // when
    AttributesBuilder startAttributes = Attributes.builder();
    underTest.onStart(startAttributes, context, request);

    AttributesBuilder endAttributes = Attributes.builder();
    underTest.onEnd(endAttributes, context, request, null, null);

    // then
    if (SemconvStability.emitStableDatabaseSemconv() && SemconvStability.emitOldDatabaseSemconv()) {
      assertThat(startAttributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_NAME, "potatoes"),
              entry(DbIncubatingAttributes.DB_STATEMENT, "INSERT INTO potato VALUES(?)"),
              entry(DbIncubatingAttributes.DB_OPERATION, "INSERT"),
              entry(DbIncubatingAttributes.DB_SQL_TABLE, "potato"),
              entry(DbIncubatingAttributes.DB_NAMESPACE, "potatoes"),
              entry(DbIncubatingAttributes.DB_QUERY_TEXT, "INSERT INTO potato VALUES(?)"),
              entry(DbIncubatingAttributes.DB_OPERATION_NAME, "BATCH INSERT"),
              entry(DbIncubatingAttributes.DB_COLLECTION_NAME, "potato"),
              entry(DbIncubatingAttributes.DB_OPERATION_BATCH_SIZE, 2L));
    } else if (SemconvStability.emitOldDatabaseSemconv()) {
      assertThat(startAttributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_NAME, "potatoes"),
              entry(DbIncubatingAttributes.DB_STATEMENT, "INSERT INTO potato VALUES(?)"),
              entry(DbIncubatingAttributes.DB_OPERATION, "INSERT"),
              entry(DbIncubatingAttributes.DB_SQL_TABLE, "potato"));
    } else if (SemconvStability.emitStableDatabaseSemconv()) {
      assertThat(startAttributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_NAMESPACE, "potatoes"),
              entry(DbIncubatingAttributes.DB_QUERY_TEXT, "INSERT INTO potato VALUES(?)"),
              entry(DbIncubatingAttributes.DB_OPERATION_NAME, "BATCH INSERT"),
              entry(DbIncubatingAttributes.DB_COLLECTION_NAME, "potato"),
              entry(DbIncubatingAttributes.DB_OPERATION_BATCH_SIZE, 2L));
    }

    assertThat(endAttributes.build().isEmpty()).isTrue();
  }

  @Test
  void shouldExtractMultiQueryBatchAttributes() {
    // given
    Map<String, Object> request = new HashMap<>();
    request.put("db.name", "potatoes");
    request.put(
        "db.statements",
        Arrays.asList("INSERT INTO potato VALUES(1)", "INSERT INTO potato VALUES(2)"));
    request.put("db.operation.batch.size", 2L);

    Context context = Context.root();

    AttributesExtractor<Map<String, Object>, Void> underTest =
        SqlClientAttributesExtractor.create(new TestMultiAttributesGetter());

    // when
    AttributesBuilder startAttributes = Attributes.builder();
    underTest.onStart(startAttributes, context, request);

    AttributesBuilder endAttributes = Attributes.builder();
    underTest.onEnd(endAttributes, context, request, null, null);

    // then
    if (SemconvStability.emitStableDatabaseSemconv() && SemconvStability.emitOldDatabaseSemconv()) {
      assertThat(startAttributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_NAME, "potatoes"),
              entry(DbIncubatingAttributes.DB_NAMESPACE, "potatoes"),
              entry(DbIncubatingAttributes.DB_QUERY_TEXT, "INSERT INTO potato VALUES(?)"),
              entry(DbIncubatingAttributes.DB_OPERATION_NAME, "BATCH INSERT"),
              entry(DbIncubatingAttributes.DB_COLLECTION_NAME, "potato"),
              entry(DbIncubatingAttributes.DB_OPERATION_BATCH_SIZE, 2L));
    } else if (SemconvStability.emitOldDatabaseSemconv()) {
      assertThat(startAttributes.build())
          .containsOnly(entry(DbIncubatingAttributes.DB_NAME, "potatoes"));
    } else if (SemconvStability.emitStableDatabaseSemconv()) {
      assertThat(startAttributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_NAMESPACE, "potatoes"),
              entry(DbIncubatingAttributes.DB_QUERY_TEXT, "INSERT INTO potato VALUES(?)"),
              entry(DbIncubatingAttributes.DB_OPERATION_NAME, "BATCH INSERT"),
              entry(DbIncubatingAttributes.DB_COLLECTION_NAME, "potato"),
              entry(DbIncubatingAttributes.DB_OPERATION_BATCH_SIZE, 2L));
    }

    assertThat(endAttributes.build().isEmpty()).isTrue();
  }

  @Test
  void shouldIgnoreBatchSizeOne() {
    // given
    Map<String, Object> request = new HashMap<>();
    request.put("db.name", "potatoes");
    request.put("db.statements", singleton("INSERT INTO potato VALUES(?)"));
    request.put("db.operation.batch.size", 1L);

    Context context = Context.root();

    AttributesExtractor<Map<String, Object>, Void> underTest =
        SqlClientAttributesExtractor.create(new TestMultiAttributesGetter());

    // when
    AttributesBuilder startAttributes = Attributes.builder();
    underTest.onStart(startAttributes, context, request);

    AttributesBuilder endAttributes = Attributes.builder();
    underTest.onEnd(endAttributes, context, request, null, null);

    // then
    if (SemconvStability.emitStableDatabaseSemconv() && SemconvStability.emitOldDatabaseSemconv()) {
      assertThat(startAttributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_NAME, "potatoes"),
              entry(DbIncubatingAttributes.DB_STATEMENT, "INSERT INTO potato VALUES(?)"),
              entry(DbIncubatingAttributes.DB_OPERATION, "INSERT"),
              entry(DbIncubatingAttributes.DB_SQL_TABLE, "potato"),
              entry(DbIncubatingAttributes.DB_NAMESPACE, "potatoes"),
              entry(DbIncubatingAttributes.DB_QUERY_TEXT, "INSERT INTO potato VALUES(?)"),
              entry(DbIncubatingAttributes.DB_OPERATION_NAME, "INSERT"),
              entry(DbIncubatingAttributes.DB_COLLECTION_NAME, "potato"));
    } else if (SemconvStability.emitOldDatabaseSemconv()) {
      assertThat(startAttributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_NAME, "potatoes"),
              entry(DbIncubatingAttributes.DB_STATEMENT, "INSERT INTO potato VALUES(?)"),
              entry(DbIncubatingAttributes.DB_OPERATION, "INSERT"),
              entry(DbIncubatingAttributes.DB_SQL_TABLE, "potato"));
    } else if (SemconvStability.emitStableDatabaseSemconv()) {
      assertThat(startAttributes.build())
          .containsOnly(
              entry(DbIncubatingAttributes.DB_NAMESPACE, "potatoes"),
              entry(DbIncubatingAttributes.DB_QUERY_TEXT, "INSERT INTO potato VALUES(?)"),
              entry(DbIncubatingAttributes.DB_OPERATION_NAME, "INSERT"),
              entry(DbIncubatingAttributes.DB_COLLECTION_NAME, "potato"));
    }

    assertThat(endAttributes.build().isEmpty()).isTrue();
  }
}
