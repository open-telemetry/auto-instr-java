/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.network;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.ClientAttributes;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class ClientAttributesExtractorTest {

  static class TestClientAttributesGetter implements ClientAttributesGetter<Map<String, String>> {

    @Nullable
    @Override
    public String getClientAddress(Map<String, String> request) {
      return request.get("address");
    }

    @Nullable
    @Override
    public Integer getClientPort(Map<String, String> request) {
      String value = request.get("port");
      return value == null ? null : Integer.parseInt(value);
    }
  }

  @Test
  void allAttributes() {
    Map<String, String> request = new HashMap<>();
    request.put("address", "opentelemetry.io");
    request.put("port", "80");

    AttributesExtractor<Map<String, String>, Void> extractor =
        ClientAttributesExtractor.create(new TestClientAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onEnd(startAttributes, Context.root(), request, null, null);
    assertThat(startAttributes.build())
        .containsOnly(
            entry(ClientAttributes.CLIENT_ADDRESS, "opentelemetry.io"),
            entry(ClientAttributes.CLIENT_PORT, 80L));

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onStart(endAttributes, Context.root(), request);
    assertThat(endAttributes.build()).isEmpty();
  }

  @Test
  void noAttributes() {
    AttributesExtractor<Map<String, String>, Void> extractor =
        ClientAttributesExtractor.create(new TestClientAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), emptyMap());
    assertThat(startAttributes.build()).isEmpty();

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), emptyMap(), null, null);
    assertThat(endAttributes.build()).isEmpty();
  }

  @Test
  void doesNotSetNegativePortValue() {
    Map<String, String> request = new HashMap<>();
    request.put("port", "-12");

    AttributesExtractor<Map<String, String>, Void> extractor =
        ClientAttributesExtractor.create(new TestClientAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);
    assertThat(startAttributes.build()).isEmpty();

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, null, null);
    assertThat(endAttributes.build()).isEmpty();
  }

  @Test
  void portWithoutAddress() {
    Map<String, String> request = new HashMap<>();
    request.put("port", "80");

    AttributesExtractor<Map<String, String>, Void> extractor =
        ClientAttributesExtractor.create(new TestClientAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);
    assertThat(startAttributes.build()).isEmpty();

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, null, null);
    assertThat(endAttributes.build()).isEmpty();
  }
}
