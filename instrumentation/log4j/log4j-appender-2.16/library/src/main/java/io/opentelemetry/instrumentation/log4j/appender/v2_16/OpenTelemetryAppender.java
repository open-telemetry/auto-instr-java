/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.log4j.appender.v2_16;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.appender.GlobalLogEmitterProvider;
import io.opentelemetry.instrumentation.api.appender.LogBuilder;
import io.opentelemetry.instrumentation.log4j.appender.v2_16.internal.LogEventMapper;
import java.io.Serializable;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

@Plugin(
    name = OpenTelemetryAppender.PLUGIN_NAME,
    category = Core.CATEGORY_NAME,
    elementType = Appender.ELEMENT_TYPE)
public class OpenTelemetryAppender extends AbstractAppender {

  static final String PLUGIN_NAME = "OpenTelemetry";

  @PluginBuilderFactory
  public static <B extends Builder<B>> B builder() {
    return new Builder<B>().asBuilder();
  }

  static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
      implements org.apache.logging.log4j.core.util.Builder<OpenTelemetryAppender> {

    @Override
    public OpenTelemetryAppender build() {
      return new OpenTelemetryAppender(
          getName(), getLayout(), getFilter(), isIgnoreExceptions(), getPropertyArray());
    }
  }

  private OpenTelemetryAppender(
      String name,
      Layout<? extends Serializable> layout,
      Filter filter,
      boolean ignoreExceptions,
      Property[] properties) {
    super(name, filter, layout, ignoreExceptions, properties);
  }

  @Override
  public void append(LogEvent event) {
    LogBuilder builder =
        GlobalLogEmitterProvider.get()
            .logEmitterBuilder(event.getLoggerName())
            .build()
            .logBuilder();
    ReadOnlyStringMap contextData = event.getContextData();
    LogEventMapper.mapLogEvent(
        builder,
        event.getMessage(),
        event.getLevel(),
        event.getThrown(),
        event.getInstant(),
        contextData,
        contextData.isEmpty(),
        ContextDataMapper.INSTANCE);
    builder.emit();
  }

  private enum ContextDataMapper implements BiConsumer<AttributesBuilder, ReadOnlyStringMap> {
    INSTANCE;

    @Override
    public void accept(AttributesBuilder attributesBuilder, ReadOnlyStringMap contextData) {
      contextData.forEach(
          (key, value) ->
              attributesBuilder.put(LogEventMapper.getMdcAttributeKey(key), String.valueOf(value)));
    }
  }
}
