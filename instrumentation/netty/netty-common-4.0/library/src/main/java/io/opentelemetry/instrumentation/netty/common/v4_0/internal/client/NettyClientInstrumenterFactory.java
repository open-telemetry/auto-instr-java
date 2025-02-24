/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesExtractor;
import io.opentelemetry.instrumentation.netty.common.internal.NettyConnectionRequest;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class NettyClientInstrumenterFactory {

  private final DefaultHttpClientInstrumenterBuilder<HttpRequestAndChannel, HttpResponse> builder;
  private final NettyConnectionInstrumentationFlag connectionTelemetryState;
  private final NettyConnectionInstrumentationFlag sslTelemetryState;

  public NettyClientInstrumenterFactory(
      DefaultHttpClientInstrumenterBuilder<HttpRequestAndChannel, HttpResponse> builder,
      NettyConnectionInstrumentationFlag connectionTelemetryState,
      NettyConnectionInstrumentationFlag sslTelemetryState) {
    this.builder = builder;
    this.connectionTelemetryState = connectionTelemetryState;
    this.sslTelemetryState = sslTelemetryState;
  }

  public Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter() {
    return builder.build();
  }

  public NettyConnectionInstrumenter createConnectionInstrumenter() {
    if (connectionTelemetryState == NettyConnectionInstrumentationFlag.DISABLED) {
      return NoopConnectionInstrumenter.INSTANCE;
    }

    boolean connectionTelemetryFullyEnabled =
        connectionTelemetryState == NettyConnectionInstrumentationFlag.ENABLED;
    NettyConnectHttpAttributesGetter getter = NettyConnectHttpAttributesGetter.INSTANCE;

    InstrumenterBuilder<NettyConnectionRequest, Channel> builder =
        this.builder.instrumenterBuilder(NettyConnectionRequest::spanName);

    if (connectionTelemetryFullyEnabled) {
      // when the connection telemetry is fully enabled, CONNECT spans are created for every
      // request; and semantically they're not HTTP spans, they must not use the HTTP client
      // extractor
      builder.addAttributesExtractor(NetworkAttributesExtractor.create(getter));
      builder.addAttributesExtractor(ServerAttributesExtractor.create(getter));
    } else {
      // in case the connection telemetry is emitted only on errors, the CONNECT span is a stand-in
      // for the HTTP client span
      builder.addAttributesExtractor(HttpClientAttributesExtractor.create(getter));
    }

    Instrumenter<NettyConnectionRequest, Channel> instrumenter =
        builder.buildInstrumenter(
            connectionTelemetryFullyEnabled
                ? SpanKindExtractor.alwaysInternal()
                : SpanKindExtractor.alwaysClient());

    return connectionTelemetryFullyEnabled
        ? new NettyConnectionInstrumenterImpl(instrumenter)
        : new NettyErrorOnlyConnectionInstrumenter(instrumenter);
  }

  public NettySslInstrumenter createSslInstrumenter() {
    if (sslTelemetryState == NettyConnectionInstrumentationFlag.DISABLED) {
      return NoopSslInstrumenter.INSTANCE;
    }

    boolean sslTelemetryFullyEnabled =
        sslTelemetryState == NettyConnectionInstrumentationFlag.ENABLED;
    NettySslNetAttributesGetter netAttributesGetter = new NettySslNetAttributesGetter();
    Instrumenter<NettySslRequest, Void> instrumenter =
        builder
            .<NettySslRequest, Void>instrumenterBuilder(NettySslRequest::spanName)
            .addAttributesExtractor(NetworkAttributesExtractor.create(netAttributesGetter))
            .buildInstrumenter(
                sslTelemetryFullyEnabled
                    ? SpanKindExtractor.alwaysInternal()
                    : SpanKindExtractor.alwaysClient());

    return sslTelemetryFullyEnabled
        ? new NettySslInstrumenterImpl(instrumenter)
        : new NettySslErrorOnlyInstrumenter(instrumenter);
  }
}
