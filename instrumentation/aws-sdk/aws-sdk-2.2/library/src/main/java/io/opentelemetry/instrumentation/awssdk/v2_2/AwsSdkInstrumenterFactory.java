/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributeGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;

final class AwsSdkInstrumenterFactory {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.aws-sdk-2.2";

  private static final AttributesExtractor<ExecutionAttributes, Response> rpcAttributesExtractor =
      RpcClientAttributesExtractor.create(AwsSdkRpcAttributeGetter.INSTANCE);
  private static final AwsSdkExperimentalAttributesExtractor experimentalAttributesExtractor =
      new AwsSdkExperimentalAttributesExtractor();

  static final AwsSdkHttpAttributeGetter httpAttributeGetter = new AwsSdkHttpAttributeGetter();
  static final AttributesExtractor<ExecutionAttributes, Response> httpAttributesExtractor =
      HttpClientAttributesExtractor.create(httpAttributeGetter);

  private static final AttributesExtractor<ExecutionAttributes, Response>
      httpClientSuppressionAttributesExtractor =
          new AwsSdkHttpClientSuppressionAttributesExtractor();

  private static final List<AttributesExtractor<ExecutionAttributes, Response>>
      defaultAttributesExtractors =
          Arrays.asList(rpcAttributesExtractor, httpClientSuppressionAttributesExtractor);

  private static final List<AttributesExtractor<ExecutionAttributes, Response>>
      extendedAttributesExtractors =
          Arrays.asList(
              rpcAttributesExtractor,
              experimentalAttributesExtractor,
              httpClientSuppressionAttributesExtractor);

  private static final List<AttributesExtractor<ExecutionAttributes, Response>>
      defaultConsumerAttributesExtractors =
          Arrays.asList(rpcAttributesExtractor, httpAttributesExtractor);

  private static final List<AttributesExtractor<ExecutionAttributes, Response>>
      extendedConsumerAttributesExtractors =
          Arrays.asList(
              rpcAttributesExtractor, httpAttributesExtractor, experimentalAttributesExtractor);

  private final OpenTelemetry openTelemetry;
  @Nullable private final TextMapPropagator messagingPropagator;
  private final List<String> capturedHeaders;
  private final boolean captureExperimentalSpanAttributes;
  private final boolean messagingReceiveInstrumentationEnabled;
  private final boolean useXrayPropagator;

  AwsSdkInstrumenterFactory(
      OpenTelemetry openTelemetry,
      @Nullable TextMapPropagator messagingPropagator,
      List<String> capturedHeaders,
      boolean captureExperimentalSpanAttributes,
      boolean messagingReceiveInstrumentationEnabled,
      boolean useXrayPropagator) {
    this.openTelemetry = openTelemetry;
    this.messagingPropagator = messagingPropagator;
    this.capturedHeaders = capturedHeaders;
    this.captureExperimentalSpanAttributes = captureExperimentalSpanAttributes;
    this.messagingReceiveInstrumentationEnabled = messagingReceiveInstrumentationEnabled;
    this.useXrayPropagator = useXrayPropagator;
  }

  Instrumenter<ExecutionAttributes, Response> requestInstrumenter() {
    return createInstrumenter(
        openTelemetry,
        AwsSdkInstrumenterFactory::spanName,
        SpanKindExtractor.alwaysClient(),
        attributesExtractors(),
        emptyList(),
        true);
  }

  private List<AttributesExtractor<ExecutionAttributes, Response>> attributesExtractors() {
    return captureExperimentalSpanAttributes
        ? extendedAttributesExtractors
        : defaultAttributesExtractors;
  }

  private List<AttributesExtractor<ExecutionAttributes, Response>> consumerAttributesExtractors() {
    return captureExperimentalSpanAttributes
        ? extendedConsumerAttributesExtractors
        : defaultConsumerAttributesExtractors;
  }

  private <REQUEST, RESPONSE> AttributesExtractor<REQUEST, RESPONSE> messagingAttributesExtractor(
      MessagingAttributeGetter<REQUEST, RESPONSE> getter, MessageOperation operation) {
    return MessagingAttributesExtractor.builder(getter, operation)
        .setCapturedHeaders(capturedHeaders)
        .build();
  }

  Instrumenter<SqsReceiveRequest, Response> consumerReceiveInstrumenter() {
    MessageOperation operation = MessageOperation.RECEIVE;
    SqsReceiveRequestAttributeGetter getter = SqsReceiveRequestAttributeGetter.INSTANCE;
    AttributesExtractor<SqsReceiveRequest, Response> messagingAttributeExtractor =
        messagingAttributesExtractor(getter, operation);

    return createInstrumenter(
        openTelemetry,
        MessagingSpanNameExtractor.create(getter, operation),
        SpanKindExtractor.alwaysConsumer(),
        toSqsRequestExtractors(consumerAttributesExtractors(), Function.identity()),
        singletonList(messagingAttributeExtractor),
        messagingReceiveInstrumentationEnabled);
  }

  Instrumenter<SqsProcessRequest, Void> consumerProcessInstrumenter() {
    MessageOperation operation = MessageOperation.PROCESS;
    SqsProcessRequestAttributeGetter getter = SqsProcessRequestAttributeGetter.INSTANCE;

    InstrumenterBuilder<SqsProcessRequest, Void> builder =
        Instrumenter.<SqsProcessRequest, Void>builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                MessagingSpanNameExtractor.create(getter, operation))
            .addAttributesExtractors(
                toSqsRequestExtractors(consumerAttributesExtractors(), unused -> null))
            .addAttributesExtractor(messagingAttributesExtractor(getter, operation));

    if (messagingReceiveInstrumentationEnabled) {
      builder.addSpanLinksExtractor(
          (spanLinks, parentContext, request) -> {
            Context extracted =
                SqsParentContext.ofMessage(
                    request.getMessage(), messagingPropagator, useXrayPropagator);
            spanLinks.addLink(Span.fromContext(extracted).getSpanContext());
          });
    }
    return builder.buildInstrumenter(SpanKindExtractor.alwaysConsumer());
  }

  private static <RESPONSE>
      List<AttributesExtractor<AbstractSqsRequest, RESPONSE>> toSqsRequestExtractors(
          List<AttributesExtractor<ExecutionAttributes, Response>> extractors,
          Function<RESPONSE, Response> responseConverter) {
    List<AttributesExtractor<AbstractSqsRequest, RESPONSE>> result = new ArrayList<>();
    for (AttributesExtractor<ExecutionAttributes, Response> extractor : extractors) {
      result.add(
          new AttributesExtractor<AbstractSqsRequest, RESPONSE>() {
            @Override
            public void onStart(
                AttributesBuilder attributes,
                Context parentContext,
                AbstractSqsRequest sqsRequest) {
              extractor.onStart(attributes, parentContext, sqsRequest.getRequest());
            }

            @Override
            public void onEnd(
                AttributesBuilder attributes,
                Context context,
                AbstractSqsRequest sqsRequest,
                @Nullable RESPONSE response,
                @Nullable Throwable error) {
              extractor.onEnd(
                  attributes,
                  context,
                  sqsRequest.getRequest(),
                  responseConverter.apply(response),
                  error);
            }
          });
    }
    return result;
  }

  Instrumenter<ExecutionAttributes, Response> producerInstrumenter() {
    MessageOperation operation = MessageOperation.PUBLISH;
    SqsAttributeGetter getter = SqsAttributeGetter.INSTANCE;
    AttributesExtractor<ExecutionAttributes, Response> messagingAttributeExtractor =
        messagingAttributesExtractor(getter, operation);

    return createInstrumenter(
        openTelemetry,
        MessagingSpanNameExtractor.create(getter, operation),
        SpanKindExtractor.alwaysProducer(),
        attributesExtractors(),
        singletonList(messagingAttributeExtractor),
        true);
  }

  private static <REQUEST, RESPONSE> Instrumenter<REQUEST, RESPONSE> createInstrumenter(
      OpenTelemetry openTelemetry,
      SpanNameExtractor<REQUEST> spanNameExtractor,
      SpanKindExtractor<REQUEST> spanKindExtractor,
      List<? extends AttributesExtractor<? super REQUEST, ? super RESPONSE>> attributeExtractors,
      List<AttributesExtractor<REQUEST, RESPONSE>> additionalAttributeExtractors,
      boolean enabled) {

    return Instrumenter.<REQUEST, RESPONSE>builder(
            openTelemetry, INSTRUMENTATION_NAME, spanNameExtractor)
        .addAttributesExtractors(attributeExtractors)
        .addAttributesExtractors(additionalAttributeExtractors)
        .setEnabled(enabled)
        .buildInstrumenter(spanKindExtractor);
  }

  private static String spanName(ExecutionAttributes attributes) {
    String awsServiceName = attributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME);
    String awsOperation = attributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
    return awsServiceName + "." + awsOperation;
  }
}
