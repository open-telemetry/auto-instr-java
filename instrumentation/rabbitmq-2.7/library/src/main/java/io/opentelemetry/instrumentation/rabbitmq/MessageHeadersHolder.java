/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

public class MessageHeadersHolder {
  private InstrumentedChannel instrumentedChannel;

  public InstrumentedChannel getInstrumentedChannel() {
    return instrumentedChannel;
  }

  public void setInstrumentedChannel(InstrumentedChannel instrumentedChannel) {
    this.instrumentedChannel = instrumentedChannel;
  }
}
