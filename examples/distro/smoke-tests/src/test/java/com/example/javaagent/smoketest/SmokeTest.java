/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.javaagent.smoketest;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

abstract class SmokeTest {
  private static final Logger logger = LoggerFactory.getLogger(SmokeTest.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  // use JDK 8 for local tests
  private static final int SMOKE_TEST_JAVA_VERSION =
      Integer.parseInt(Objects.requireNonNullElse(System.getenv("SMOKE_TEST_JAVA_VERSION"), "8"));

  private static OkHttpClient client = OkHttpUtils.client();

  private static final Network network = Network.newNetwork();
  protected static final String agentPath =
      System.getProperty("io.opentelemetry.smoketest.agent.shadowJar.path");

  protected abstract String getTargetImage(int jdk);

  private static GenericContainer backend;

  @BeforeAll
  static void setupSpec() {
    backend =
        new GenericContainer<>(
                "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-fake-backend:20221127.3559314891")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/health").forPort(8080))
            .withNetwork(network)
            .withNetworkAliases("backend")
            .withLogConsumer(new Slf4jLogConsumer(logger));
    backend.start();
  }

  protected GenericContainer<?> target;

  protected void startTarget() {
    startTarget("");
  }

  protected void startTarget(String extraCliArgs) {
    target =
        new GenericContainer<>(getTargetImage(SMOKE_TEST_JAVA_VERSION))
            .withExposedPorts(8080)
            .withNetwork(network)
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withCopyFileToContainer(
                MountableFile.forHostPath(agentPath), "/opentelemetry-javaagent.jar")
            .withEnv("JAVA_TOOL_OPTIONS", "-javaagent:/opentelemetry-javaagent.jar " + extraCliArgs)
            .withEnv("OTEL_BSP_MAX_EXPORT_BATCH", "1")
            .withEnv("OTEL_BSP_SCHEDULE_DELAY", "10")
            .withEnv("OTEL_METRIC_EXPORT_INTERVAL", "2000")
            .withEnv("OTEL_PROPAGATORS", "tracecontext,baggage")
            .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://backend:8080");
    target.start();
  }

  @AfterEach
  void cleanup() throws IOException {
    if (target != null) {
      target.stop();
      client
          .newCall(
              new Request.Builder()
                  .url(String.format("http://localhost:%d/clear", backend.getMappedPort(8080)))
                  .build())
          .execute()
          .close();
    }
  }

  @AfterAll
  static void cleanupSpec() {
    backend.stop();
  }

  protected static String makeCall(Request request) {
    return await()
        .atMost(10, SECONDS)
        .until(
            () -> {
              try {
                Response response = client.newCall(request).execute();
                if (response.code() != 200) {
                  return null;
                }
                try (ResponseBody body = response.body()) {
                  return body.string();
                }
              } catch (IOException e) {
                return null;
              }
            },
            Objects::nonNull);
  }

  protected static int countResourcesByValue(
      Collection<ExportTraceServiceRequest> traces, String resourceName, String value) {
    return (int)
        traces.stream()
            .flatMap(it -> it.getResourceSpansList().stream())
            .flatMap(it -> it.getResource().getAttributesList().stream())
            .filter(
                kv ->
                    kv.getKey().equals(resourceName)
                        && kv.getValue().getStringValue().equals(value))
            .count();
  }

  protected static int countSpansByName(
      Collection<ExportTraceServiceRequest> traces, String spanName) {
    return (int) getSpanStream(traces).filter(it -> it.getName().equals(spanName)).count();
  }

  protected static int countSpansByAttributeValue(
      Collection<ExportTraceServiceRequest> traces, String attributeName, String attributeValue) {
    return (int)
        getSpanStream(traces)
            .flatMap(it -> it.getAttributesList().stream())
            .filter(
                kv ->
                    kv.getKey().equals(attributeName)
                        && kv.getValue().getStringValue().equals(attributeValue))
            .count();
  }

  protected static Stream<Span> getSpanStream(Collection<ExportTraceServiceRequest> traces) {
    return traces.stream()
        .flatMap(it -> it.getResourceSpansList().stream())
        .flatMap(it -> it.getScopeSpansList().stream())
        .flatMap(it -> it.getSpansList().stream());
  }

  protected static Stream<LogRecord> getLogStream(Collection<ExportLogsServiceRequest> traces) {
    return traces.stream()
        .flatMap(it -> it.getResourceLogsList().stream())
        .flatMap(it -> it.getScopeLogsList().stream())
        .flatMap(it -> it.getLogRecordsList().stream());
  }

  protected static Stream<Metric> getMetricsStream(Collection<ExportMetricsServiceRequest> traces) {
    return traces.stream()
        .flatMap(it -> it.getResourceMetricsList().stream())
        .flatMap(it -> it.getScopeMetricsList().stream())
        .flatMap(it -> it.getMetricsList().stream());
  }

  protected static Stream<String> getLogMessages(Collection<ExportLogsServiceRequest> logs) {
    return getLogStream(logs).map(l -> l.getBody().getStringValue());
  }

  protected static Stream<String> getMetricNames(Collection<ExportMetricsServiceRequest> metrics) {
    return getMetricsStream(metrics).map(Metric::getName);
  }

  protected Collection<ExportTraceServiceRequest> waitForTraces()
      throws IOException, InterruptedException {
    String content = waitForContent("traces");

    return StreamSupport.stream(OBJECT_MAPPER.readTree(content).spliterator(), false)
        .map(
            it -> {
              ExportTraceServiceRequest.Builder builder = ExportTraceServiceRequest.newBuilder();
              // TODO(anuraaga): Register parser into object mapper to avoid de -> re ->
              // deserialize.
              try {
                JsonFormat.parser().merge(OBJECT_MAPPER.writeValueAsString(it), builder);
              } catch (InvalidProtocolBufferException | JsonProcessingException e) {
                e.printStackTrace();
              }
              return builder.build();
            })
        .collect(Collectors.toList());
  }

  protected Collection<ExportLogsServiceRequest> waitForLogs()
      throws IOException, InterruptedException {
    String content = waitForContent("logs");

    return StreamSupport.stream(OBJECT_MAPPER.readTree(content).spliterator(), false)
        .map(
            it -> {
              ExportLogsServiceRequest.Builder builder = ExportLogsServiceRequest.newBuilder();
              try {
                JsonFormat.parser().merge(OBJECT_MAPPER.writeValueAsString(it), builder);
              } catch (InvalidProtocolBufferException | JsonProcessingException e) {
                e.printStackTrace();
              }
              return builder.build();
            })
        .collect(Collectors.toList());
  }

  protected Collection<ExportMetricsServiceRequest> waitForMetrics()
      throws IOException, InterruptedException {
    String content = waitForContent("metrics");

    return StreamSupport.stream(OBJECT_MAPPER.readTree(content).spliterator(), false)
        .map(
            it -> {
              ExportMetricsServiceRequest.Builder builder =
                  ExportMetricsServiceRequest.newBuilder();
              try {
                JsonFormat.parser().merge(OBJECT_MAPPER.writeValueAsString(it), builder);
              } catch (InvalidProtocolBufferException | JsonProcessingException e) {
                e.printStackTrace();
              }
              return builder.build();
            })
        .collect(Collectors.toList());
  }

  private String waitForContent(String signal) throws InterruptedException {
    long previousSize = 0;
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
    String content = "[]";
    while (System.currentTimeMillis() < deadline) {

      Request request =
          new Request.Builder()
              .url(String.format("http://localhost:%d/get-" + signal, backend.getMappedPort(8080)))
              .build();

      content = makeCall(request);
      if (content.length() > 2 && content.length() == previousSize) {
        break;
      }
      previousSize = content.length();
      System.out.printf("Current content size for %s %d%n", signal, previousSize);
      TimeUnit.MILLISECONDS.sleep(500);
    }

    return content;
  }
}
