/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2.recording;

import static com.github.tomakehurst.wiremock.client.WireMock.proxyAllTo;
import static com.github.tomakehurst.wiremock.client.WireMock.recordSpec;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class RecordingExtension extends WireMockExtension
    implements AfterTestExecutionCallback {
  /**
   * Setting this to true will make the tests call the real API instead and record the responses.
   * You'll have to setup credentials for this to work.
   */
  private static final boolean RECORD_WITH_REAL_API = System.getenv("RECORD_WITH_REAL_API") != null;

  private final String apiUrl;

  @SuppressWarnings({"unchecked", "varargs"})
  public RecordingExtension(String apiUrl) {
    super(
        WireMockExtension.newInstance()
            .options(
                options()
                    .extensions(
                        ResponseHeaderScrubber.class,
                        PrettyPrintEqualToJsonStubMappingTransformer.class)
                    .mappingSource(
                        new YamlFileMappingsSource(
                            new SingleRootFileSource("../testing/src/main/resources")
                                .child("mappings")))));
    this.apiUrl = apiUrl;
  }

  public boolean isRecording() {
    return RECORD_WITH_REAL_API;
  }

  @Override
  protected void onBeforeEach(WireMockRuntimeInfo wireMock) {
    // Set a low priority so recordings are used when available
    if (RECORD_WITH_REAL_API) {
      stubFor(proxyAllTo(apiUrl).atPriority(Integer.MAX_VALUE));
      startRecording(
          recordSpec()
              .forTarget(apiUrl)
              .transformers("scrub-response-header", "pretty-print-equal-to-json")
              // Include all bodies inline.
              .extractTextBodiesOver(Long.MAX_VALUE)
              .extractBinaryBodiesOver(Long.MAX_VALUE));
    }
  }

  @Override
  protected void onAfterEach(WireMockRuntimeInfo wireMockRuntimeInfo) {
    if (RECORD_WITH_REAL_API) {
      stopRecording();
    }
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    YamlFileMappingsSource.setCurrentTest(context);
  }
}
