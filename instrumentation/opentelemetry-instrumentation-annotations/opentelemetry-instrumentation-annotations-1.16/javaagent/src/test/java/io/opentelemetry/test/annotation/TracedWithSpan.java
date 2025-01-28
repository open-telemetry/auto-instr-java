/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.test.annotation;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class TracedWithSpan {

  @WithSpan
  public String otel() {
    return "hello!";
  }

  @WithSpan("manualName")
  public String namedOtel() {
    return "hello!";
  }

  @WithSpan
  public String ignored() {
    return "hello!";
  }

  @WithSpan(kind = SpanKind.PRODUCER)
  public String someKind() {
    return "hello!";
  }

  @WithSpan(kind = SpanKind.SERVER)
  public String server() {
    return otel();
  }

  @WithSpan
  public String withSpanAttributes(
      @SpanAttribute String implicitName,
      @SpanAttribute("explicitName") String parameter,
      @SpanAttribute("nullAttribute") String nullAttribute,
      String notTraced) {

    return "hello!";
  }

  @WithSpan
  public CompletionStage<String> completionStage(CompletableFuture<String> future) {
    return future;
  }

  @WithSpan
  public CompletableFuture<String> completableFuture(CompletableFuture<String> future) {
    return future;
  }
}
