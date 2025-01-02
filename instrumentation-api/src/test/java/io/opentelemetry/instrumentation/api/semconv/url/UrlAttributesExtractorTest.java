/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.url;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.UrlAttributes;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class UrlAttributesExtractorTest {

  static class TestUrlAttributesGetter implements UrlAttributesGetter<Map<String, String>> {

    @Nullable
    @Override
    public String getUrlScheme(Map<String, String> request) {
      return request.get("scheme");
    }

    @Nullable
    @Override
    public String getUrlPath(Map<String, String> request) {
      return request.get("path");
    }

    @Nullable
    @Override
    public String getUrlQuery(Map<String, String> request) {
      return request.get("query");
    }
  }

  @Test
  void allAttributes() {
    Map<String, String> request = new HashMap<>();
    request.put("scheme", "https");
    request.put("path", "/test");
    request.put("query", "q=Java");

    AttributesExtractor<Map<String, String>, Void> extractor =
        UrlAttributesExtractor.create(new TestUrlAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onEnd(startAttributes, Context.root(), request, null, null);
    assertThat(startAttributes.build())
        .containsOnly(
            entry(UrlAttributes.URL_SCHEME, "https"),
            entry(UrlAttributes.URL_PATH, "/test"),
            entry(UrlAttributes.URL_QUERY, "q=Java"));

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onStart(endAttributes, Context.root(), request);
    assertThat(endAttributes.build()).isEmpty();
  }

  @Test
  void noAttributes() {
    AttributesExtractor<Map<String, String>, Void> extractor =
        UrlAttributesExtractor.create(new TestUrlAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), emptyMap());
    assertThat(startAttributes.build()).isEmpty();

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), emptyMap(), null, null);
    assertThat(endAttributes.build()).isEmpty();
  }
}
