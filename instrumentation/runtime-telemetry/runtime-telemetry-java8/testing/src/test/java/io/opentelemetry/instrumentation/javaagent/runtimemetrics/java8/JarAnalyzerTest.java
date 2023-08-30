/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.javaagent.runtimemetrics.java8;

import static io.opentelemetry.instrumentation.javaagent.runtimemetrics.java8.JarAnalyzerUtil.PACKAGE_CHECKSUM;
import static io.opentelemetry.instrumentation.javaagent.runtimemetrics.java8.JarAnalyzerUtil.PACKAGE_DESCRIPTION;
import static io.opentelemetry.instrumentation.javaagent.runtimemetrics.java8.JarAnalyzerUtil.PACKAGE_NAME;
import static io.opentelemetry.instrumentation.javaagent.runtimemetrics.java8.JarAnalyzerUtil.PACKAGE_PATH;
import static io.opentelemetry.instrumentation.javaagent.runtimemetrics.java8.JarAnalyzerUtil.PACKAGE_TYPE;
import static io.opentelemetry.instrumentation.javaagent.runtimemetrics.java8.JarAnalyzerUtil.PACKAGE_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.events.EventEmitter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.assertj.AttributesAssert;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpRequest;

class JarAnalyzerTest {

  @ParameterizedTest
  @MethodSource("processUrlArguments")
  void processUrl_EmitsEvents(URL archiveUrl, Consumer<AttributesAssert> attributesConsumer) {
    EventEmitter eventEmitter = mock(EventEmitter.class);
    JarAnalyzer.processUrl(eventEmitter, archiveUrl);

    ArgumentCaptor<Attributes> attributesArgumentCaptor = ArgumentCaptor.forClass(Attributes.class);
    verify(eventEmitter).emit(eq("info"), attributesArgumentCaptor.capture());

    attributesConsumer.accept(
        OpenTelemetryAssertions.assertThat(attributesArgumentCaptor.getValue()));
  }

  private static Stream<Arguments> processUrlArguments() {
    return Stream.of(
        // instrumentation code
        Arguments.of(
            archiveUrl(JarAnalyzer.class),
            assertAttributes(
                attributes ->
                    attributes
                        .containsEntry(PACKAGE_TYPE, "jar")
                        .containsEntry(
                            PACKAGE_PATH,
                            "opentelemetry-javaagent-runtime-telemetry-java8-1.30.0-alpha-SNAPSHOT.jar")
                        .containsEntry(PACKAGE_DESCRIPTION, "javaagent by OpenTelemetry")
                        .hasEntrySatisfying(
                            PACKAGE_CHECKSUM, checksum -> assertThat(checksum).isNotEmpty()))),
        // dummy war
        Arguments.of(
            archiveUrl(new File(System.getenv("DUMMY_APP_WAR"))),
            assertAttributes(
                attributes ->
                    attributes
                        .containsEntry(PACKAGE_TYPE, "war")
                        .containsEntry(PACKAGE_PATH, "app.war")
                        .containsEntry(PACKAGE_DESCRIPTION, "Dummy App by OpenTelemetry")
                        .hasEntrySatisfying(
                            PACKAGE_CHECKSUM, checksum -> assertThat(checksum).isNotEmpty()))),
        // io.opentelemetry:opentelemetry-api
        Arguments.of(
            archiveUrl(Tracer.class),
            assertAttributes(
                attributes ->
                    attributes
                        .containsEntry(PACKAGE_TYPE, "jar")
                        //
                        .containsEntry(PACKAGE_PATH, "opentelemetry-api-1.29.0.jar")
                        .containsEntry(PACKAGE_DESCRIPTION, "all")
                        .hasEntrySatisfying(
                            PACKAGE_CHECKSUM, checksum -> assertThat(checksum).isNotEmpty()))),
        // org.springframework:spring-webmvc
        Arguments.of(
            archiveUrl(HttpRequest.class),
            assertAttributes(
                attributes ->
                    attributes
                        .containsEntry(PACKAGE_TYPE, "jar")
                        // TODO(jack-berg): can we extract version out of path to populate
                        // package.version field?
                        .containsEntry(PACKAGE_PATH, "spring-web-3.1.0.RELEASE.jar")
                        .containsEntry(PACKAGE_DESCRIPTION, "org.springframework.web")
                        .hasEntrySatisfying(
                            PACKAGE_CHECKSUM, checksum -> assertThat(checksum).isNotEmpty()))),
        // com.google.guava:guava
        Arguments.of(
            archiveUrl(ImmutableMap.class),
            assertAttributes(
                attributes ->
                    attributes
                        .containsEntry(PACKAGE_TYPE, "jar")
                        .containsEntry(PACKAGE_PATH, "guava-32.1.2-jre.jar")
                        .containsEntry(PACKAGE_NAME, "com.google.guava:guava")
                        .containsEntry(PACKAGE_VERSION, "32.1.2-jre")
                        .hasEntrySatisfying(
                            PACKAGE_CHECKSUM, checksum -> assertThat(checksum).isNotEmpty()))));
  }

  private static URL archiveUrl(File file) {
    try {
      return new URL("file://" + file.getAbsolutePath());
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Error creating URL for file", e);
    }
  }

  private static URL archiveUrl(Class<?> clazz) {
    return clazz.getProtectionDomain().getCodeSource().getLocation();
  }

  private static Consumer<AttributesAssert> assertAttributes(
      Consumer<AttributesAssert> attributesAssert) {
    return attributesAssert;
  }
}
