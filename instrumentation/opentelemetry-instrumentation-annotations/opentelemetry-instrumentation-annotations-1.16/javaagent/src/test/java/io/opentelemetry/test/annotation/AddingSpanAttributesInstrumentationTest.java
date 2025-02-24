/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.test.annotation;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_FUNCTION;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_NAMESPACE;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AddingSpanAttributesInstrumentationTest {

  @RegisterExtension
  public static final AgentInstrumentationExtension testing =
      AgentInstrumentationExtension.create();

  @SuppressWarnings("deprecation") // using deprecated semconv
  @Test
  void captureAttributesInNewSpan() {
    testing.runWithSpan(
        "root",
        () ->
            new ExtractAttributesUsingAddingSpanAttributes()
                .withSpanTakesPrecedence("foo", "bar", null, "baz"));

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("root").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(
                            "ExtractAttributesUsingAddingSpanAttributes.withSpanTakesPrecedence")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParentSpanId(trace.getSpan(0).getSpanId())
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                CODE_NAMESPACE,
                                ExtractAttributesUsingAddingSpanAttributes.class.getName()),
                            equalTo(CODE_FUNCTION, "withSpanTakesPrecedence"),
                            equalTo(stringKey("implicitName"), "foo"),
                            equalTo(stringKey("explicitName"), "bar"))));
  }

  @Test
  void captureAttributesInCurrentSpan() {
    testing.runWithSpan(
        "root",
        () ->
            new ExtractAttributesUsingAddingSpanAttributes()
                .withSpanAttributes("foo", "bar", null, "baz"));

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("root")
                        .hasKind(SpanKind.INTERNAL)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(stringKey("implicitName"), "foo"),
                            equalTo(stringKey("explicitName"), "bar"))));
  }

  @Test
  void noExistingSpan() {
    new ExtractAttributesUsingAddingSpanAttributes().withSpanAttributes("foo", "bar", null, "baz");

    assertThat(testing.waitForTraces(0)).isEmpty();
  }

  @Test
  void overwriteAttributes() {
    testing.runWithSpan(
        "root",
        () -> {
          Span.current().setAttribute("implicitName", "willbegone");
          Span.current().setAttribute("keep", "willbekept");
          new ExtractAttributesUsingAddingSpanAttributes()
              .withSpanAttributes("foo", "bar", null, "baz");
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("root")
                        .hasKind(SpanKind.INTERNAL)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(stringKey("keep"), "willbekept"),
                            equalTo(stringKey("implicitName"), "foo"),
                            equalTo(stringKey("explicitName"), "bar"))));
  }

  @Test
  void multiMethodOverwriteAttributes() {
    testing.runWithSpan(
        "root",
        () -> {
          Span.current().setAttribute("implicitName", "willbegone");
          Span.current().setAttribute("keep", "willbekept");
          new ExtractAttributesUsingAddingSpanAttributes()
              .withSpanAttributesParent("parentbegone", "parentbegone", null, "parentbegone");
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("root")
                        .hasKind(SpanKind.INTERNAL)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(stringKey("keep"), "willbekept"),
                            equalTo(stringKey("implicitName"), "foo"),
                            equalTo(stringKey("explicitName"), "bar"))));
  }
}
