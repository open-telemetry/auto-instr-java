/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4_1.internal.server;

import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpServerInstrumenterBuilder;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.v4_1.NettyServerTelemetryBuilder;
import java.util.function.Function;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class NettyServerInstrumenterBuilderUtil {

  private static Function<
          NettyServerTelemetryBuilder,
          DefaultHttpServerInstrumenterBuilder<HttpRequestAndChannel, HttpResponse>>
      builderExtractor;

  private NettyServerInstrumenterBuilderUtil() {}

  public static void setBuilderExtractor(
      Function<
              NettyServerTelemetryBuilder,
              DefaultHttpServerInstrumenterBuilder<HttpRequestAndChannel, HttpResponse>>
          builderExtractor) {
    NettyServerInstrumenterBuilderUtil.builderExtractor = builderExtractor;
  }

  public static Function<
          NettyServerTelemetryBuilder,
          DefaultHttpServerInstrumenterBuilder<HttpRequestAndChannel, HttpResponse>>
      getBuilderExtractor() {
    return builderExtractor;
  }
}
