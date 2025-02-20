/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.tooling.config;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.javaagent.tooling.OpenTelemetryInstaller;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;

@ClearSystemProperty(key = ConfigurationFile.CONFIGURATION_FILE_PROPERTY)
class ConfigurationPropertiesSupplierTest {

  @BeforeAll
  @AfterAll
  static void cleanUp() {
    GlobalOpenTelemetry.resetForTest();
    ConfigurationFile.resetForTest();
  }

  // regression for https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/6696
  @SetSystemProperty(key = "otel.sdk.disabled", value = "true") // don't setup the SDK
  @Test
  void fileConfigOverwritesUserPropertiesSupplier(@TempDir Path tempDir) throws IOException {
    // given
    Path configFile = tempDir.resolve("test-config.properties");
    Files.write(configFile, singleton("custom.key = 42"));
    System.setProperty(ConfigurationFile.CONFIGURATION_FILE_PROPERTY, configFile.toString());

    // when
    AutoConfiguredOpenTelemetrySdk autoConfiguredSdk =
        OpenTelemetryInstaller.installOpenTelemetrySdk(this.getClass().getClassLoader());

    // then
    assertThat(AutoConfigureUtil.getConfig(autoConfiguredSdk).getString("custom.key"))
        .isEqualTo("42");
  }

  // SPI used in test
  public static class UserCustomPropertiesSupplier implements AutoConfigurationCustomizerProvider {

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
      autoConfiguration.addPropertiesSupplier(() -> singletonMap("custom.key", "123"));
    }
  }
}
