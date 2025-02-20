/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.genai;

import java.util.List;
import javax.annotation.Nullable;

/**
 * An interface for getting GenAI attributes.
 *
 * <p>Instrumentation authors will create implementations of this interface for their specific
 * library/framework. It will be used by the {@link GenAiAttributesExtractor} to obtain the various
 * GenAI attributes in a type-generic way.
 */
public interface GenAiAttributesGetter<REQUEST, RESPONSE> {
  String getOperationName(REQUEST request);

  String getSystem(REQUEST request);

  @Nullable
  String getRequestModel(REQUEST request);

  @Nullable
  Long getRequestSeed(REQUEST request);

  @Nullable
  List<String> getRequestEncodingFormats(REQUEST request);

  @Nullable
  Double getRequestFrequencyPenalty(REQUEST request);

  @Nullable
  Long getRequestMaxTokens(REQUEST request);

  @Nullable
  Double getRequestPresencePenalty(REQUEST request);

  @Nullable
  List<String> getRequestStopSequences(REQUEST request);

  @Nullable
  Double getRequestTemperature(REQUEST request);

  @Nullable
  Double getRequestTopK(REQUEST request);

  @Nullable
  Double getRequestTopP(REQUEST request);

  @Nullable
  List<String> getResponseFinishReasons(REQUEST request, RESPONSE response);

  @Nullable
  String getResponseId(REQUEST request, RESPONSE response);

  @Nullable
  String getResponseModel(REQUEST request, RESPONSE response);

  @Nullable
  Long getUsageInputTokens(REQUEST request, RESPONSE response);

  @Nullable
  Long getUsageOutputTokens(REQUEST request, RESPONSE response);
}
