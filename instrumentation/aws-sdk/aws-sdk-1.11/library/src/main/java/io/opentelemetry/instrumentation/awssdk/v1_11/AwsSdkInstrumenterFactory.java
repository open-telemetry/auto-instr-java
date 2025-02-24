/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitStableMessagingSemconv;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientMetrics;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;

final class AwsSdkInstrumenterFactory {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.aws-sdk-1.11";

  private static final AttributesExtractor<Request<?>, Response<?>> httpAttributesExtractor =
      HttpClientAttributesExtractor.create(new AwsSdkHttpAttributesGetter());
  private static final AttributesExtractor<Request<?>, Response<?>> rpcAttributesExtractor =
      RpcClientAttributesExtractor.create(AwsSdkRpcAttributesGetter.INSTANCE);
  private static final AwsSdkExperimentalAttributesExtractor experimentalAttributesExtractor =
      new AwsSdkExperimentalAttributesExtractor();
  private static final AwsSdkAttributesExtractor sdkAttributesExtractor =
      new AwsSdkAttributesExtractor();
  private static final SnsAttributesExtractor snsAttributesExtractor = new SnsAttributesExtractor();
  private static final MessagingNetworkAttributesExtractor<Request<?>, Response<?>>
      messagingNetworkAttributesExtractor =
          MessagingNetworkAttributesExtractor.create(SqsAttributesGetter.INSTANCE);

  private static final List<AttributesExtractor<Request<?>, Response<?>>>
      defaultAttributesExtractors = createAttributesExtractors(false);
  private static final List<AttributesExtractor<Request<?>, Response<?>>>
      extendedAttributesExtractors = createAttributesExtractors(true);
  private static final AwsSdkSpanNameExtractor spanName = new AwsSdkSpanNameExtractor();

  private final OpenTelemetry openTelemetry;
  private final List<String> capturedHeaders;
  private final boolean captureExperimentalSpanAttributes;
  private final boolean messagingReceiveInstrumentationEnabled;

  AwsSdkInstrumenterFactory(
      OpenTelemetry openTelemetry,
      List<String> capturedHeaders,
      boolean captureExperimentalSpanAttributes,
      boolean messagingReceiveInstrumentationEnabled) {
    this.openTelemetry = openTelemetry;
    this.capturedHeaders = capturedHeaders;
    this.captureExperimentalSpanAttributes = captureExperimentalSpanAttributes;
    this.messagingReceiveInstrumentationEnabled = messagingReceiveInstrumentationEnabled;
  }

  private static List<AttributesExtractor<Request<?>, Response<?>>> createAttributesExtractors(
      boolean includeExperimental) {
    List<AttributesExtractor<Request<?>, Response<?>>> extractors =
        new ArrayList<>(
            Arrays.asList(
                httpAttributesExtractor,
                rpcAttributesExtractor,
                snsAttributesExtractor,
                sdkAttributesExtractor,
                messagingNetworkAttributesExtractor));
    if (includeExperimental) {
      extractors.add(experimentalAttributesExtractor);
    }
    return extractors;
  }

  Instrumenter<Request<?>, Response<?>> requestInstrumenter() {
    return createInstrumenter(
        openTelemetry,
        spanName,
        SpanKindExtractor.alwaysClient(),
        attributesExtractors(),
        emptyList(),
        true);
  }

  private List<AttributesExtractor<Request<?>, Response<?>>> attributesExtractors() {
    return captureExperimentalSpanAttributes
        ? extendedAttributesExtractors
        : defaultAttributesExtractors;
  }

  private <REQUEST, RESPONSE> AttributesExtractor<REQUEST, RESPONSE> messagingAttributesExtractor(
      MessagingAttributesGetter<REQUEST, RESPONSE> getter, MessageOperation operation) {
    return MessagingAttributesExtractor.builder(getter, operation)
        .setCapturedHeaders(capturedHeaders)
        .build();
  }

  Instrumenter<SqsReceiveRequest, Response<?>> consumerReceiveInstrumenter() {
    MessageOperation operation = MessageOperation.RECEIVE;
    SqsReceiveRequestAttributesGetter getter = SqsReceiveRequestAttributesGetter.INSTANCE;
    AttributesExtractor<SqsReceiveRequest, Response<?>> messagingAttributeExtractor =
        messagingAttributesExtractor(getter, operation);

    return createInstrumenter(
        openTelemetry,
        MessagingSpanNameExtractor.create(getter, operation, getter),
        SpanKindExtractor.alwaysConsumer(),
        toSqsRequestExtractors(attributesExtractors()),
        singletonList(messagingAttributeExtractor),
        messagingReceiveInstrumentationEnabled || SemconvStability.emitStableMessagingSemconv());
  }

  Instrumenter<SqsProcessRequest, Response<?>> consumerProcessInstrumenter() {
    MessageOperation operation = MessageOperation.PROCESS;
    SqsProcessRequestAttributesGetter getter = SqsProcessRequestAttributesGetter.INSTANCE;
    AttributesExtractor<SqsProcessRequest, Response<?>> messagingAttributeExtractor =
        messagingAttributesExtractor(getter, operation);

    InstrumenterBuilder<SqsProcessRequest, Response<?>> builder =
        Instrumenter.<SqsProcessRequest, Response<?>>builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                MessagingSpanNameExtractor.create(getter, operation, getter))
            .addAttributesExtractors(toSqsRequestExtractors(attributesExtractors()))
            .addAttributesExtractor(messagingAttributeExtractor);

    if (messagingReceiveInstrumentationEnabled || SemconvStability.emitStableMessagingSemconv()) {
      builder.addSpanLinksExtractor(
          (spanLinks, parentContext, request) -> {
            Context extracted =
                SqsParentContext.ofSystemAttributes(request.getMessage().getAttributes());
            spanLinks.addLink(Span.fromContext(extracted).getSpanContext());
          });
    }
    return builder.buildInstrumenter(SpanKindExtractor.alwaysConsumer());
  }

  private static List<AttributesExtractor<AbstractSqsRequest, Response<?>>> toSqsRequestExtractors(
      List<AttributesExtractor<Request<?>, Response<?>>> extractors) {
    List<AttributesExtractor<AbstractSqsRequest, Response<?>>> result = new ArrayList<>();
    for (AttributesExtractor<Request<?>, Response<?>> extractor : extractors) {
      result.add(
          new AttributesExtractor<AbstractSqsRequest, Response<?>>() {
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
                @Nullable Response<?> response,
                @Nullable Throwable error) {
              extractor.onEnd(attributes, context, sqsRequest.getRequest(), response, error);
            }
          });
    }
    return result;
  }

  @SuppressWarnings("deprecation") // using deprecated semconv
  Instrumenter<Request<?>, Response<?>> producerInstrumenter() {
    MessageOperation operation =
        emitStableMessagingSemconv() ? MessageOperation.SEND : MessageOperation.PUBLISH;
    ;
    SqsAttributesGetter getter = SqsAttributesGetter.INSTANCE;
    AttributesExtractor<Request<?>, Response<?>> messagingAttributeExtractor =
        messagingAttributesExtractor(getter, operation);

    return createInstrumenter(
        openTelemetry,
        MessagingSpanNameExtractor.create(getter, operation, getter),
        SpanKindExtractor.alwaysProducer(),
        attributesExtractors(),
        singletonList(messagingAttributeExtractor),
        true);
  }

  Instrumenter<Request<?>, Response<?>> dynamoDbInstrumenter() {
    return createInstrumenter(
        openTelemetry,
        spanName,
        SpanKindExtractor.alwaysClient(),
        attributesExtractors(),
        builder ->
            builder
                .addAttributesExtractor(new DynamoDbAttributesExtractor())
                .addOperationMetrics(DbClientMetrics.get()),
        true);
  }

  private static <REQUEST, RESPONSE> Instrumenter<REQUEST, RESPONSE> createInstrumenter(
      OpenTelemetry openTelemetry,
      SpanNameExtractor<REQUEST> spanNameExtractor,
      SpanKindExtractor<REQUEST> spanKindExtractor,
      List<? extends AttributesExtractor<? super REQUEST, ? super RESPONSE>> attributeExtractors,
      List<AttributesExtractor<REQUEST, RESPONSE>> additionalAttributeExtractors,
      boolean enabled) {
    return createInstrumenter(
        openTelemetry,
        spanNameExtractor,
        spanKindExtractor,
        attributeExtractors,
        builder -> builder.addAttributesExtractors(additionalAttributeExtractors),
        enabled);
  }

  private static <REQUEST, RESPONSE> Instrumenter<REQUEST, RESPONSE> createInstrumenter(
      OpenTelemetry openTelemetry,
      SpanNameExtractor<REQUEST> spanNameExtractor,
      SpanKindExtractor<REQUEST> spanKindExtractor,
      List<? extends AttributesExtractor<? super REQUEST, ? super RESPONSE>> attributeExtractors,
      Consumer<InstrumenterBuilder<REQUEST, RESPONSE>> customizer,
      boolean enabled) {
    InstrumenterBuilder<REQUEST, RESPONSE> builder =
        Instrumenter.<REQUEST, RESPONSE>builder(
                openTelemetry, INSTRUMENTATION_NAME, spanNameExtractor)
            .addAttributesExtractors(attributeExtractors)
            .setEnabled(enabled);
    customizer.accept(builder);

    return builder.buildInstrumenter(spanKindExtractor);
  }
}
