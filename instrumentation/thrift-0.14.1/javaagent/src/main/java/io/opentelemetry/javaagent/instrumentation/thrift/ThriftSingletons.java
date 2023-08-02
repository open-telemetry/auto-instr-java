/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.rpc.RpcClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.rpc.RpcServerAttributesExtractor;
import io.opentelemetry.javaagent.instrumentation.thrift.internal.ThriftNetServerAttributesGetter;

public final class ThriftSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.thrift-0.14.1";

  private static final Instrumenter<ThriftRequest, Integer> CLIENT_INSTRUMENTER;
  private static final Instrumenter<ThriftRequest, Integer> SERVER_INSTRUMENTER;

  static {
    ThriftNetServerAttributesGetter thriftNetServerAttributesGetter =
        new ThriftNetServerAttributesGetter();
    SpanNameExtractor<ThriftRequest> spanNameExtractor = new ThriftSpanNameExtractor();
    ThriftRpcAttributesGetter thriftRpcAttributesGetter = ThriftRpcAttributesGetter.INSTANCE;
    CLIENT_INSTRUMENTER =
        Instrumenter.<ThriftRequest, Integer>builder(
                GlobalOpenTelemetry.get(), INSTRUMENTATION_NAME, spanNameExtractor)
            .addAttributesExtractor(new ThriftAttributesExtractor())
            .addAttributesExtractor(RpcClientAttributesExtractor.create(thriftRpcAttributesGetter))
            .buildClientInstrumenter(ThriftHeaderSetter.INSTANCE);
    SERVER_INSTRUMENTER =
        Instrumenter.<ThriftRequest, Integer>builder(
                GlobalOpenTelemetry.get(), INSTRUMENTATION_NAME, spanNameExtractor)
            .addAttributesExtractor(RpcServerAttributesExtractor.create(thriftRpcAttributesGetter))
            .addAttributesExtractor(
                NetServerAttributesExtractor.create(thriftNetServerAttributesGetter))
            .buildServerInstrumenter(ThriftHeaderGetter.INSTANCE);
  }

  public static Instrumenter<ThriftRequest, Integer> clientInstrumenter() {
    return CLIENT_INSTRUMENTER;
  }

  public static Instrumenter<ThriftRequest, Integer> serverInstrumenter() {
    return SERVER_INSTRUMENTER;
  }

  private ThriftSingletons() {}
}
