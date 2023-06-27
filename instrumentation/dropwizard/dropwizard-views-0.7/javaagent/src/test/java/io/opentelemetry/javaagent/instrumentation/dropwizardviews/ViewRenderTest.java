package io.opentelemetry.javaagent.instrumentation.dropwizardviews;/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dropwizard.views.View;
import io.dropwizard.views.ViewRenderer;
import io.dropwizard.views.freemarker.FreemarkerViewRenderer;
import io.dropwizard.views.mustache.MustacheViewRenderer;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ViewRenderTest{
  private static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  private static Stream<Arguments> provideParameters() {
    return Stream.of(
        Arguments.of(new FreemarkerViewRenderer(), "/views/ftl/utf8.ftl"),
        Arguments.of(new MustacheViewRenderer(), "/views/mustache/utf8.mustache"),
        Arguments.of(new FreemarkerViewRenderer(), "/views/ftl/utf8.ftl"),
        Arguments.of(new MustacheViewRenderer(),  "/views/mustache/utf8.mustache"));
  }

  @ParameterizedTest
  @MethodSource("provideParameters")
  void testSpan(ViewRenderer renderer, String template) throws IOException {
    View view = new View(template, StandardCharsets.UTF_8) {};
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    testing.runWithSpan("parent", () -> {
      renderer.render(view, Locale.ENGLISH, outputStream);
    });
    assertTrue(outputStream.toString("UTF-8").contains("This is an example of a view"));
    testing.waitAndAssertTraces(
      trace ->
          trace
              .hasSize(2)
              .hasSpansSatisfyingExactly(
                span ->
                    span.hasName("parent")
                      .hasKind(SpanKind.INTERNAL)
                      .hasNoParent(),
                span ->
                    span.hasName("Render " + template)
                        .hasParent(trace.getSpan(0))
              )
    );
  }

  @Test
  void testDontCreateSpanWithoutParent() throws InterruptedException, IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    View view = new View("/views/ftl/utf8.ftl", StandardCharsets.UTF_8) {};
    new FreemarkerViewRenderer().render(view, Locale.ENGLISH, outputStream);
    Thread.sleep(500);
    assertEquals(0,testing.spans().size());
  }

  @AfterEach
  void cleanUp() {
    testing.clearData();
  }
}
