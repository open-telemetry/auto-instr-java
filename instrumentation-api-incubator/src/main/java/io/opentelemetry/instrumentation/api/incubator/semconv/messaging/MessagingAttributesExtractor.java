/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.messaging;

import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import io.opentelemetry.instrumentation.api.internal.SpanKey;
import io.opentelemetry.instrumentation.api.internal.SpanKeyProvider;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Extractor of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/main/docs/messaging/messaging-spans.md">messaging
 * attributes</a>.
 *
 * <p>This class delegates to a type-specific {@link MessagingAttributesGetter} for individual
 * attribute extraction from request/response objects.
 */
public final class MessagingAttributesExtractor<REQUEST, RESPONSE>
    implements AttributesExtractor<REQUEST, RESPONSE>, SpanKeyProvider {

  // copied from io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes (stable
  // attributes)
  private static final AttributeKey<String> MESSAGING_OPERATION_NAME =
      AttributeKey.stringKey("messaging.operation.name");
  private static final AttributeKey<String> MESSAGING_SYSTEM =
      AttributeKey.stringKey("messaging.system");
  private static final AttributeKey<Long> MESSAGING_BATCH_MESSAGE_COUNT =
      AttributeKey.longKey("messaging.batch.message_count");
  // Messaging specific
  private static final AttributeKey<String> MESSAGING_CONSUMER_GROUP_NAME =
      AttributeKey.stringKey("messaging.consumer.group.name");
  private static final AttributeKey<Boolean> MESSAGING_DESTINATION_ANONYMOUS =
      AttributeKey.booleanKey("messaging.destination.anonymous");
  private static final AttributeKey<String> MESSAGING_DESTINATION_NAME =
      AttributeKey.stringKey("messaging.destination.name");
  // Messaging specific
  private static final AttributeKey<String> MESSAGING_DESTINATION_SUBSCRIPTION_NAME =
      AttributeKey.stringKey("messaging.destination.subscription.name");
  private static final AttributeKey<String> MESSAGING_DESTINATION_TEMPLATE =
      AttributeKey.stringKey("messaging.destination.template");
  private static final AttributeKey<Boolean> MESSAGING_DESTINATION_TEMPORARY =
      AttributeKey.booleanKey("messaging.destination.temporary");
  private static final AttributeKey<String> MESSAGING_OPERATION_TYPE =
      AttributeKey.stringKey("messaging.operation.type");
  private static final AttributeKey<String> MESSAGING_CLIENT_ID_STABLE =
      AttributeKey.stringKey("messaging.client.id");
  private static final AttributeKey<String> MESSAGING_DESTINATION_PARTITION_ID =
      AttributeKey.stringKey("messaging.destination.partition.id");
  private static final AttributeKey<String> MESSAGING_MESSAGE_CONVERSATION_ID =
      AttributeKey.stringKey("messaging.message.conversation_id");
  private static final AttributeKey<String> MESSAGING_MESSAGE_ID =
      AttributeKey.stringKey("messaging.message.id");
  private static final AttributeKey<Long> MESSAGING_MESSAGE_BODY_SIZE =
      AttributeKey.longKey("messaging.message.body.size");
  private static final AttributeKey<Long> MESSAGING_MESSAGE_ENVELOPE_SIZE =
      AttributeKey.longKey("messaging.message.envelope.size");

  // copied from MessagingIncubatingAttributes (old attributes)
  @Deprecated
  private static final AttributeKey<String> MESSAGING_CLIENT_ID =
      AttributeKey.stringKey("messaging.client_id");

  @Deprecated
  private static final AttributeKey<String> MESSAGING_OPERATION =
      AttributeKey.stringKey("messaging.operation");

  static final String TEMP_DESTINATION_NAME = "(temporary)";
  static final String ANONYMOUS_DESTINATION_NAME = "(anonymous)";

  /**
   * Creates the messaging attributes extractor for the given {@link MessageOperation operation}
   * with default configuration.
   */
  public static <REQUEST, RESPONSE> AttributesExtractor<REQUEST, RESPONSE> create(
      MessagingAttributesGetter<REQUEST, RESPONSE> getter, MessageOperation operation) {
    return builder(getter, operation).build();
  }

  /**
   * Returns a new {@link MessagingAttributesExtractorBuilder} for the given {@link MessageOperation
   * operation} that can be used to configure the messaging attributes extractor.
   */
  public static <REQUEST, RESPONSE> MessagingAttributesExtractorBuilder<REQUEST, RESPONSE> builder(
      MessagingAttributesGetter<REQUEST, RESPONSE> getter, MessageOperation operation) {
    return new MessagingAttributesExtractorBuilder<>(getter, operation);
  }

  private final MessagingAttributesGetter<REQUEST, RESPONSE> getter;
  private final MessageOperation operation;
  private final List<String> capturedHeaders;

  MessagingAttributesExtractor(
      MessagingAttributesGetter<REQUEST, RESPONSE> getter,
      MessageOperation operation,
      List<String> capturedHeaders) {
    this.getter = getter;
    this.operation = operation;
    this.capturedHeaders = CapturedMessageHeadersUtil.lowercase(capturedHeaders);
  }

  @Override
  @SuppressWarnings("deprecation") // using deprecated semconv
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    internalSet(attributes, MESSAGING_SYSTEM, getter.getSystem(request));

    // Old messaging attributes
    if (SemconvStability.emitOldMessagingSemconv()) {
      internalSet(attributes, MESSAGING_CLIENT_ID, getter.getClientId(request));
      if (operation != null) { // in old implementation operation could be null
        internalSet(attributes, MESSAGING_OPERATION, operation.operationName());
      }
    }

    // New, stable attributes
    if (SemconvStability.emitStableMessagingSemconv()) {
      internalSet(attributes, MESSAGING_CLIENT_ID_STABLE, getter.getClientId(request));
      internalSet(attributes, MESSAGING_OPERATION_TYPE, operation.operationType());
      internalSet(
          attributes, MESSAGING_OPERATION_NAME, getter.getOperationName(request, operation));
      internalSet(attributes, MESSAGING_CONSUMER_GROUP_NAME, getter.getConsumerGroupName(request));
      internalSet(
          attributes,
          MESSAGING_DESTINATION_SUBSCRIPTION_NAME,
          getter.getDestinationSubscriptionName(request));
    }

    // Unchanged attributes from 1.25.0 to stable
    boolean isTemporaryDestination = getter.isTemporaryDestination(request);
    boolean isAnonymousDestination = getter.isAnonymousDestination(request);

    String destination =
        isTemporaryDestination
            ? TEMP_DESTINATION_NAME
            : (isAnonymousDestination
                ? ANONYMOUS_DESTINATION_NAME
                : getter.getDestination(request));
    internalSet(attributes, MESSAGING_DESTINATION_NAME, destination);

    if (isTemporaryDestination) {
      internalSet(attributes, MESSAGING_DESTINATION_TEMPORARY, true);
    } else {
      internalSet(
          attributes, MESSAGING_DESTINATION_TEMPLATE, getter.getDestinationTemplate(request));
    }
    if (isAnonymousDestination) {
      internalSet(attributes, MESSAGING_DESTINATION_ANONYMOUS, true);
    }

    internalSet(
        attributes, MESSAGING_DESTINATION_PARTITION_ID, getter.getDestinationPartitionId(request));

    internalSet(attributes, MESSAGING_MESSAGE_CONVERSATION_ID, getter.getConversationId(request));
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {
    internalSet(attributes, MESSAGING_MESSAGE_ID, getter.getMessageId(request, response));
    internalSet(
        attributes, MESSAGING_BATCH_MESSAGE_COUNT, getter.getBatchMessageCount(request, response));

    internalSet(attributes, MESSAGING_MESSAGE_BODY_SIZE, getter.getMessageBodySize(request));
    internalSet(
        attributes, MESSAGING_MESSAGE_ENVELOPE_SIZE, getter.getMessageEnvelopeSize(request));

    for (String name : capturedHeaders) {
      List<String> values = getter.getMessageHeader(request, name);
      if (!values.isEmpty()) {
        internalSet(attributes, CapturedMessageHeadersUtil.attributeKey(name), values);
      }
    }
  }

  /**
   * This method is internal and is hence not for public use. Its API is unstable and can change at
   * any time.
   */
  @Override
  @SuppressWarnings("deprecation") // using deprecated semconv
  public SpanKey internalGetSpanKey() {
    if (operation == null) {
      return null;
    }

    switch (operation) {
      case CREATE:
        return SpanKey.PRODUCER;
      case SEND:
        return SpanKey.PRODUCER;
      case PUBLISH:
        return SpanKey.PRODUCER;
      case RECEIVE:
        return SpanKey.CONSUMER_RECEIVE;
      case PROCESS:
        return SpanKey.CONSUMER_PROCESS;
      case SETTLE:
        return SpanKey.CONSUMER_SETTLE;
    }
    throw new IllegalStateException("Can't possibly happen");
  }
}
