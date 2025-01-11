/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import static io.opentelemetry.instrumentation.rabbitmq.RabbitTelemetry.RABBITMQ_COMMAND;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.Date;
import javax.annotation.Nullable;

class RabbitDeliveryExperimentalAttributesExtractor
    implements AttributesExtractor<DeliveryRequest, Void> {
  private static final AttributeKey<Long> RABBITMQ_QUEUE_TIME =
      AttributeKey.longKey("rabbitmq.record.queue_time_ms");

  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, DeliveryRequest request) {
    Date timestamp = request.getProperties().getTimestamp();
    if (timestamp != null) {
      // this will be set if the sender sets the timestamp,
      // or if a plugin is installed on the rabbitmq broker
      long produceTimeMillis = timestamp.getTime();
      attributes.put(
          RABBITMQ_QUEUE_TIME, Math.max(0L, System.currentTimeMillis() - produceTimeMillis));
    }

    attributes.put(RABBITMQ_COMMAND, "basic.deliver");
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      DeliveryRequest request,
      @Nullable Void unused,
      @Nullable Throwable error) {}
}
