/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jedis.v1_4;

import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.RedisCommandSanitizer;
import io.opentelemetry.javaagent.bootstrap.internal.AgentCommonConfig;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import javax.annotation.Nullable;

final class JedisDbAttributesGetter implements DbClientAttributesGetter<JedisRequest, Void> {

  private static final RedisCommandSanitizer sanitizer =
      RedisCommandSanitizer.create(AgentCommonConfig.get().isStatementSanitizationEnabled());

  @Override
  public String getDbSystem(JedisRequest request) {
    return DbIncubatingAttributes.DbSystemNameIncubatingValues.REDIS;
  }

  @Deprecated
  @Override
  @Nullable
  public String getUser(JedisRequest request) {
    return null;
  }

  @Override
  public String getDbNamespace(JedisRequest request) {
    return null;
  }

  @Deprecated
  @Override
  public String getConnectionString(JedisRequest request) {
    return null;
  }

  @Override
  public String getDbQueryText(JedisRequest request) {
    return sanitizer.sanitize(request.getCommand().name(), request.getArgs());
  }

  @Override
  public String getDbOperationName(JedisRequest request) {
    return request.getCommand().name();
  }
}
