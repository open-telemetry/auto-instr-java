/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import javax.annotation.Nullable;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.ServerContext;
import org.apache.thrift.server.TServerEventHandler;
import org.apache.thrift.transport.TTransport;

public final class ThriftServerEventHandler implements TServerEventHandler {
  public TServerEventHandler innerEventHandler;

  public ThriftServerEventHandler(TServerEventHandler inner) {
    if (inner instanceof ThriftServerEventHandler) {
      innerEventHandler = ((ThriftServerEventHandler) inner).innerEventHandler;
    } else {
      innerEventHandler = inner;
    }
  }

  @Override
  public void preServe() {
    if (innerEventHandler != null) {
      innerEventHandler.preServe();
    }
  }

  @Override
  @Nullable
  public ServerContext createContext(TProtocol protocol, TProtocol protocol1) {
    if (innerEventHandler != null) {
      return innerEventHandler.createContext(protocol, protocol1);
    }
    return null;
  }

  /** 删除Context的时候，触发 在server启动后，只会执行一次 */
  @Override
  public void deleteContext(ServerContext serverContext, TProtocol input, TProtocol output) {
    if (innerEventHandler != null) {
      innerEventHandler.deleteContext(serverContext, input, output);
    }
  }

  @Override
  public void processContext(
      ServerContext serverContext, TTransport transport, TTransport transport1) {
    if (innerEventHandler != null) {
      innerEventHandler.processContext(serverContext, transport, transport1);
    }
  }
}
