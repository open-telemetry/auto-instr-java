/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kafkastreams;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaProcessRequest;

public final class StateHolder {
  public static final ThreadLocal<StateHolder> HOLDER = new ThreadLocal<>();

  private KafkaProcessRequest request;
  private Context context;
  private Scope scope;

  public void closeScope() {
    scope.close();
  }

  public KafkaProcessRequest getRequest() {
    return request;
  }

  public Context getContext() {
    return context;
  }

  public void set(KafkaProcessRequest request, Context context, Scope scope) {
    this.request = request;
    this.context = context;
    this.scope = scope;
  }
}
