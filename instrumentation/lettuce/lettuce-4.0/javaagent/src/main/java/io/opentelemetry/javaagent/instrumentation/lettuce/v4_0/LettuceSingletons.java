/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.lettuce.v4_0;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.protocol.RedisCommand;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientSpanNameExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesExtractor;
import io.opentelemetry.javaagent.bootstrap.internal.CommonConfig;

public final class LettuceSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.lettuce-4.0";

  private static final Instrumenter<RedisCommand<?, ?, ?>, Void> INSTRUMENTER;
  private static final Instrumenter<RedisURI, Void> CONNECT_INSTRUMENTER;

  public static final ContextKey<Context> COMMAND_CONTEXT_KEY =
      ContextKey.named("opentelemetry-lettuce-v4_0-context-key");

  static {
    LettuceDbAttributeGetter dbAttributeGetter = new LettuceDbAttributeGetter();

    INSTRUMENTER =
        Instrumenter.<RedisCommand<?, ?, ?>, Void>builder(
                GlobalOpenTelemetry.get(),
                INSTRUMENTATION_NAME,
                DbClientSpanNameExtractor.create(dbAttributeGetter))
            .addAttributesExtractor(DbClientAttributesExtractor.create(dbAttributeGetter))
            .buildInstrumenter(SpanKindExtractor.alwaysClient());

    LettuceConnectNetworkAttributeGetter netAttributeGetter =
        new LettuceConnectNetworkAttributeGetter();

    CONNECT_INSTRUMENTER =
        Instrumenter.<RedisURI, Void>builder(
                GlobalOpenTelemetry.get(), INSTRUMENTATION_NAME, redisUri -> "CONNECT")
            .addAttributesExtractor(ServerAttributesExtractor.create(netAttributeGetter))
            .addAttributesExtractor(
                PeerServiceAttributesExtractor.create(
                    netAttributeGetter, CommonConfig.get().getPeerServiceResolver()))
            .addAttributesExtractor(new LettuceConnectAttributesExtractor())
            .buildInstrumenter(SpanKindExtractor.alwaysClient());
  }

  public static Instrumenter<RedisCommand<?, ?, ?>, Void> instrumenter() {
    return INSTRUMENTER;
  }

  public static Instrumenter<RedisURI, Void> connectInstrumenter() {
    return CONNECT_INSTRUMENTER;
  }

  private LettuceSingletons() {}
}
