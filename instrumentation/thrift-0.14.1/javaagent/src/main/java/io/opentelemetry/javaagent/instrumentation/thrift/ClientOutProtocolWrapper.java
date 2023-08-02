/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import static io.opentelemetry.javaagent.instrumentation.thrift.ThriftSingletons.clientInstrumenter;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import javax.annotation.Nullable;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;

@SuppressWarnings({"serial"})
public final class ClientOutProtocolWrapper extends AbstractProtocolWrapper {
  public ThriftRequest request;
  private boolean injected = true;
  @Nullable public Scope scope;
  @Nullable public Context context;

  public ClientOutProtocolWrapper(TProtocol protocol) {
    super(protocol);
    request = new ThriftRequest(protocol);
  }

  @Override
  public final void writeMessageBegin(TMessage message) throws TException {
    this.request.methodName = message.name;
    super.writeMessageBegin(message);
    injected = false;
  }

  @Override
  public final void writeFieldStop() throws TException {
    if (!injected) {
      Context context = Context.current();
      if (!clientInstrumenter().shouldStart(context, request)) {
        return;
      }
      try {
        context = clientInstrumenter().start(context, request);
        this.context = context;
        GlobalOpenTelemetry.get()
            .getPropagators()
            .getTextMapPropagator()
            .inject(context, request, ThriftHeaderSetter.INSTANCE);
        request.writeAttachment();
      } catch (Throwable e) {
        clientInstrumenter().end(context, request, 0, e);
      } finally {
        injected = true;
      }
    }
    super.writeFieldStop();
  }
}
