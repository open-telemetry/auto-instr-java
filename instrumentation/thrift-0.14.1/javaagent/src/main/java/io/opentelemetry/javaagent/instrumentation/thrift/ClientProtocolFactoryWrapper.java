/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransport;

@SuppressWarnings({"serial"})
public final class ClientProtocolFactoryWrapper implements TProtocolFactory {
  public TProtocolFactory innerProtocolFactoryWrapper;

  @Override
  public TProtocol getProtocol(TTransport transport) {
    TProtocol protocol = innerProtocolFactoryWrapper.getProtocol(transport);
    if (protocol instanceof ClientOutProtocolWrapper) {
      return protocol;
    }
    return new ClientOutProtocolWrapper(innerProtocolFactoryWrapper.getProtocol(transport));
  }

  public ClientProtocolFactoryWrapper(TProtocolFactory inner) {
    innerProtocolFactoryWrapper = inner;
  }
}
