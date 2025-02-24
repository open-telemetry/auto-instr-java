/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4_1.internal.server;

import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.v4_1.internal.ProtocolEventHandler;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class HttpServerTracingHandler
    extends CombinedChannelDuplexHandler<
        HttpServerRequestTracingHandler, HttpServerResponseTracingHandler> {

  public HttpServerTracingHandler(
      Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter,
      HttpServerResponseBeforeCommitHandler responseBeforeCommitHandler,
      ProtocolEventHandler protocolEventHandler) {
    super(
        new HttpServerRequestTracingHandler(instrumenter),
        new HttpServerResponseTracingHandler(
            instrumenter, responseBeforeCommitHandler, protocolEventHandler));
  }
}
