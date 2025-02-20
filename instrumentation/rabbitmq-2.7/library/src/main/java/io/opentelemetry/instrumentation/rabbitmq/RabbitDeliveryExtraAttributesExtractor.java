/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import com.rabbitmq.client.Envelope;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.SemanticAttributes;
import javax.annotation.Nullable;

class RabbitDeliveryExtraAttributesExtractor implements AttributesExtractor<DeliveryRequest, Void> {

  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, DeliveryRequest request) {
    Envelope envelope = request.getEnvelope();
    String routingKey = envelope.getRoutingKey();
    if (routingKey != null && !routingKey.isEmpty()) {
      attributes.put(SemanticAttributes.MESSAGING_RABBITMQ_DESTINATION_ROUTING_KEY, routingKey);
    }
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      DeliveryRequest request,
      @Nullable Void unused,
      @Nullable Throwable error) {}
}
