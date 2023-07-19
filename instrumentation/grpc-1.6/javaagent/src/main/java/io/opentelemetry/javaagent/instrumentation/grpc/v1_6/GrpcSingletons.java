/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.grpc.v1_6;

import static java.util.Collections.emptyList;

import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.ServerInterceptor;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import io.opentelemetry.instrumentation.grpc.v1_6.internal.ContextStorageBridge;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;
import java.util.List;

// Holds singleton references.
public final class GrpcSingletons {

  public static final ClientInterceptor CLIENT_INTERCEPTOR;

  public static final ServerInterceptor SERVER_INTERCEPTOR;

  public static final Context.Storage STORAGE;

  static {
    boolean propagateGrpcDeadline =
        InstrumentationConfig.get()
            .getBoolean("otel.instrumentation.grpc.propagate-grpc-deadline", false);
    STORAGE = new ContextStorageBridge(propagateGrpcDeadline);

    boolean experimentalSpanAttributes =
        InstrumentationConfig.get()
            .getBoolean("otel.instrumentation.grpc.experimental-span-attributes", false);

    List<String> clientRequestMetadata =
        InstrumentationConfig.get()
            .getList("otel.instrumentation.grpc.capture-metadata.client.request", emptyList());
    List<String> serverRequestMetadata =
        InstrumentationConfig.get()
            .getList("otel.instrumentation.grpc.capture-metadata.server.request", emptyList());

    GrpcTelemetry telemetry =
        GrpcTelemetry.builder(GlobalOpenTelemetry.get())
            .setCaptureExperimentalSpanAttributes(experimentalSpanAttributes)
            .setCapturedClientRequestMetadata(clientRequestMetadata)
            .setCapturedServerRequestMetadata(serverRequestMetadata)
            .build();

    CLIENT_INTERCEPTOR = telemetry.newClientInterceptor();
    SERVER_INTERCEPTOR = telemetry.newServerInterceptor();
  }

  private GrpcSingletons() {}
}
