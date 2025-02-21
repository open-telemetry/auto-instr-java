/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

class DeliveryRequest {
  private final String queue;
  private final Envelope envelope;
  private final AMQP.BasicProperties properties;
  private final byte[] body;

  public DeliveryRequest(
      String queue, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
    this.queue = queue;
    this.envelope = envelope;
    this.properties = properties;
    this.body = body;
  }

  public String getQueue() {
    return this.queue;
  }

  public Envelope getEnvelope() {
    return this.envelope;
  }

  public AMQP.BasicProperties getProperties() {
    return this.properties;
  }

  public byte[] getBody() {
    return this.body;
  }

  public String spanName() {
    String queue = getQueue();
    if (queue == null || queue.isEmpty()) {
      return "<default> process";
    } else if (queue.startsWith("amq.gen-")) {
      return "<generated> process";
    } else {
      return queue + " process";
    }
  }
}
