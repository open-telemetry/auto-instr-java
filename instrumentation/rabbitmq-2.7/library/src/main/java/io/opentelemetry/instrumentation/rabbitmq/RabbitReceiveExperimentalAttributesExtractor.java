/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import static io.opentelemetry.instrumentation.rabbitmq.RabbitTelemetry.RABBITMQ_COMMAND;

import com.rabbitmq.client.GetResponse;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import javax.annotation.Nullable;

class RabbitReceiveExperimentalAttributesExtractor
    implements AttributesExtractor<ReceiveRequest, GetResponse> {
  private static final AttributeKey<String> RABBITMQ_QUEUE =
      AttributeKey.stringKey("rabbitmq.queue");

  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, ReceiveRequest receiveRequest) {
    attributes.put(RABBITMQ_COMMAND, "basic.get");
    attributes.put(RABBITMQ_QUEUE, receiveRequest.getQueue());
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      ReceiveRequest receiveRequest,
      @Nullable GetResponse response,
      @Nullable Throwable error) {}
}
