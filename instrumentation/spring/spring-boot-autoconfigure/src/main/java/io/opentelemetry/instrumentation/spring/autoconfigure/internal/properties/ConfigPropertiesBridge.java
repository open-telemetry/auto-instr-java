/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.internal.properties;

import io.opentelemetry.instrumentation.api.incubator.config.internal.InstrumentationConfig;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Support for {@link ConfigProperties} in {@link InstrumentationConfig}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class ConfigPropertiesBridge implements InstrumentationConfig {

  private final ConfigProperties configProperties;

  public ConfigPropertiesBridge(ConfigProperties configProperties) {
    this.configProperties = configProperties;
  }

  @Nullable
  @Override
  public String getString(String name) {
    try {
      return configProperties.getString(name);
    } catch (ConfigurationException ignored) {
      return null;
    }
  }

  @Override
  public String getString(String name, String defaultValue) {
    try {
      return configProperties.getString(name, defaultValue);
    } catch (ConfigurationException ignored) {
      return defaultValue;
    }
  }

  @Override
  public boolean getBoolean(String name, boolean defaultValue) {
    try {
      return configProperties.getBoolean(name, defaultValue);
    } catch (ConfigurationException ignored) {
      return defaultValue;
    }
  }

  @Override
  public int getInt(String name, int defaultValue) {
    try {
      return configProperties.getInt(name, defaultValue);
    } catch (ConfigurationException ignored) {
      return defaultValue;
    }
  }

  @Override
  public long getLong(String name, long defaultValue) {
    try {
      return configProperties.getLong(name, defaultValue);
    } catch (ConfigurationException ignored) {
      return defaultValue;
    }
  }

  @Override
  public double getDouble(String name, double defaultValue) {
    try {
      return configProperties.getDouble(name, defaultValue);
    } catch (ConfigurationException ignored) {
      return defaultValue;
    }
  }

  @Override
  public Duration getDuration(String name, Duration defaultValue) {
    try {
      return configProperties.getDuration(name, defaultValue);
    } catch (ConfigurationException ignored) {
      return defaultValue;
    }
  }

  @Override
  public List<String> getList(String name, List<String> defaultValue) {
    try {
      return configProperties.getList(name, defaultValue);
    } catch (ConfigurationException ignored) {
      return defaultValue;
    }
  }

  @Override
  public Map<String, String> getMap(String name, Map<String, String> defaultValue) {
    try {
      return configProperties.getMap(name, defaultValue);
    } catch (ConfigurationException ignored) {
      return defaultValue;
    }
  }
}
