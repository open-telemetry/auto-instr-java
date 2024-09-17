/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.v4_0.redis;

import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.RedisCommandSanitizer;
import io.opentelemetry.javaagent.bootstrap.internal.AgentCommonConfig;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import javax.annotation.Nullable;

public enum VertxRedisClientAttributesGetter
    implements DbClientAttributesGetter<VertxRedisClientRequest> {
  INSTANCE;

  private static final RedisCommandSanitizer sanitizer =
      RedisCommandSanitizer.create(AgentCommonConfig.get().isStatementSanitizationEnabled());

  @Override
  public String getSystem(VertxRedisClientRequest request) {
    return DbIncubatingAttributes.DbSystemValues.REDIS;
  }

  @Deprecated
  @Override
  @Nullable
  public String getUser(VertxRedisClientRequest request) {
    return request.getUser();
  }

  @Deprecated
  @Override
  @Nullable
  public String getName(VertxRedisClientRequest request) {
    return null;
  }

  @Nullable
  @Override
  public String getDbNamespace(VertxRedisClientRequest request) {
    return null;
  }

  @Deprecated
  @Override
  @Nullable
  public String getConnectionString(VertxRedisClientRequest request) {
    return request.getConnectionString();
  }

  @Deprecated
  @Override
  public String getStatement(VertxRedisClientRequest request) {
    return sanitizer.sanitize(request.getCommand(), request.getArgs());
  }

  @Nullable
  @Override
  public String getDbQueryText(VertxRedisClientRequest request) {
    return sanitizer.sanitize(request.getCommand(), request.getArgs());
  }

  @Deprecated
  @Nullable
  @Override
  public String getOperation(VertxRedisClientRequest request) {
    return request.getCommand();
  }

  @Nullable
  @Override
  public String getDbOperationName(VertxRedisClientRequest request) {
    return request.getCommand();
  }
}
