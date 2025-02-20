/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.rediscala;

import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesGetter;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import java.util.Locale;
import javax.annotation.Nullable;
import redis.RedisCommand;

final class RediscalaAttributesGetter
    implements DbClientAttributesGetter<RedisCommand<?, ?>, Void> {

  @SuppressWarnings("deprecation") // using deprecated DbSystemIncubatingValues
  @Override
  public String getDbSystem(RedisCommand<?, ?> redisCommand) {
    return DbIncubatingAttributes.DbSystemIncubatingValues.REDIS;
  }

  @Deprecated
  @Override
  @Nullable
  public String getUser(RedisCommand<?, ?> redisCommand) {
    return null;
  }

  @Override
  @Nullable
  public String getDbNamespace(RedisCommand<?, ?> redisCommand) {
    return null;
  }

  @Deprecated
  @Override
  @Nullable
  public String getConnectionString(RedisCommand<?, ?> redisCommand) {
    return null;
  }

  @Override
  @Nullable
  public String getDbQueryText(RedisCommand<?, ?> redisCommand) {
    return null;
  }

  @Override
  public String getDbOperationName(RedisCommand<?, ?> redisCommand) {
    return redisCommand.getClass().getSimpleName().toUpperCase(Locale.ROOT);
  }
}
