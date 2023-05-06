/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.tooling.config;

import static java.util.Collections.emptyMap;
import static java.util.logging.Level.SEVERE;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.internal.ConfigPropertiesUtil;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@AutoService(AutoConfigurationCustomizerProvider.class)
public final class ConfigurationFileLoader implements AutoConfigurationCustomizerProvider {

  private static final Logger logger = Logger.getLogger(ConfigurationFileLoader.class.getName());

  static final String CONFIGURATION_FILE_PROPERTY = "otel.javaagent.configuration-file";

  private static Map<String, String> configFileContents;

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addPropertiesSupplier(ConfigurationFileLoader::getConfigFileContents);
  }

  static Map<String, String> getConfigFileContents() {
    if (configFileContents == null) {
      configFileContents = loadConfigFile();
    }
    return configFileContents;
  }

  // visible for tests
  static Map<String, String> loadConfigFile() {
    // Reading from system property first and from env after
    String configurationFilePath = ConfigPropertiesUtil.getString(CONFIGURATION_FILE_PROPERTY);
    if (configurationFilePath == null) {
      return emptyMap();
    }

    // Normalizing tilde (~) paths for unix systems
    configurationFilePath =
        configurationFilePath.replaceFirst("^~", System.getProperty("user.home"));

    // Configuration properties file is optional
    File configurationFile = new File(configurationFilePath);
    if (!configurationFile.exists()) {
      logger.log(SEVERE, "Configuration file \"{0}\" not found.", configurationFilePath);
      return emptyMap();
    }

    Properties properties = new Properties();
    try (InputStreamReader reader =
        new InputStreamReader(new FileInputStream(configurationFile), StandardCharsets.UTF_8)) {
      properties.load(reader);
    } catch (FileNotFoundException fnf) {
      logger.log(SEVERE, "Configuration file \"{0}\" not found.", configurationFilePath);
    } catch (IOException ioe) {
      logger.log(
          SEVERE,
          "Configuration file \"{0}\" cannot be accessed or correctly parsed.",
          configurationFilePath);
    }

    SubstitutionReplacer substitutorReplacer = new SubstitutionReplacer(configurationFilePath);

    return properties.entrySet().stream()
        .collect(
            Collectors.toMap(
                e -> e.getKey().toString(),
                e -> substitutorReplacer.replace(e.getValue().toString())));
  }

  @Override
  public int order() {
    // make sure it runs after all the user-provided customizers
    return Integer.MAX_VALUE;
  }

  static class SubstitutionReplacer {
    private final String configurationFilePath;
    private final Function<String, String> replacer;

    SubstitutionReplacer(String configurationFilePath) {
      this.configurationFilePath = configurationFilePath;
      this.replacer = s -> s != null && !"".equals(s) ? ConfigPropertiesUtil.getString(s) : null;
    }

    String replace(String input) {
      StringBuilder outputBuilder = new StringBuilder();

      for (int i = 0; i < input.length(); i++) {
        if (Character.valueOf(input.charAt(i)).equals('$') // Escaped $
            && input.length() > i + 1
            && Character.valueOf(input.charAt(i + 1)).equals('$')) {
          i++;
          outputBuilder.append('$');
        } else if (Character.valueOf(input.charAt(i)).equals('$') // placeholder start
            && input.length() > i + 1
            && Character.valueOf(input.charAt(i + 1)).equals('{')) {

          StringBuilder placeholderBuilder = new StringBuilder();
          for (i = i + 2;
              i < input.length() && !Character.valueOf(input.charAt(i)).equals('}');
              i++) {
            placeholderBuilder.append(input.charAt(i));
          }
          // finished before end of input string => properly formatted placeholder
          if (i < input.length()) {
            String placeholder = placeholderBuilder.toString();
            String substitutionValue = replacer.apply(placeholder);
            if (substitutionValue != null) {
              outputBuilder.append(substitutionValue);
            } else {
              logger.log(
                  SEVERE,
                  "Configuration file \"{0}\" cannot be fully parsed. No value found to substitute for placeholder \"$'{'{1}'}'\".",
                  new Object[] {configurationFilePath, placeholder});
              outputBuilder.append("${").append(placeholder).append("}");
            }
          } else { // end of input string before } found => not properly formatted placeholder
            outputBuilder.append("${");
            outputBuilder.append(placeholderBuilder);
          }
        } else {
          outputBuilder.append(input.charAt(i));
        }
      }
      return outputBuilder.toString();
    }
  }
}
