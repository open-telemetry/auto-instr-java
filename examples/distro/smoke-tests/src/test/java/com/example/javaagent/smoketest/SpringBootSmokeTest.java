/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.javaagent.smoketest;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.TELEMETRY_SDK_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.TELEMETRY_SDK_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.javaagent.DistributionVersion;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.io.IOException;
import java.util.Collection;
import okhttp3.Request;
import org.junit.jupiter.api.Test;

class SpringBootSmokeTest extends SmokeTest {

  @Override
  protected String getTargetImage(int jdk) {
    return "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-spring-boot:jdk"
        + jdk
        + "-20211213.1570880324";
  }

  @Test
  public void checkDistributionVersion() throws IOException, InterruptedException {
    startTarget();

    String url = String.format("http://localhost:%d/greeting", target.getMappedPort(8080));
    Request request = new Request.Builder().url(url).get().build();

    String response = makeCall(request);

    Collection<ExportTraceServiceRequest> traces = waitForTraces();

    assertThat(response).isEqualTo("Hi!");
    assertThat(countSpansByName(traces, "GET /greeting")).isEqualTo(1);
    assertThat(countSpansByName(traces, "WebController.greeting")).isEqualTo(1);
    assertThat(countSpansByName(traces, "WebController.withSpan")).isEqualTo(1);
    assertThat(
            countResourcesByValue(
                traces, TELEMETRY_SDK_VERSION.getKey(), DistributionVersion.VERSION))
        .isGreaterThan(0);
    assertThat(
            countResourcesByValue(
                traces, TELEMETRY_SDK_NAME.getKey(), "opentelemetry-java-instrumentation-distro"))
        .isGreaterThan(0);

    assertThat(getLogMessages(waitForLogs())).contains("HTTP request received");
    assertThat(getMetricNames(waitForMetrics())).contains("process.runtime.jvm.memory.usage");
  }
}
