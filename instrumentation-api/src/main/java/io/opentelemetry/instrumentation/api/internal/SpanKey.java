/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.internal;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import javax.annotation.Nullable;

/**
 * Makes span keys for specific instrumentation accessible to enrich and suppress spans.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class SpanKey {

  /* Context keys */

  // span kind keys
  private static final ContextKey<Span> KIND_SERVER_KEY =
      ContextKey.named("opentelemetry-traces-span-key-kind-server");
  private static final ContextKey<Span> KIND_CLIENT_KEY =
      ContextKey.named("opentelemetry-traces-span-key-kind-client");
  private static final ContextKey<Span> KIND_CONSUMER_KEY =
      ContextKey.named("opentelemetry-traces-span-key-kind-consumer");
  private static final ContextKey<Span> KIND_PRODUCER_KEY =
      ContextKey.named("opentelemetry-traces-span-key-kind-producer");

  // semantic convention keys
  private static final ContextKey<Span> HTTP_SERVER_KEY =
      ContextKey.named("opentelemetry-traces-span-key-http-server");
  private static final ContextKey<Span> RPC_SERVER_KEY =
      ContextKey.named("opentelemetry-traces-span-key-rpc-server");

  private static final ContextKey<Span> HTTP_CLIENT_KEY =
      ContextKey.named("opentelemetry-traces-span-key-http-client");
  private static final ContextKey<Span> RPC_CLIENT_KEY =
      ContextKey.named("opentelemetry-traces-span-key-rpc-client");
  private static final ContextKey<Span> DB_CLIENT_KEY =
      ContextKey.named("opentelemetry-traces-span-key-db-client");

  private static final ContextKey<Span> PRODUCER_KEY =
      ContextKey.named("opentelemetry-traces-span-key-producer");
  private static final ContextKey<Span> CONSUMER_RECEIVE_KEY =
      ContextKey.named("opentelemetry-traces-span-key-consumer-receive");
  private static final ContextKey<Span> CONSUMER_PROCESS_KEY =
      ContextKey.named("opentelemetry-traces-span-key-consumer-process");

  /* Span keys */

  // span kind keys
  public static final SpanKey KIND_SERVER = new SpanKey(KIND_SERVER_KEY);
  public static final SpanKey KIND_CLIENT = new SpanKey(KIND_CLIENT_KEY);
  public static final SpanKey KIND_CONSUMER = new SpanKey(KIND_CONSUMER_KEY);
  public static final SpanKey KIND_PRODUCER = new SpanKey(KIND_PRODUCER_KEY);

  // semantic convention keys
  public static final SpanKey HTTP_SERVER = new SpanKey(HTTP_SERVER_KEY);
  public static final SpanKey RPC_SERVER = new SpanKey(RPC_SERVER_KEY);

  public static final SpanKey HTTP_CLIENT = new SpanKey(HTTP_CLIENT_KEY);
  public static final SpanKey RPC_CLIENT = new SpanKey(RPC_CLIENT_KEY);
  public static final SpanKey DB_CLIENT = new SpanKey(DB_CLIENT_KEY);

  public static final SpanKey PRODUCER = new SpanKey(PRODUCER_KEY);
  public static final SpanKey CONSUMER_RECEIVE = new SpanKey(CONSUMER_RECEIVE_KEY);
  public static final SpanKey CONSUMER_PROCESS = new SpanKey(CONSUMER_PROCESS_KEY);

  private final ContextKey<Span> key;

  private SpanKey(ContextKey<Span> key) {
    this.key = key;
  }

  // instumented by SpanKeyInstrumentation
  public Context storeInContext(Context context, Span span) {
    return context.with(key, span);
  }

  @Nullable
  // instumented by SpanKeyInstrumentation
  public Span fromContextOrNull(Context context) {
    return context.get(key);
  }

  @Override
  public String toString() {
    return key.toString();
  }
}
