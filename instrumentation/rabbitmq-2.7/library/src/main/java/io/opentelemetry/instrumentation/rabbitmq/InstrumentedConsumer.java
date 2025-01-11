/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.io.IOException;

public class InstrumentedConsumer implements Consumer {
  private final Consumer delegate;
  private final RabbitTelemetry rabbitTelemetry;

  public InstrumentedConsumer(Consumer delegate, RabbitTelemetry rabbitTelemetry) {
    this.delegate = delegate;
    this.rabbitTelemetry = rabbitTelemetry;
  }

  @Override
  public void handleConsumeOk(String consumerTag) {
    delegate.handleConsumeOk(consumerTag);
  }

  @Override
  public void handleCancelOk(String consumerTag) {
    delegate.handleCancelOk(consumerTag);
  }

  @Override
  public void handleCancel(String consumerTag) throws IOException {
    delegate.handleCancel(consumerTag);
  }

  @Override
  public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
    delegate.handleShutdownSignal(consumerTag, sig);
  }

  @Override
  public void handleRecoverOk(String consumerTag) {
    delegate.handleRecoverOk(consumerTag);
  }

  @Override
  public void handleDelivery(
      String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
      throws IOException {
    Context parentContext = Context.current();
    DeliveryRequest request = null;

    if (!rabbitTelemetry.getDeliverInstrumenter().shouldStart(parentContext, request)) {
      delegate.handleDelivery(consumerTag, envelope, properties, body);
      return;
    }

    Context context = rabbitTelemetry.getDeliverInstrumenter().start(parentContext, request);
    try (Scope ignored = context.makeCurrent()) {
      // Call delegate.
      delegate.handleDelivery(consumerTag, envelope, properties, body);
    } catch (Throwable throwable) {
      rabbitTelemetry.getDeliverInstrumenter().end(context, request, null, throwable);
      throw throwable;
    } finally {
      rabbitTelemetry.getDeliverInstrumenter().end(context, request, null, null);
    }
  }
}
