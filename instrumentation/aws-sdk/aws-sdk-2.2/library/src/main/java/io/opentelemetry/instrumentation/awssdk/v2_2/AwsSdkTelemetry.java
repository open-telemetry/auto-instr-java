/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.awssdk.v2_2.internal.AwsSdkInstrumenterFactory;
import io.opentelemetry.instrumentation.awssdk.v2_2.internal.Response;
import io.opentelemetry.instrumentation.awssdk.v2_2.internal.SqsImpl;
import io.opentelemetry.instrumentation.awssdk.v2_2.internal.SqsProcessRequest;
import io.opentelemetry.instrumentation.awssdk.v2_2.internal.SqsReceiveRequest;
import io.opentelemetry.instrumentation.awssdk.v2_2.internal.TracingExecutionInterceptor;
import io.opentelemetry.javaagent.tooling.muzzle.NoMuzzle;
import java.util.List;
import javax.annotation.Nullable;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Entrypoint to OpenTelemetry instrumentation of the AWS SDK. Register the {@link
 * ExecutionInterceptor} returned by {@link #newExecutionInterceptor()} with an SDK client to have
 * all requests traced.
 *
 * <pre>{@code
 * DynamoDbClient dynamoDb = DynamoDbClient.builder()
 *     .overrideConfiguration(ClientOverrideConfiguration.builder()
 *         .addExecutionInterceptor(AwsSdkTelemetry.create(openTelemetry).newExecutionInterceptor())
 *         .build())
 *     .build();
 * }</pre>
 */
public class AwsSdkTelemetry {

  /** Returns a new {@link AwsSdkTelemetry} configured with the given {@link OpenTelemetry}. */
  public static AwsSdkTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  /**
   * Returns a new {@link AwsSdkTelemetryBuilder} configured with the given {@link OpenTelemetry}.
   */
  public static AwsSdkTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new AwsSdkTelemetryBuilder(openTelemetry);
  }

  private final Instrumenter<ExecutionAttributes, Response> requestInstrumenter;
  private final Instrumenter<SqsReceiveRequest, Response> consumerReceiveInstrumenter;
  private final Instrumenter<SqsProcessRequest, Response> consumerProcessInstrumenter;
  private final Instrumenter<ExecutionAttributes, Response> producerInstrumenter;
  private final Instrumenter<ExecutionAttributes, Response> dynamoDbInstrumenter;
  private final Instrumenter<ExecutionAttributes, Response> bedrockRuntimeInstrumenter;
  private final boolean captureExperimentalSpanAttributes;
  @Nullable private final TextMapPropagator messagingPropagator;
  private final boolean useXrayPropagator;
  private final boolean recordIndividualHttpError;

  AwsSdkTelemetry(
      OpenTelemetry openTelemetry,
      List<String> capturedHeaders,
      boolean captureExperimentalSpanAttributes,
      boolean useMessagingPropagator,
      boolean useXrayPropagator,
      boolean recordIndividualHttpError,
      boolean messagingReceiveInstrumentationEnabled) {
    this.useXrayPropagator = useXrayPropagator;
    this.messagingPropagator =
        useMessagingPropagator ? openTelemetry.getPropagators().getTextMapPropagator() : null;

    AwsSdkInstrumenterFactory instrumenterFactory =
        new AwsSdkInstrumenterFactory(
            openTelemetry,
            messagingPropagator,
            capturedHeaders,
            captureExperimentalSpanAttributes,
            messagingReceiveInstrumentationEnabled,
            useXrayPropagator);

    this.requestInstrumenter = instrumenterFactory.requestInstrumenter();
    this.consumerReceiveInstrumenter = instrumenterFactory.consumerReceiveInstrumenter();
    this.consumerProcessInstrumenter = instrumenterFactory.consumerProcessInstrumenter();
    this.producerInstrumenter = instrumenterFactory.producerInstrumenter();
    this.dynamoDbInstrumenter = instrumenterFactory.dynamoDbInstrumenter();
    this.bedrockRuntimeInstrumenter = instrumenterFactory.bedrockRuntimeInstrumenter();
    this.captureExperimentalSpanAttributes = captureExperimentalSpanAttributes;
    this.recordIndividualHttpError = recordIndividualHttpError;
  }

  /**
   * Returns a new {@link ExecutionInterceptor} that can be used with methods like {@link
   * ClientOverrideConfiguration.Builder#addExecutionInterceptor(ExecutionInterceptor)}.
   */
  public ExecutionInterceptor newExecutionInterceptor() {
    return new TracingExecutionInterceptor(
        requestInstrumenter,
        consumerReceiveInstrumenter,
        consumerProcessInstrumenter,
        producerInstrumenter,
        dynamoDbInstrumenter,
        bedrockRuntimeInstrumenter,
        captureExperimentalSpanAttributes,
        messagingPropagator,
        useXrayPropagator,
        recordIndividualHttpError);
  }

  /**
   * Construct a new tracing-enable {@link SqsClient} using the provided {@link SqsClient} instance.
   */
  @NoMuzzle
  public SqsClient wrap(SqsClient sqsClient) {
    return SqsImpl.wrap(sqsClient);
  }

  /**
   * Construct a new tracing-enable {@link SqsAsyncClient} using the provided {@link SqsAsyncClient}
   * instance.
   */
  @NoMuzzle
  public SqsAsyncClient wrap(SqsAsyncClient sqsClient) {
    return SqsImpl.wrap(sqsClient);
  }
}
