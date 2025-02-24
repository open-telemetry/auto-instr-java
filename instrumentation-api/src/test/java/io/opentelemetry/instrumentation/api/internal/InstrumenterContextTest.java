/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.internal;

import static io.opentelemetry.instrumentation.testing.junit.db.SemconvStabilityUtil.maybeStable;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientSpanNameExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.SqlClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.SqlClientAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.SqlStatementInfo;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InstrumenterContextTest {

  @SuppressWarnings({"unchecked", "deprecation"}) // using deprecated DB_SQL_TABLE
  @Test
  void testSqlSanitizer() {
    String testQuery = "SELECT name FROM test WHERE id = 1";
    SqlClientAttributesGetter<Object> getter =
        new SqlClientAttributesGetter<Object>() {

          @Override
          public Collection<String> getRawQueryTexts(Object request) {
            return Collections.singletonList(testQuery);
          }
        };
    SpanNameExtractor<Object> spanNameExtractor = DbClientSpanNameExtractor.create(getter);
    AttributesExtractor<Object, Void> attributesExtractor =
        SqlClientAttributesExtractor.create(getter);

    assertThat(InstrumenterContext.isActive()).isFalse();
    InstrumenterContext.withContext(
        () -> {
          assertThat(InstrumenterContext.isActive()).isTrue();
          assertThat(InstrumenterContext.get()).isEmpty();
          assertThat(spanNameExtractor.extract(null)).isEqualTo("SELECT test");
          // verify that sanitized statement was cached, see SqlStatementSanitizerUtil
          assertThat(InstrumenterContext.get()).containsKey("sanitized-sql-map");
          Map<String, SqlStatementInfo> sanitizedMap =
              (Map<String, SqlStatementInfo>) InstrumenterContext.get().get("sanitized-sql-map");
          assertThat(sanitizedMap).containsKey(testQuery);

          // replace cached sanitization result to verify it is used
          sanitizedMap.put(
              testQuery,
              SqlStatementInfo.create("SELECT name2 FROM test2 WHERE id = ?", "SELECT", "test2"));
          {
            AttributesBuilder builder = Attributes.builder();
            attributesExtractor.onStart(builder, Context.root(), null);
            assertThat(builder.build().get(maybeStable(DbIncubatingAttributes.DB_SQL_TABLE)))
                .isEqualTo("test2");
          }

          // clear cached value to see whether it gets recomputed correctly
          sanitizedMap.clear();
          {
            AttributesBuilder builder = Attributes.builder();
            attributesExtractor.onStart(builder, Context.root(), null);
            assertThat(builder.build().get(maybeStable(DbIncubatingAttributes.DB_SQL_TABLE)))
                .isEqualTo("test");
          }

          return null;
        });
  }
}
