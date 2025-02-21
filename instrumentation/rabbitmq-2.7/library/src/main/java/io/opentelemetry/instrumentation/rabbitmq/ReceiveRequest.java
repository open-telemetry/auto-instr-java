/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.GetResponse;
import javax.annotation.Nullable;

class ReceiveRequest {

  private final String queue;
  private final GetResponse response;
  private final Connection connection;

  public ReceiveRequest(String queue, GetResponse response, Connection connection) {
    this.queue = queue;
    this.response = response;
    this.connection = connection;
  }

  public String getQueue() {
    return this.queue;
  }

  @Nullable
  public GetResponse getResponse() {
    return this.response;
  }

  public Connection getConnection() {
    return this.connection;
  }

  String spanName() {
    String queue = getQueue();
    return (queue.startsWith("amq.gen-") ? "<generated>" : queue) + " receive";
  }
}
